package net.minecraft.client.render.entity;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.feature.HorseMarkingFeatureRenderer;
import net.minecraft.client.render.entity.feature.SaddleFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.HorseEntityModel;
import net.minecraft.client.render.entity.model.HorseSaddleEntityModel;
import net.minecraft.client.render.entity.state.HorseEntityRenderState;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.util.Identifier;

import java.util.Map;

@Environment(EnvType.CLIENT)
/**
 * {@code HorseEntityRenderer}.
 */
public final class HorseEntityRenderer extends AbstractHorseEntityRenderer<HorseEntity, HorseEntityRenderState, HorseEntityModel> {

	private static final Map<HorseColor, Identifier> TEXTURES = Maps.newEnumMap(
			Map.of(
					HorseColor.WHITE,
					Identifier.ofVanilla("textures/entity/horse/horse_white.png"),
					HorseColor.CREAMY,
					Identifier.ofVanilla("textures/entity/horse/horse_creamy.png"),
					HorseColor.CHESTNUT,
					Identifier.ofVanilla("textures/entity/horse/horse_chestnut.png"),
					HorseColor.BROWN,
					Identifier.ofVanilla("textures/entity/horse/horse_brown.png"),
					HorseColor.BLACK,
					Identifier.ofVanilla("textures/entity/horse/horse_black.png"),
					HorseColor.GRAY,
					Identifier.ofVanilla("textures/entity/horse/horse_gray.png"),
					HorseColor.DARK_BROWN,
					Identifier.ofVanilla("textures/entity/horse/horse_darkbrown.png")
			)
	);

	public HorseEntityRenderer(EntityRendererFactory.Context context) {
		super(
				context,
				new HorseEntityModel(context.getPart(EntityModelLayers.HORSE)),
				new HorseEntityModel(context.getPart(EntityModelLayers.HORSE_BABY))
		);
		this.addFeature(new HorseMarkingFeatureRenderer(this));
		this.addFeature(
				new SaddleFeatureRenderer<>(
						this,
						context.getEquipmentRenderer(),
						EquipmentModel.LayerType.HORSE_BODY,
						state -> state.armorStack,
						new HorseEntityModel(context.getPart(EntityModelLayers.HORSE_ARMOR)),
						new HorseEntityModel(context.getPart(EntityModelLayers.HORSE_ARMOR_BABY)),
						2
				)
		);
		this.addFeature(
				new SaddleFeatureRenderer<>(
						this,
						context.getEquipmentRenderer(),
						EquipmentModel.LayerType.HORSE_SADDLE,
						state -> state.saddleStack,
						new HorseSaddleEntityModel(context.getPart(EntityModelLayers.HORSE_SADDLE)),
						new HorseSaddleEntityModel(context.getPart(EntityModelLayers.HORSE_BABY_SADDLE)),
						2
				)
		);
	}

	public Identifier getTexture(HorseEntityRenderState horseEntityRenderState) {
		return TEXTURES.get(horseEntityRenderState.color);
	}

	/**
	 * Создаёт render state.
	 *
	 * @return HorseEntityRenderState — результат операции
	 */
	public HorseEntityRenderState createRenderState() {
		return new HorseEntityRenderState();
	}

	/**
	 * Обновляет render state.
	 *
	 * @param horseEntity horse entity
	 * @param horseEntityRenderState horse entity render state
	 * @param f f
	 */
	public void updateRenderState(HorseEntity horseEntity, HorseEntityRenderState horseEntityRenderState, float f) {
		super.updateRenderState(horseEntity, horseEntityRenderState, f);
		horseEntityRenderState.color = horseEntity.getHorseColor();
		horseEntityRenderState.marking = horseEntity.getMarking();
		horseEntityRenderState.armorStack = horseEntity.getBodyArmor().copy();
	}
}
