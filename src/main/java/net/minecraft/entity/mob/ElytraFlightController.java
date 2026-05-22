package net.minecraft.entity.mob;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Контроллер полёта на элитрах для мобов.
 */
public class ElytraFlightController {

	private static final float WING_PITCH_IDLE = (float) (Math.PI / 12);
	private static final float WING_ROLL_IDLE = (float) (-Math.PI / 12);
	private float leftWingPitch;
	private float leftWingYaw;
	private float leftWingRoll;
	private float lastLeftWingPitch;
	private float lastLeftWingYaw;
	private float lastLeftWingRoll;
	private final LivingEntity entity;

	public ElytraFlightController(LivingEntity entity) {
		this.entity = entity;
	}

	public void update() {
		lastLeftWingPitch = leftWingPitch;
		lastLeftWingYaw = leftWingYaw;
		lastLeftWingRoll = leftWingRoll;

		float targetPitch;
		float targetRoll;
		float targetYaw;

		if (entity.isGliding()) {
			float glideBlend = 1.0F;
			Vec3d velocity = entity.getVelocity();

			if (velocity.y < 0.0) {
				Vec3d normalized = velocity.normalize();
				glideBlend = 1.0F - (float) Math.pow(-normalized.y, 1.5);
			}

			targetPitch = MathHelper.lerp(glideBlend, (float) (Math.PI / 12), (float) (Math.PI / 9));
			targetRoll = MathHelper.lerp(glideBlend, (float) (-Math.PI / 12), (float) (-Math.PI / 2));
			targetYaw = 0.0F;
		} else if (entity.isInSneakingPose()) {
			targetPitch = (float) (Math.PI * 2.0 / 9.0);
			targetRoll = (float) (-Math.PI / 4);
			targetYaw = 0.08726646F;
		} else {
			targetPitch = WING_PITCH_IDLE;
			targetRoll = WING_ROLL_IDLE;
			targetYaw = 0.0F;
		}

		leftWingPitch = leftWingPitch + (targetPitch - leftWingPitch) * 0.3F;
		leftWingYaw = leftWingYaw + (targetYaw - leftWingYaw) * 0.3F;
		leftWingRoll = leftWingRoll + (targetRoll - leftWingRoll) * 0.3F;
	}

	public float leftWingPitch(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastLeftWingPitch, leftWingPitch);
	}

	public float leftWingYaw(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastLeftWingYaw, leftWingYaw);
	}

	public float leftWingRoll(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastLeftWingRoll, leftWingRoll);
	}
}
