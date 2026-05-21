package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.state.PiglinEntityRenderState;
import net.minecraft.entity.mob.PiglinActivity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code PiglinEntityModel}.
 */
public class PiglinEntityModel extends PiglinBaseEntityModel<PiglinEntityRenderState> {

	public PiglinEntityModel(ModelPart modelPart) {
		super(modelPart);
	}

	public void setAngles(PiglinEntityRenderState piglinEntityRenderState) {
		super.setAngles(piglinEntityRenderState);
		float f = (float) (Math.PI / 6);
		float g = piglinEntityRenderState.handSwingProgress;
		PiglinActivity piglinActivity = piglinEntityRenderState.activity;
		if (piglinActivity == PiglinActivity.DANCING) {
			float h = piglinEntityRenderState.age / 60.0F;
			this.rightEar.roll = (float) (Math.PI / 6) + (float) (Math.PI / 180.0) * MathHelper.sin(h * 30.0F) * 10.0F;
			this.leftEar.roll = (float) (-Math.PI / 6) - (float) (Math.PI / 180.0) * MathHelper.cos(h * 30.0F) * 10.0F;
			this.head.originX = this.head.originX + MathHelper.sin(h * 10.0F);
			this.head.originY = this.head.originY + (MathHelper.sin(h * 40.0F) + 0.4F);
			this.rightArm.roll = (float) (Math.PI / 180.0) * (70.0F + MathHelper.cos(h * 40.0F) * 10.0F);
			this.leftArm.roll = this.rightArm.roll * -1.0F;
			this.rightArm.originY = this.rightArm.originY + (MathHelper.sin(h * 40.0F) * 0.5F - 0.5F);
			this.leftArm.originY = this.leftArm.originY + (MathHelper.sin(h * 40.0F) * 0.5F + 0.5F);
			this.body.originY = this.body.originY + MathHelper.sin(h * 40.0F) * 0.35F;
		}
		else if (piglinActivity == PiglinActivity.ATTACKING_WITH_MELEE_WEAPON && g == 0.0F) {
			this.rotateMainArm(piglinEntityRenderState);
		}
		else if (piglinActivity == PiglinActivity.CROSSBOW_HOLD) {
			ArmPosing.hold(this.rightArm, this.leftArm, this.head, piglinEntityRenderState.mainArm == Arm.RIGHT);
		}
		else if (piglinActivity == PiglinActivity.CROSSBOW_CHARGE) {
			ArmPosing.charge(
					this.rightArm,
					this.leftArm,
					piglinEntityRenderState.piglinCrossbowPullTime,
					piglinEntityRenderState.itemUseTime,
					piglinEntityRenderState.mainArm == Arm.RIGHT
			);
		}
		else if (piglinActivity == PiglinActivity.ADMIRING_ITEM) {
			this.head.pitch = 0.5F;
			this.head.yaw = 0.0F;
			if (piglinEntityRenderState.mainArm == Arm.LEFT) {
				this.rightArm.yaw = -0.5F;
				this.rightArm.pitch = -0.9F;
			}
			else {
				this.leftArm.yaw = 0.5F;
				this.leftArm.pitch = -0.9F;
			}
		}
	}

	protected void animateArms(PiglinEntityRenderState piglinEntityRenderState) {
		float f = piglinEntityRenderState.handSwingProgress;
		if (f > 0.0F && piglinEntityRenderState.activity == PiglinActivity.ATTACKING_WITH_MELEE_WEAPON) {
			ArmPosing.meleeAttack(
					this.rightArm,
					this.leftArm,
					piglinEntityRenderState.mainArm,
					f,
					piglinEntityRenderState.age
			);
		}
		else {
			super.animateArms(piglinEntityRenderState);
		}
	}

	private void rotateMainArm(PiglinEntityRenderState state) {
		if (state.mainArm == Arm.LEFT) {
			this.leftArm.pitch = -1.8F;
		}
		else {
			this.rightArm.pitch = -1.8F;
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		this.leftSleeve.visible = visible;
		this.rightSleeve.visible = visible;
		this.leftPants.visible = visible;
		this.rightPants.visible = visible;
		this.jacket.visible = visible;
	}
}
