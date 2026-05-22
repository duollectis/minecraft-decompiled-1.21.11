package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.function.Function;

/**
 * Фабричный класс задачи мозга, направляющей сущность прочь от запомненной позиции или сущности.
 * Использует нечёткий поиск для выбора позиции в заданном радиусе от цели.
 */
public class GoToRememberedPositionTask {

	private static final int SEARCH_ATTEMPTS = 10;
	private static final int HORIZONTAL_RANGE = 16;
	private static final int VERTICAL_RANGE = 7;

	public static Task<PathAwareEntity> createPosBased(
			MemoryModuleType<BlockPos> posModule,
			float speed,
			int range,
			boolean requiresWalkTarget
	) {
		return create(posModule, speed, range, requiresWalkTarget, Vec3d::ofBottomCenter);
	}

	public static SingleTickTask<PathAwareEntity> createEntityBased(
			MemoryModuleType<? extends Entity> entityModule,
			float speed,
			int range,
			boolean requiresWalkTarget
	) {
		return create(entityModule, speed, range, requiresWalkTarget, Entity::getEntityPos);
	}

	private static <T> SingleTickTask<PathAwareEntity> create(
			MemoryModuleType<T> posSource,
			float speed,
			int range,
			boolean requiresWalkTarget,
			Function<T, Vec3d> posGetter
	) {
		return TaskTriggerer.task(
				context -> context.group(
						context.queryMemoryOptional(MemoryModuleType.WALK_TARGET),
						context.queryMemoryValue(posSource)
				).apply(
						context,
						(walkTarget, posSourceValue) -> (world, entity, time) -> {
							Optional<WalkTarget> currentWalkTarget = context.getOptionalValue(walkTarget);

							if (currentWalkTarget.isPresent() && !requiresWalkTarget) {
								return false;
							}

							Vec3d entityPos = entity.getEntityPos();
							Vec3d targetPos = posGetter.apply(context.getValue(posSourceValue));

							if (!entityPos.isInRange(targetPos, range)) {
								return false;
							}

							if (currentWalkTarget.isPresent() && currentWalkTarget.get().getSpeed() == speed) {
								Vec3d currentDir = currentWalkTarget.get().getLookTarget().getPos().subtract(entityPos);
								Vec3d targetDir = targetPos.subtract(entityPos);

								if (currentDir.dotProduct(targetDir) < 0.0) {
									return false;
								}
							}

							for (int attempt = 0; attempt < SEARCH_ATTEMPTS; attempt++) {
								Vec3d wanderPos = FuzzyTargeting.findFrom(entity, HORIZONTAL_RANGE, VERTICAL_RANGE, targetPos);

								if (wanderPos != null) {
									walkTarget.remember(new WalkTarget(wanderPos, speed, 0));
									break;
								}
							}

							return true;
						}
				)
		);
	}
}
