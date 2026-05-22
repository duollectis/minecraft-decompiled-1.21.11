package net.minecraft.entity.ai.brain.task;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Фабричный класс задачи мозга, направляющей сущность к ближайшей незанятой кровати (HOME POI).
 * Кэширует уже проверенные позиции для предотвращения повторных попыток.
 */
public class GoToHomeTask {

	private static final int POI_EXPIRY = 40;
	private static final int MAX_TRIES = 5;
	private static final int RUN_TIME = 20;
	private static final double MIN_DISTANCE_SQ = 4.0;
	private static final int SEARCH_RADIUS = 48;

	public static Task<PathAwareEntity> create(float speed) {
		Long2LongMap triedPositions = new Long2LongOpenHashMap();
		MutableLong nextRunTime = new MutableLong(0L);

		return TaskTriggerer.task(
				taskContext -> taskContext.group(
						taskContext.queryMemoryAbsent(MemoryModuleType.WALK_TARGET),
						taskContext.queryMemoryAbsent(MemoryModuleType.HOME)
				).apply(
						taskContext,
						(walkTarget, home) -> (world, entity, time) -> {
							if (world.getTime() - nextRunTime.longValue() < RUN_TIME) {
								return false;
							}

							PointOfInterestStorage poiStorage = world.getPointOfInterestStorage();
							Optional<BlockPos> nearest = poiStorage.getNearestPosition(
									poiType -> poiType.matchesKey(PointOfInterestTypes.HOME),
									entity.getBlockPos(),
									SEARCH_RADIUS,
									PointOfInterestStorage.OccupationStatus.ANY
							);

							if (nearest.isEmpty()
									|| nearest.get().getSquaredDistance(entity.getBlockPos()) <= MIN_DISTANCE_SQ) {
								return false;
							}

							MutableInt triedCount = new MutableInt(0);
							nextRunTime.setValue(world.getTime() + world.getRandom().nextInt(RUN_TIME));

							Predicate<BlockPos> posFilter = pos -> {
								long key = pos.asLong();

								if (triedPositions.containsKey(key)) {
									return false;
								}

								if (triedCount.incrementAndGet() >= MAX_TRIES) {
									return false;
								}

								triedPositions.put(key, nextRunTime.longValue() + POI_EXPIRY);
								return true;
							};

							Set<Pair<RegistryEntry<PointOfInterestType>, BlockPos>> candidates = poiStorage
									.getTypesAndPositions(
											poiType -> poiType.matchesKey(PointOfInterestTypes.HOME),
											posFilter,
											entity.getBlockPos(),
											SEARCH_RADIUS,
											PointOfInterestStorage.OccupationStatus.ANY
									)
									.collect(Collectors.toSet());

							Path path = FindPointOfInterestTask.findPathToPoi(entity, candidates);

							if (path != null && path.reachesTarget()) {
								BlockPos target = path.getTarget();

								if (poiStorage.getType(target).isPresent()) {
									walkTarget.remember(new WalkTarget(target, speed, 1));
									world.getSubscriptionTracker().onPoiUpdated(target);
								}
							} else if (triedCount.intValue() < MAX_TRIES) {
								triedPositions.long2LongEntrySet()
										.removeIf(entry -> entry.getLongValue() < nextRunTime.longValue());
							}

							return true;
						}
				)
		);
	}
}
