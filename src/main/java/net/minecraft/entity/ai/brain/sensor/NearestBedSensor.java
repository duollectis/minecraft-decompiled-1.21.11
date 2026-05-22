package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.FindPointOfInterestTask;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Сенсор поиска ближайшей кровати для детёнышей мобов.
 * Использует POI-хранилище и кэш недоступных позиций, чтобы не проверять одни и те же блоки повторно.
 * Запускается каждые {@code MAX_EXPIRY_TIME} тиков.
 */
public class NearestBedSensor extends Sensor<MobEntity> {

	private static final int REMEMBER_TIME = 40;
	private static final int MAX_TRIES = 5;
	private static final int MAX_EXPIRY_TIME = 20;
	private static final int BED_SEARCH_RADIUS = 48;

	private final Long2LongMap positionToExpiryTime = new Long2LongOpenHashMap();
	private int tries;
	private long expiryTime;

	public NearestBedSensor() {
		super(MAX_EXPIRY_TIME);
	}

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(MemoryModuleType.NEAREST_BED);
	}

	@Override
	protected void sense(ServerWorld world, MobEntity entity) {
		if (!entity.isBaby()) {
			return;
		}

		tries = 0;
		expiryTime = world.getTime() + world.getRandom().nextInt(MAX_EXPIRY_TIME);
		PointOfInterestStorage poiStorage = world.getPointOfInterestStorage();

		Predicate<BlockPos> bedFilter = pos -> {
			long posKey = pos.asLong();

			if (positionToExpiryTime.containsKey(posKey)) {
				return false;
			}

			if (++tries >= MAX_TRIES) {
				return false;
			}

			positionToExpiryTime.put(posKey, expiryTime + REMEMBER_TIME);
			return true;
		};

		Set<Pair<RegistryEntry<PointOfInterestType>, BlockPos>> candidates = poiStorage
				.getTypesAndPositions(
						registryEntry -> registryEntry.matchesKey(PointOfInterestTypes.HOME),
						bedFilter,
						entity.getBlockPos(),
						BED_SEARCH_RADIUS,
						PointOfInterestStorage.OccupationStatus.ANY
				)
				.collect(Collectors.toSet());

		Path path = FindPointOfInterestTask.findPathToPoi(entity, candidates);

		if (path != null && path.reachesTarget()) {
			BlockPos target = path.getTarget();
			Optional<RegistryEntry<PointOfInterestType>> poiType = poiStorage.getType(target);

			if (poiType.isPresent()) {
				entity.getBrain().remember(MemoryModuleType.NEAREST_BED, target);
			}
		} else if (tries < MAX_TRIES) {
			positionToExpiryTime.long2LongEntrySet().removeIf(entry -> entry.getLongValue() < expiryTime);
		}
	}
}
