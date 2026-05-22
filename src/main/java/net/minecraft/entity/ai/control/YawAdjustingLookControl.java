package net.minecraft.entity.ai.control;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Расширение {@link LookControl} для существ, у которых тело поворачивается
 * вслед за головой (например, эндермен). Добавляет смещение тангажа и рыскания
 * при взгляде на цель, а также плавно подтягивает тело при превышении порога.
 */
public class YawAdjustingLookControl extends LookControl {

	private static final int ADDED_PITCH = 10;
	private static final int ADDED_YAW = 20;
	private static final float BODY_ADJUST_SPEED = 4.0F;
	private static final float IDLE_PITCH_RETURN_SPEED = 5.0F;

	private final int yawAdjustThreshold;

	public YawAdjustingLookControl(MobEntity entity, int yawAdjustThreshold) {
		super(entity);
		this.yawAdjustThreshold = yawAdjustThreshold;
	}

	@Override
	public void tick() {
		if (lookAtTimer > 0) {
			lookAtTimer--;
			getTargetYaw().ifPresent(yaw ->
					entity.headYaw = changeAngle(entity.headYaw, yaw + ADDED_YAW, maxYawChange));
			getTargetPitch().ifPresent(pitch ->
					entity.setPitch(changeAngle(entity.getPitch(), pitch + ADDED_PITCH, maxPitchChange)));
		}
		else {
			if (entity.getNavigation().isIdle()) {
				entity.setPitch(changeAngle(entity.getPitch(), 0.0F, IDLE_PITCH_RETURN_SPEED));
			}

			entity.headYaw = changeAngle(entity.headYaw, entity.bodyYaw, maxYawChange);
		}

		float headBodyDiff = MathHelper.wrapDegrees(entity.headYaw - entity.bodyYaw);

		if (headBodyDiff < -yawAdjustThreshold) {
			entity.bodyYaw -= BODY_ADJUST_SPEED;
		}
		else if (headBodyDiff > yawAdjustThreshold) {
			entity.bodyYaw += BODY_ADJUST_SPEED;
		}
	}
}
