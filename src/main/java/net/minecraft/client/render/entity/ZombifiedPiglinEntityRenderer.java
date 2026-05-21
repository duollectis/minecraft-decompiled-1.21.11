package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EquipmentModelData;
import net.minecraft.client.render.entity.model.ZombifiedPiglinEntityModel;
import net.minecraft.client.render.entity.state.ZombifiedPiglinEntityRenderState;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code ZombifiedPiglinEntityRenderer}.
 */
public class ZombifiedPiglinEntityRenderer extends BipedEntityRenderer<ZombifiedPiglinEntity, ZombifiedPiglinEntityRenderState, ZombifiedPiglinEntityModel> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/piglin/zombified_piglin.png");

	public ZombifiedPiglinEntityRenderer(
			EntityRendererFactory.Context context,
			EntityModelLayer mainLayer,
			EntityModelLayer babyMainLayer,
			EquipmentModelData<EntityModelLayer> equipmentModelData,
			EquipmentModelData<EntityModelLayer> equipmentModelData2
	) {
		super(
				context,
				new ZombifiedPiglinEntityModel(context.getPart(mainLayer)),
				new ZombifiedPiglinEntityModel(context.getPart(babyMainLayer)),
				0.5F,
				PiglinEntityRenderer.HEAD_TRANSFORMATION
		);
		this.addFeature(
				new ArmorFeatureRenderer<>(
						this,
						EquipmentModelData.mapToEntityModel(
								equipmentModelData,
								context.getEntityModels(),
								ZombifiedPiglinEntityModel::new
						),
						EquipmentModelData.mapToEntityModel(
								equipmentModelData2,
								context.getEntityModels(),
								ZombifiedPiglinEntityModel::new
						),
						context.getEquipmentRenderer()
				)
		);
	}

	public Identifier getTexture(ZombifiedPiglinEntityRenderState zombifiedPiglinEntityRenderState) {
		return TEXTURE;
	}

	/**
	 * Создаёт render state.
	 *
	 * @return ZombifiedPiglinEntityRenderState — результат операции
	 */
	public ZombifiedPiglinEntityRenderState createRenderState() {
		return new ZombifiedPiglinEntityRenderState();
	}

	public void updateRenderState(
			ZombifiedPiglinEntity zombifiedPiglinEntity,
			ZombifiedPiglinEntityRenderState zombifiedPiglinEntityRenderState,
			float f
	) {
		super.updateRenderState(zombifiedPiglinEntity, zombifiedPiglinEntityRenderState, f);
		zombifiedPiglinEntityRenderState.attacking = zombifiedPiglinEntity.isAttacking();
	}
}
