package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;

/**
 * Утилитарный класс для вычисления позиций и проверки видимости при движении бриза.
 * Используется задачами прыжка и стрельбы для определения достижимых точек.
 */
public class BreezeMovementUtil {

	private static final double MAX_MOVE_DISTANCE = 50.0;
	private static final float BEHIND_TARGET_ANGLE_SPREAD = 90.0F;
	private static final float BEHIND_TARGET_MIN_DIST = 4.0F;
	private static final float BEHIND_TARGET_MAX_DIST = 8.0F;

	public static Vec3d getRandomPosBehindTarget(LivingEntity target, Random random) {
		float yaw = target.headYaw + 180.0F + (float) random.nextGaussian() * BEHIND_TARGET_ANGLE_SPREAD / 2.0F;
		float distance = MathHelper.lerp(random.nextFloat(), BEHIND_TARGET_MIN_DIST, BEHIND_TARGET_MAX_DIST);
		Vec3d offset = Vec3d.fromPolar(0.0F, yaw).multiply(distance);
		return target.getEntityPos().add(offset);
	}

	public static boolean canMoveTo(BreezeEntity breeze, Vec3d pos) {
		Vec3d origin = new Vec3d(breeze.getX(), breeze.getY(), breeze.getZ());
		if (pos.distanceTo(origin) > getMaxMoveDistance(breeze)) {
			return false;
		}

		return breeze.getEntityWorld()
				.raycast(new RaycastContext(
						origin,
						pos,
						RaycastContext.ShapeType.COLLIDER,
						RaycastContext.FluidHandling.NONE,
						breeze
				))
				.getType() == HitResult.Type.MISS;
	}

	private static double getMaxMoveDistance(BreezeEntity breeze) {
		return Math.max(MAX_MOVE_DISTANCE, breeze.getAttributeValue(EntityAttributes.FOLLOW_RANGE));
	}
}
