package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EquipmentModelData;
import net.minecraft.client.render.entity.model.ZombieEntityModel;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.SwingAnimationComponent;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.SwingAnimationType;

@Environment(EnvType.CLIENT)
/**
 * {@code ZombieBaseEntityRenderer}.
 */
public abstract class ZombieBaseEntityRenderer<T extends ZombieEntity, S extends ZombieEntityRenderState, M extends ZombieEntityModel<S>>
		extends BipedEntityRenderer<T, S, M> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/zombie/zombie.png");

	protected ZombieBaseEntityRenderer(
			EntityRendererFactory.Context context,
			M mainModel,
			M babyMainModel,
			EquipmentModelData<M> equipmentModelData,
			EquipmentModelData<M> equipmentModelData2
	) {
		super(context, mainModel, babyMainModel, 0.5F);
		this.addFeature(new ArmorFeatureRenderer<>(
				this,
				equipmentModelData,
				equipmentModelData2,
				context.getEquipmentRenderer()
		));
	}

	public Identifier getTexture(S zombieEntityRenderState) {
		return TEXTURE;
	}

	/**
	 * Обновляет render state.
	 *
	 * @param zombieEntity zombie entity
	 * @param zombieEntityRenderState zombie entity render state
	 * @param f f
	 */
	public void updateRenderState(T zombieEntity, S zombieEntityRenderState, float f) {
		super.updateRenderState(zombieEntity, zombieEntityRenderState, f);
		zombieEntityRenderState.attacking = zombieEntity.isAttacking();
		zombieEntityRenderState.convertingInWater = zombieEntity.isConvertingInWater();
	}

	protected boolean isShaking(S zombieEntityRenderState) {
		return super.isShaking(zombieEntityRenderState) || zombieEntityRenderState.convertingInWater;
	}

	protected BipedEntityModel.ArmPose getArmPose(T zombieEntity, Arm arm) {
		SwingAnimationComponent
				swingAnimationComponent =
				zombieEntity.getStackInArm(arm.getOpposite()).get(DataComponentTypes.SWING_ANIMATION);
		return swingAnimationComponent != null && swingAnimationComponent.type() == SwingAnimationType.STAB
		       ? BipedEntityModel.ArmPose.SPEAR
		       : super.getArmPose(zombieEntity, arm);
	}
}
