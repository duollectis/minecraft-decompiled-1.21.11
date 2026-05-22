package net.minecraft.entity.ai.brain.task;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Фабричный класс задач мозга для поиска ближайшей точки интереса (POI).
 * Использует экспоненциальную задержку повторных попыток через {@link RetryMarker}.
 */
public class FindPointOfInterestTask {

	public static final int POI_SORTING_RADIUS = 48;
	private static final int POI_LIMIT = 5;
	private static final int SEARCH_INTERVAL = 20;

	public static Task<PathAwareEntity> create(
			Predicate<RegistryEntry<PointOfInterestType>> poiPredicate,
			MemoryModuleType<GlobalPos> poiPosModule,
			boolean onlyRunIfChild,
			Optional<Byte> entityStatus,
			BiPredicate<ServerWorld, BlockPos> worldPosBiPredicate
	) {
		return create(poiPredicate, poiPosModule, poiPosModule, onlyRunIfChild, entityStatus, worldPosBiPredicate);
	}

	public static Task<PathAwareEntity> create(
			Predicate<RegistryEntry<PointOfInterestType>> poiPredicate,
			MemoryModuleType<GlobalPos> poiPosModule,
			boolean onlyRunIfChild,
			Optional<Byte> entityStatus
	) {
		return create(poiPredicate, poiPosModule, poiPosModule, onlyRunIfChild, entityStatus, (world, pos) -> true);
	}

	/**
	 * Создаёт задачу поиска ближайшей точки интереса (POI) с поддержкой повторных попыток.
	 * Использует {@link RetryMarker} для экспоненциальной задержки между попытками достичь
	 * недоступных позиций, чтобы не перегружать навигацию.
	 */
	public static Task<PathAwareEntity> create(
			Predicate<RegistryEntry<PointOfInterestType>> poiPredicate,
			MemoryModuleType<GlobalPos> poiPosModule,
			MemoryModuleType<GlobalPos> potentialPoiPosModule,
			boolean onlyRunIfChild,
			Optional<Byte> entityStatus,
			BiPredicate<ServerWorld, BlockPos> worldPosBiPredicate
	) {
		MutableLong nextSearchTime = new MutableLong(0L);
		Long2ObjectMap<RetryMarker> retryMap = new Long2ObjectOpenHashMap();

		SingleTickTask<PathAwareEntity> searchTask = TaskTriggerer.task(
				taskContext -> taskContext.group(taskContext.queryMemoryAbsent(potentialPoiPosModule))
						.apply(taskContext, queryResult -> (world, entity, time) -> {
							if (onlyRunIfChild && entity.isBaby()) {
								return false;
							}

							if (nextSearchTime.longValue() == 0L) {
								nextSearchTime.setValue(world.getTime() + world.random.nextInt(SEARCH_INTERVAL));
								return false;
							}

							if (world.getTime() < nextSearchTime.longValue()) {
								return false;
							}

							nextSearchTime.setValue(time + SEARCH_INTERVAL + world.getRandom().nextInt(SEARCH_INTERVAL));

							PointOfInterestStorage poiStorage = world.getPointOfInterestStorage();
							retryMap.long2ObjectEntrySet().removeIf(entry -> !entry.getValue().isAttempting(time));

							Predicate<BlockPos> retryFilter = pos -> {
								RetryMarker marker = retryMap.get(pos.asLong());

								if (marker == null) {
									return true;
								}

								if (!marker.shouldRetry(time)) {
									return false;
								}

								marker.setAttemptTime(time);
								return true;
							};

							Set<Pair<RegistryEntry<PointOfInterestType>, BlockPos>> candidates = poiStorage
									.getSortedTypesAndPositions(
											poiPredicate,
											retryFilter,
											entity.getBlockPos(),
											POI_SORTING_RADIUS,
											PointOfInterestStorage.OccupationStatus.HAS_SPACE
									)
									.limit(POI_LIMIT)
									.filter(pair -> worldPosBiPredicate.test(world, (BlockPos) pair.getSecond()))
									.collect(Collectors.toSet());

							Path path = findPathToPoi(entity, candidates);

							if (path != null && path.reachesTarget()) {
								BlockPos target = path.getTarget();
								poiStorage.getType(target).ifPresent(poiType -> {
									poiStorage.getPosition(
											poiPredicate,
											(entry, pos) -> pos.equals(target),
											target,
											1
									);
									queryResult.remember(GlobalPos.create(world.getRegistryKey(), target));
									entityStatus.ifPresent(status -> world.sendEntityStatus(entity, status));
									retryMap.clear();
									world.getSubscriptionTracker().onPoiUpdated(target);
								});
							} else {
								for (Pair<RegistryEntry<PointOfInterestType>, BlockPos> pair : candidates) {
									retryMap.computeIfAbsent(
											((BlockPos) pair.getSecond()).asLong(),
											key -> new RetryMarker(world.random, time)
									);
								}
							}

							return true;
						})
		);

		return potentialPoiPosModule == poiPosModule
				? searchTask
				: TaskTriggerer.task(context -> context
						.group(context.queryMemoryAbsent(poiPosModule))
						.apply(context, poiPos -> searchTask));
	}

	public static @Nullable Path findPathToPoi(
			MobEntity entity,
			Set<Pair<RegistryEntry<PointOfInterestType>, BlockPos>> pois
	) {
		if (pois.isEmpty()) {
			return null;
		}

		Set<BlockPos> positions = new HashSet<>();
		int searchDistance = 1;

		for (Pair<RegistryEntry<PointOfInterestType>, BlockPos> pair : pois) {
			searchDistance = Math.max(
					searchDistance,
					((PointOfInterestType) ((RegistryEntry) pair.getFirst()).value()).searchDistance()
			);
			positions.add((BlockPos) pair.getSecond());
		}

		return entity.getNavigation().findPathTo(positions, searchDistance);
	}

	static class RetryMarker {

		private static final int MIN_DELAY = 40;
		private static final int ATTEMPT_DURATION = 400;

		private final Random random;
		private long previousAttemptAt;
		private long nextScheduledAttemptAt;
		private int currentDelay;

		RetryMarker(Random random, long time) {
			this.random = random;
			setAttemptTime(time);
		}

		public void setAttemptTime(long time) {
			previousAttemptAt = time;
			int next = currentDelay + random.nextInt(MIN_DELAY) + MIN_DELAY;
			currentDelay = Math.min(next, ATTEMPT_DURATION);
			nextScheduledAttemptAt = time + currentDelay;
		}

		public boolean isAttempting(long time) {
			return time - previousAttemptAt < ATTEMPT_DURATION;
		}

		public boolean shouldRetry(long time) {
			return time >= nextScheduledAttemptAt;
		}

		@Override
		public String toString() {
			return "RetryMarker{previousAttemptAt="
					+ previousAttemptAt
					+ ", nextScheduledAttemptAt="
					+ nextScheduledAttemptAt
					+ ", currentDelay="
					+ currentDelay
					+ "}";
		}
	}
}
