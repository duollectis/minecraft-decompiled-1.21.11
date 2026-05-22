package net.minecraft.entity.ai.control;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Управление движением водных существ (дельфины, кальмары и т.п.).
 * Поддерживает плавучесть, управление тангажом под водой и снижение
 * скорости при повороте в воздухе.
 */
public class AquaticMoveControl extends MoveControl {

	private static final float MIN_ANGLE_FOR_SPEED_REDUCTION = 10.0F;
	private static final float ANGLE_SPEED_REDUCTION_RANGE = 50.0F;
	private static final float BUOYANCY_FORCE = 0.005F;
	private static final float PITCH_ADJUST_SPEED = 5.0F;

	private final int pitchChange;
	private final int yawChange;
	private final float speedInWater;
	private final float speedInAir;
	private final boolean buoyant;

	public AquaticMoveControl(
			MobEntity entity,
			int pitchChange,
			int yawChange,
			float speedInWater,
			float speedInAir,
			boolean buoyant
	) {
		super(entity);
		this.pitchChange = pitchChange;
		this.yawChange = yawChange;
		this.speedInWater = speedInWater;
		this.speedInAir = speedInAir;
		this.buoyant = buoyant;
	}

	@Override
	public void tick() {
		if (buoyant && entity.isTouchingWater()) {
			entity.setVelocity(entity.getVelocity().add(0.0, BUOYANCY_FORCE, 0.0));
		}

		if (state != MoveControl.State.MOVE_TO || entity.getNavigation().isIdle()) {
			entity.setMovementSpeed(0.0F);
			entity.setSidewaysSpeed(0.0F);
			entity.setUpwardSpeed(0.0F);
			entity.setForwardSpeed(0.0F);
			return;
		}

		double dx = targetX - entity.getX();
		double dy = targetY - entity.getY();
		double dz = targetZ - entity.getZ();
		double distSq = dx * dx + dy * dy + dz * dz;

		if (distSq < REACHED_DESTINATION_DISTANCE_SQUARED) {
			entity.setForwardSpeed(0.0F);
			return;
		}

		float targetYaw = (float) (MathHelper.atan2(dz, dx) * 180.0F / (float) Math.PI) - 90.0F;
		entity.setYaw(wrapDegrees(entity.getYaw(), targetYaw, yawChange));
		entity.bodyYaw = entity.getYaw();
		entity.headYaw = entity.getYaw();

		float baseSpeed = (float) (speed * entity.getAttributeValue(EntityAttributes.MOVEMENT_SPEED));

		if (entity.isTouchingWater()) {
			entity.setMovementSpeed(baseSpeed * speedInWater);

			double horizontalDist = Math.sqrt(dx * dx + dz * dz);

			if (Math.abs(dy) > 1.0E-5F || Math.abs(horizontalDist) > 1.0E-5F) {
				float rawPitch = -((float) (MathHelper.atan2(dy, horizontalDist) * 180.0F / (float) Math.PI));
				float clampedPitch = MathHelper.clamp(
						MathHelper.wrapDegrees(rawPitch),
						(float) (-pitchChange),
						(float) pitchChange
				);
				entity.setPitch(changeAngle(entity.getPitch(), clampedPitch, PITCH_ADJUST_SPEED));
			}

			float pitchCos = MathHelper.cos(entity.getPitch() * (float) (Math.PI / 180.0));
			float pitchSin = MathHelper.sin(entity.getPitch() * (float) (Math.PI / 180.0));
			entity.forwardSpeed = pitchCos * baseSpeed;
			entity.upwardSpeed = -pitchSin * baseSpeed;
		}
		else {
			float yawDiff = Math.abs(MathHelper.wrapDegrees(entity.getYaw() - targetYaw));
			float speedFactor = calculateSpeedFactor(yawDiff);
			entity.setMovementSpeed(baseSpeed * speedInAir * speedFactor);
		}
	}

	private static float calculateSpeedFactor(float angleDiff) {
		return 1.0F - MathHelper.clamp((angleDiff - MIN_ANGLE_FOR_SPEED_REDUCTION) / ANGLE_SPEED_REDUCTION_RANGE, 0.0F, 1.0F);
	}
}
