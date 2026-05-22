package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.feature.SaddleFeatureRenderer;
import net.minecraft.client.render.entity.model.DonkeyEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.HorseSaddleEntityModel;
import net.minecraft.client.render.entity.state.DonkeyEntityRenderState;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.util.Identifier;

/**
 * Базовый рендерер для осла и мула.
 * <p>
 * Загружает модели взрослой и детской особи, а также добавляет
 * {@link SaddleFeatureRenderer} для отображения седла. Конкретный тип
 * (осёл или мул) задаётся через {@link Type}.
 */
@Environment(EnvType.CLIENT)
public class AbstractDonkeyEntityRenderer<T extends AbstractDonkeyEntity>
		extends AbstractHorseEntityRenderer<T, DonkeyEntityRenderState, DonkeyEntityModel> {

	private final Identifier texture;

	public AbstractDonkeyEntityRenderer(EntityRendererFactory.Context context, AbstractDonkeyEntityRenderer.Type type) {
		super(
				context,
				new DonkeyEntityModel(context.getPart(type.adultModelLayer)),
				new DonkeyEntityModel(context.getPart(type.babyModelLayer))
		);
		texture = type.texture;
		addFeature(
				new SaddleFeatureRenderer<>(
						this,
						context.getEquipmentRenderer(),
						type.saddleLayerType,
						state -> state.saddleStack,
						new HorseSaddleEntityModel(context.getPart(type.adultSaddleModelLayer)),
						new HorseSaddleEntityModel(context.getPart(type.babySaddleModelLayer))
				)
		);
	}

	@Override
	public Identifier getTexture(DonkeyEntityRenderState state) {
		return texture;
	}

	@Override
	public DonkeyEntityRenderState createRenderState() {
		return new DonkeyEntityRenderState();
	}

	@Override
	public void updateRenderState(T entity, DonkeyEntityRenderState state, float tickProgress) {
		super.updateRenderState(entity, state, tickProgress);
		state.hasChest = entity.hasChest();
	}

	/** Перечисление конкретных видов ослиных существ с их текстурами и слоями моделей. */
	@Environment(EnvType.CLIENT)
	public enum Type {
		DONKEY(
				Identifier.ofVanilla("textures/entity/horse/donkey.png"),
				EntityModelLayers.DONKEY,
				EntityModelLayers.DONKEY_BABY,
				EquipmentModel.LayerType.DONKEY_SADDLE,
				EntityModelLayers.DONKEY_SADDLE,
				EntityModelLayers.DONKEY_BABY_SADDLE
		),
		MULE(
				Identifier.ofVanilla("textures/entity/horse/mule.png"),
				EntityModelLayers.MULE,
				EntityModelLayers.MULE_BABY,
				EquipmentModel.LayerType.MULE_SADDLE,
				EntityModelLayers.MULE_SADDLE,
				EntityModelLayers.MULE_BABY_SADDLE
		);

		final Identifier texture;
		final EntityModelLayer adultModelLayer;
		final EntityModelLayer babyModelLayer;
		final EquipmentModel.LayerType saddleLayerType;
		final EntityModelLayer adultSaddleModelLayer;
		final EntityModelLayer babySaddleModelLayer;

		Type(
				final Identifier texture,
				final EntityModelLayer adultModelLayer,
				final EntityModelLayer babyModelLayer,
				final EquipmentModel.LayerType saddleLayerType,
				final EntityModelLayer adultSaddleModelLayer,
				final EntityModelLayer babySaddleModelLayer
		) {
			this.texture = texture;
			this.adultModelLayer = adultModelLayer;
			this.babyModelLayer = babyModelLayer;
			this.saddleLayerType = saddleLayerType;
			this.adultSaddleModelLayer = adultSaddleModelLayer;
			this.babySaddleModelLayer = babySaddleModelLayer;
		}
	}
}
