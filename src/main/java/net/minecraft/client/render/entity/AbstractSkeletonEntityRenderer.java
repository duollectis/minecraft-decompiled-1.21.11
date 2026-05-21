package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EquipmentModelData;
import net.minecraft.client.render.entity.model.SkeletonEntityModel;
import net.minecraft.client.render.entity.state.SkeletonEntityRenderState;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;

@Environment(EnvType.CLIENT)
/**
 * {@code AbstractSkeletonEntityRenderer}.
 */
public abstract class AbstractSkeletonEntityRenderer<T extends AbstractSkeletonEntity, S extends SkeletonEntityRenderState>
		extends BipedEntityRenderer<T, S, SkeletonEntityModel<S>> {

	public AbstractSkeletonEntityRenderer(
			EntityRendererFactory.Context context,
			EntityModelLayer layer,
			EquipmentModelData<EntityModelLayer> equipmentModelData
	) {
		this(context, equipmentModelData, new SkeletonEntityModel<>(context.getPart(layer)));
	}

	public AbstractSkeletonEntityRenderer(
			EntityRendererFactory.Context context,
			EquipmentModelData<EntityModelLayer> equipmentModelData,
			SkeletonEntityModel<S> skeletonEntityModel
	) {
		super(context, skeletonEntityModel, 0.5F);
		this.addFeature(
				new ArmorFeatureRenderer<>(
						this,
						EquipmentModelData.mapToEntityModel(
								equipmentModelData,
								context.getEntityModels(),
								SkeletonEntityModel::new
						),
						context.getEquipmentRenderer()
				)
		);
	}

	public void updateRenderState(T abstractSkeletonEntity, S skeletonEntityRenderState, float f) {
		super.updateRenderState(abstractSkeletonEntity, skeletonEntityRenderState, f);
		skeletonEntityRenderState.attacking = abstractSkeletonEntity.isAttacking();
		skeletonEntityRenderState.shaking = abstractSkeletonEntity.isShaking();
		skeletonEntityRenderState.holdingBow = abstractSkeletonEntity.getMainHandStack().isOf(Items.BOW);
	}

	protected boolean isShaking(S skeletonEntityRenderState) {
		return skeletonEntityRenderState.shaking;
	}

	protected BipedEntityModel.ArmPose getArmPose(T abstractSkeletonEntity, Arm arm) {
		return abstractSkeletonEntity.getMainArm() == arm && abstractSkeletonEntity.isAttacking()
				       && abstractSkeletonEntity.getMainHandStack().isOf(Items.BOW)
		       ? BipedEntityModel.ArmPose.BOW_AND_ARROW
		       : super.getArmPose(abstractSkeletonEntity, arm);
	}
}
