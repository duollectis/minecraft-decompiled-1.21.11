package net.minecraft.entity.ai.control;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Управляет поворотом тела моба относительно головы.
 * Синхронизирует {@code bodyYaw} с {@code headYaw} при движении,
 * а при стоянии плавно подтягивает тело к направлению взгляда.
 */
public class BodyControl implements Control {

	private static final int BODY_KEEP_UP_THRESHOLD = 15;
	private static final int ROTATE_BODY_START_TICK = 10;
	private static final int ROTATION_INCREMENTS = 10;

	private final MobEntity entity;
	private int bodyAdjustTicks;
	private float lastHeadYaw;

	public BodyControl(MobEntity entity) {
		this.entity = entity;
	}

	public void tick() {
		if (isMoving()) {
			entity.bodyYaw = entity.getYaw();
			keepUpHead();
			lastHeadYaw = entity.headYaw;
			bodyAdjustTicks = 0;
			return;
		}

		if (!isIndependent()) {
			return;
		}

		if (Math.abs(entity.headYaw - lastHeadYaw) > BODY_KEEP_UP_THRESHOLD) {
			bodyAdjustTicks = 0;
			lastHeadYaw = entity.headYaw;
			keepUpBody();
		}
		else {
			bodyAdjustTicks++;

			if (bodyAdjustTicks > ROTATE_BODY_START_TICK) {
				slowlyAdjustBody();
			}
		}
	}

	private void keepUpBody() {
		entity.bodyYaw = MathHelper.clampAngle(entity.bodyYaw, entity.headYaw, entity.getMaxHeadRotation());
	}

	private void keepUpHead() {
		entity.headYaw = MathHelper.clampAngle(entity.headYaw, entity.bodyYaw, entity.getMaxHeadRotation());
	}

	private void slowlyAdjustBody() {
		int elapsed = bodyAdjustTicks - ROTATE_BODY_START_TICK;
		float progress = MathHelper.clamp(elapsed / (float) ROTATION_INCREMENTS, 0.0F, 1.0F);
		float maxAngle = entity.getMaxHeadRotation() * (1.0F - progress);
		entity.bodyYaw = MathHelper.clampAngle(entity.bodyYaw, entity.headYaw, maxAngle);
	}

	private boolean isIndependent() {
		return !(entity.getFirstPassenger() instanceof MobEntity);
	}

	private boolean isMoving() {
		double dx = entity.getX() - entity.lastX;
		double dz = entity.getZ() - entity.lastZ;
		return dx * dx + dz * dz > MoveControl.REACHED_DESTINATION_DISTANCE_SQUARED;
	}
}
