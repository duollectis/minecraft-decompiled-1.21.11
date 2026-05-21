package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.feature.SaddleFeatureRenderer;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.render.entity.state.LivingHorseEntityRenderState;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * Класс undead horse entity renderer.
 */
public class UndeadHorseEntityRenderer
		extends AbstractHorseEntityRenderer<AbstractHorseEntity, LivingHorseEntityRenderState, AbstractHorseEntityModel<LivingHorseEntityRenderState>> {

	private final Identifier texture;

	public UndeadHorseEntityRenderer(EntityRendererFactory.Context ctx, UndeadHorseEntityRenderer.Type type) {
		super(
				ctx,
				new HorseEntityModel(ctx.getPart(type.modelLayer)),
				new HorseEntityModel(ctx.getPart(type.babyModelLayer))
		);
		this.texture = type.texture;
		this.addFeature(
				new SaddleFeatureRenderer<>(
						this,
						ctx.getEquipmentRenderer(),
						EquipmentModel.LayerType.HORSE_BODY,
						state -> state.armorStack,
						new HorseEntityModel(ctx.getPart(EntityModelLayers.UNDEAD_HORSE_ARMOR)),
						new HorseEntityModel(ctx.getPart(EntityModelLayers.UNDEAD_HORSE_BABY_ARMOR))
				)
		);
		this.addFeature(
				new SaddleFeatureRenderer<>(
						this,
						ctx.getEquipmentRenderer(),
						type.saddleLayerType,
						state -> state.saddleStack,
						new HorseSaddleEntityModel(ctx.getPart(type.saddleModelLayer)),
						new HorseSaddleEntityModel(ctx.getPart(type.babySaddleModelLayer))
				)
		);
	}

	public Identifier getTexture(LivingHorseEntityRenderState livingHorseEntityRenderState) {
		return this.texture;
	}

	/**
	 * Создаёт render state.
	 *
	 * @return LivingHorseEntityRenderState — результат операции
	 */
	public LivingHorseEntityRenderState createRenderState() {
		return new LivingHorseEntityRenderState();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Type}.
	 */
	public static enum Type {
		SKELETON(
				Identifier.ofVanilla("textures/entity/horse/horse_skeleton.png"),
				EntityModelLayers.SKELETON_HORSE,
				EntityModelLayers.SKELETON_HORSE_BABY,
				EquipmentModel.LayerType.SKELETON_HORSE_SADDLE,
				EntityModelLayers.SKELETON_HORSE_SADDLE,
				EntityModelLayers.SKELETON_HORSE_BABY_SADDLE
		),
		ZOMBIE(
				Identifier.ofVanilla("textures/entity/horse/horse_zombie.png"),
				EntityModelLayers.ZOMBIE_HORSE,
				EntityModelLayers.ZOMBIE_HORSE_BABY,
				EquipmentModel.LayerType.ZOMBIE_HORSE_SADDLE,
				EntityModelLayers.ZOMBIE_HORSE_SADDLE,
				EntityModelLayers.ZOMBIE_HORSE_BABY_SADDLE
		);

		final Identifier texture;
		final EntityModelLayer modelLayer;
		final EntityModelLayer babyModelLayer;
		final EquipmentModel.LayerType saddleLayerType;
		final EntityModelLayer saddleModelLayer;
		final EntityModelLayer babySaddleModelLayer;

		private Type(
				final Identifier texture,
				final EntityModelLayer modelLayer,
				final EntityModelLayer babyModelLayer,
				final EquipmentModel.LayerType saddleLayerType,
				final EntityModelLayer saddleModelLayer,
				final EntityModelLayer babySaddleModelLayer
		) {
			this.texture = texture;
			this.modelLayer = modelLayer;
			this.babyModelLayer = babyModelLayer;
			this.saddleLayerType = saddleLayerType;
			this.saddleModelLayer = saddleModelLayer;
			this.babySaddleModelLayer = babySaddleModelLayer;
		}
	}
}
