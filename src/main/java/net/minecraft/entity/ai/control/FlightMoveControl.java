package net.minecraft.entity.ai.control;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Управление движением летающих существ (летучие мыши, пчёлы, фантомы).
 * При активном состоянии MOVE_TO отключает гравитацию и управляет
 * тангажом для набора/снижения высоты.
 */
public class FlightMoveControl extends MoveControl {

	private final int maxPitchChange;
	private final boolean noGravity;

	public FlightMoveControl(MobEntity entity, int maxPitchChange, boolean noGravity) {
		super(entity);
		this.maxPitchChange = maxPitchChange;
		this.noGravity = noGravity;
	}

	@Override
	public void tick() {
		if (state != MoveControl.State.MOVE_TO) {
			if (!noGravity) {
				entity.setNoGravity(false);
			}

			entity.setUpwardSpeed(0.0F);
			entity.setForwardSpeed(0.0F);
			return;
		}

		state = MoveControl.State.WAIT;
		entity.setNoGravity(true);

		double dx = targetX - entity.getX();
		double dy = targetY - entity.getY();
		double dz = targetZ - entity.getZ();
		double distSq = dx * dx + dy * dy + dz * dz;

		if (distSq < REACHED_DESTINATION_DISTANCE_SQUARED) {
			entity.setUpwardSpeed(0.0F);
			entity.setForwardSpeed(0.0F);
			return;
		}

		float targetYaw = (float) (MathHelper.atan2(dz, dx) * 180.0F / (float) Math.PI) - 90.0F;
		entity.setYaw(wrapDegrees(entity.getYaw(), targetYaw, MAX_TURN_DEGREES));

		float currentSpeed = entity.isOnGround()
				? (float) (speed * entity.getAttributeValue(EntityAttributes.MOVEMENT_SPEED))
				: (float) (speed * entity.getAttributeValue(EntityAttributes.FLYING_SPEED));

		entity.setMovementSpeed(currentSpeed);

		double horizontalDist = Math.sqrt(dx * dx + dz * dz);

		if (Math.abs(dy) > 1.0E-5F || Math.abs(horizontalDist) > 1.0E-5F) {
			float targetPitch = (float) (-(MathHelper.atan2(dy, horizontalDist) * 180.0F / (float) Math.PI));
			entity.setPitch(wrapDegrees(entity.getPitch(), targetPitch, maxPitchChange));
			entity.setUpwardSpeed(dy > 0.0 ? currentSpeed : -currentSpeed);
		}
	}
}
