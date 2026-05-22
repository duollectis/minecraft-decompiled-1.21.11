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
 * Фабричный класс задач мозга для случайного блуждания сущности по миру.
 * Поддерживает несколько стратегий выбора цели: нечёткую, твёрдую поверхность и динамический радиус.
 */
public class StrollTask {

	private static final int DEFAULT_HORIZONTAL_RADIUS = 10;
	private static final int DEFAULT_VERTICAL_RADIUS = 7;
	private static final int[][] RADII = new int[][]{{1, 1}, {3, 3}, {5, 5}, {6, 5}, {7, 7}, {DEFAULT_HORIZONTAL_RADIUS, 7}};

	public static SingleTickTask<PathAwareEntity> create(float speed) {
		return create(speed, true);
	}

	public static SingleTickTask<PathAwareEntity> create(float speed, boolean strollInsideWater) {
		return create(
				speed,
				entity -> FuzzyTargeting.find(entity, DEFAULT_HORIZONTAL_RADIUS, DEFAULT_VERTICAL_RADIUS),
				strollInsideWater ? entity -> true : entity -> !entity.isTouchingWater()
		);
	}

	public static Task<PathAwareEntity> create(float speed, int horizontalRadius, int verticalRadius) {
		return create(speed, entity -> FuzzyTargeting.find(entity, horizontalRadius, verticalRadius), entity -> true);
	}

	public static Task<PathAwareEntity> createSolidTargeting(float speed) {
		return create(speed, entity -> findTargetPos(entity, DEFAULT_HORIZONTAL_RADIUS, DEFAULT_VERTICAL_RADIUS), entity -> true);
	}

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

							Optional<Vec3d> targetPosOpt = Optional.ofNullable(targetGetter.apply(entity));
							walkTarget.remember(targetPosOpt.map(pos -> new WalkTarget(pos, speed, 0)));
							return true;
						}
				)
		);
	}

	private static @Nullable Vec3d findTargetPos(PathAwareEntity entity) {
		Vec3d bestPos = null;
		Vec3d candidate = null;

		for (int[] radii : RADII) {
			if (bestPos == null) {
				candidate = TargetUtil.find(entity, radii[0], radii[1]);
			} else {
				candidate = entity
						.getEntityPos()
						.add(entity.getEntityPos().relativize(bestPos).normalize().multiply(radii[0], radii[1], radii[0]));
			}

			boolean inRange = NavigationConditions.isPositionTargetInRange(entity, radii[0]);

			if (candidate == null
					|| entity.getEntityWorld().getFluidState(BlockPos.ofFloored(candidate)).isEmpty()
					|| NavigationConditions.isPositionTargetOutOfWalkRange(inRange, entity, candidate)
			) {
				return bestPos;
			}

			bestPos = candidate;
		}

		return candidate;
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
