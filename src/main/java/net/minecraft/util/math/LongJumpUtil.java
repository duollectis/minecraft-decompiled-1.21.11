package net.minecraft.util.math;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.mob.MobEntity;

import java.util.Optional;

/**
 * Утилитарный класс для вычисления вектора скорости прыжка в длину.
 * Решает задачу баллистики: находит начальную скорость, при которой сущность
 * долетит до цели под заданным углом с учётом гравитации.
 */
public final class LongJumpUtil {

	/**
	 * Вычисляет вектор скорости для прыжка в длину к целевой точке.
	 * Использует формулу баллистики: v² = g·d / sin(2θ) - 2·h·cos²(θ).
	 *
	 * @param entity сущность, выполняющая прыжок
	 * @param jumpTarget целевая точка
	 * @param maxVelocity максимально допустимая скорость
	 * @param angle угол броска в градусах
	 * @param requireClearPath проверять ли отсутствие препятствий на пути
	 * @return вектор начальной скорости, или {@link Optional#empty()} если прыжок невозможен
	 */
	public static Optional<Vec3d> getJumpingVelocity(
			MobEntity entity,
			Vec3d jumpTarget,
			float maxVelocity,
			int angle,
			boolean requireClearPath
	) {
		Vec3d entityPos = entity.getEntityPos();
		Vec3d horizontalOffset = new Vec3d(jumpTarget.x - entityPos.x, 0.0, jumpTarget.z - entityPos.z)
				.normalize()
				.multiply(0.5);
		Vec3d adjustedTarget = jumpTarget.subtract(horizontalOffset);
		Vec3d delta = adjustedTarget.subtract(entityPos);

		float angleRad = angle * (float) Math.PI / 180.0F;
		double yaw = Math.atan2(delta.z, delta.x);
		double horizontalDistSq = delta.subtract(0.0, delta.y, 0.0).lengthSquared();
		double horizontalDist = Math.sqrt(horizontalDistSq);
		double verticalDelta = delta.y;
		double gravity = entity.getFinalGravity();
		double sin2Angle = Math.sin(2.0F * angleRad);
		double cos2Angle = Math.pow(Math.cos(angleRad), 2.0);
		double sinAngle = Math.sin(angleRad);
		double cosAngle = Math.cos(angleRad);
		double sinYaw = Math.sin(yaw);
		double cosYaw = Math.cos(yaw);

		double velocitySq = horizontalDistSq * gravity / (horizontalDist * sin2Angle - 2.0 * verticalDelta * cos2Angle);

		if (velocitySq < 0.0) {
			return Optional.empty();
		}

		double velocity = Math.sqrt(velocitySq);

		if (velocity > maxVelocity) {
			return Optional.empty();
		}

		double horizontalVelocity = velocity * cosAngle;
		double verticalVelocity = velocity * sinAngle;

		if (requireClearPath) {
			int steps = MathHelper.ceil(horizontalDist / horizontalVelocity) * 2;
			double stepDist = 0.0;
			Vec3d prevPos = null;
			EntityDimensions dimensions = entity.getDimensions(EntityPose.LONG_JUMPING);

			for (int step = 0; step < steps - 1; step++) {
				stepDist += horizontalDist / steps;
				double height = sinAngle / cosAngle * stepDist
						- Math.pow(stepDist, 2.0) * gravity / (2.0 * velocitySq * Math.pow(cosAngle, 2.0));
				Vec3d currentPos = new Vec3d(
						entityPos.x + stepDist * cosYaw,
						entityPos.y + height,
						entityPos.z + stepDist * sinYaw
				);

				if (prevPos != null && isPathClear(entity, dimensions, prevPos, currentPos) == false) {
					return Optional.empty();
				}

				prevPos = currentPos;
			}
		}

		return Optional.of(new Vec3d(horizontalVelocity * cosYaw, verticalVelocity, horizontalVelocity * sinYaw).multiply(0.95F));
	}

	private static boolean isPathClear(MobEntity entity, EntityDimensions dimensions, Vec3d lastPos, Vec3d nextPos) {
		Vec3d step = nextPos.subtract(lastPos);
		double minDimension = Math.min(dimensions.width(), dimensions.height());
		int steps = MathHelper.ceil(step.length() / minDimension);
		Vec3d stepDir = step.normalize();
		Vec3d current = lastPos;

		for (int i = 0; i < steps; i++) {
			current = i == steps - 1 ? nextPos : current.add(stepDir.multiply(minDimension * 0.9F));

			if (entity.getEntityWorld().isSpaceEmpty(entity, dimensions.getBoxAt(current)) == false) {
				return false;
			}
		}

		return true;
	}
}
