package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.NavigationConditions;
import net.minecraft.entity.ai.NoPenaltySolidTargeting;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * {@code StrollTask}.
 */
public class StrollTask {

	private static final int DEFAULT_HORIZONTAL_RADIUS = 10;
	private static final int DEFAULT_VERTICAL_RADIUS = 7;
	private static final int[][] RADII = new int[][]{{1, 1}, {3, 3}, {5, 5}, {6, 5}, {7, 7}, {10, 7}};

	/**
	 * Create.
	 *
	 * @param speed speed
	 *
	 * @return SingleTickTask — результат операции
	 */
	public static SingleTickTask<PathAwareEntity> create(float speed) {
		return create(speed, true);
	}

	/**
	 * Create.
	 *
	 * @param speed speed
	 * @param strollInsideWater stroll inside water
	 *
	 * @return SingleTickTask — результат операции
	 */
	public static SingleTickTask<PathAwareEntity> create(float speed, boolean strollInsideWater) {
		return create(
				speed,
				entity -> FuzzyTargeting.find(entity, 10, 7),
				strollInsideWater ? entity -> true : entity -> !entity.isTouchingWater()
		);
	}

	/**
	 * Create.
	 *
	 * @param speed speed
	 * @param horizontalRadius horizontal radius
	 * @param verticalRadius vertical radius
	 *
	 * @return Task — результат операции
	 */
	public static Task<PathAwareEntity> create(float speed, int horizontalRadius, int verticalRadius) {
		return create(speed, entity -> FuzzyTargeting.find(entity, horizontalRadius, verticalRadius), entity -> true);
	}

	/**
	 * Создаёт solid targeting.
	 *
	 * @param speed speed
	 *
	 * @return Task — результат операции
	 */
	public static Task<PathAwareEntity> createSolidTargeting(float speed) {
		return create(speed, entity -> findTargetPos(entity, 10, 7), entity -> true);
	}

	/**
	 * Создаёт dynamic radius.
	 *
	 * @param speed speed
	 *
	 * @return Task — результат операции
	 */
	public static Task<PathAwareEntity> createDynamicRadius(float speed) {
		return create(speed, StrollTask::findTargetPos, Entity::isTouchingWater);
	}

	private static SingleTickTask<PathAwareEntity> create(
			float speed,
			Function<PathAwareEntity, Vec3d> targetGetter,
			Predicate<PathAwareEntity> shouldRun
	) {
		return TaskTriggerer.task(
				context -> context.group(context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET)).apply(
						context, walkTarget -> (world, entity, time) -> {
							if (!shouldRun.test(entity)) {
								return false;
							}
							else {
								Optional<Vec3d> optional = Optional.ofNullable(targetGetter.apply(entity));
								walkTarget.remember(optional.map(pos -> new WalkTarget(pos, speed, 0)));
								return true;
							}
						}
				)
		);
	}

	private static @Nullable Vec3d findTargetPos(PathAwareEntity entity) {
		Vec3d vec3d = null;
		Vec3d vec3d2 = null;

		for (int[] is : RADII) {
			if (vec3d == null) {
				vec3d2 = TargetUtil.find(entity, is[0], is[1]);
			}
			else {
				vec3d2 =
						entity
								.getEntityPos()
								.add(entity.getEntityPos().relativize(vec3d).normalize().multiply(is[0], is[1], is[0]));
			}

			boolean bl = NavigationConditions.isPositionTargetInRange(entity, is[0]);
			if (vec3d2 == null
					|| entity.getEntityWorld().getFluidState(BlockPos.ofFloored(vec3d2)).isEmpty()
					|| NavigationConditions.isPositionTargetOutOfWalkRange(bl, entity, vec3d2)) {
				return vec3d;
			}

			vec3d = vec3d2;
		}

		return vec3d2;
	}

	private static @Nullable Vec3d findTargetPos(PathAwareEntity entity, int horizontalRadius, int verticalRadius) {
		Vec3d vec3d = entity.getRotationVec(0.0F);
		return NoPenaltySolidTargeting.find(
				entity,
				horizontalRadius,
				verticalRadius,
				-2,
				vec3d.x,
				vec3d.z,
				(float) (Math.PI / 2)
		);
	}
}
