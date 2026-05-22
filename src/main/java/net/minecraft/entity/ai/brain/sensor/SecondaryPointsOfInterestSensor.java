package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Сенсор поиска вторичных точек интереса (рабочих мест) для жителя деревни.
 * Сканирует блоки в радиусе {@code SEARCH_RADIUS_XZ} x {@code SEARCH_RADIUS_Y} и
 * записывает найденные позиции в память {@code SECONDARY_JOB_SITE}.
 */
public class SecondaryPointsOfInterestSensor extends Sensor<VillagerEntity> {

	private static final int RUN_TIME = 40;
	private static final int SEARCH_RADIUS_XZ = 4;
	private static final int SEARCH_RADIUS_Y = 2;

	public SecondaryPointsOfInterestSensor() {
		super(RUN_TIME);
	}

	@Override
	protected void sense(ServerWorld world, VillagerEntity entity) {
		RegistryKey<World> dimension = world.getRegistryKey();
		BlockPos origin = entity.getBlockPos();
		List<GlobalPos> secondaryPois = new ArrayList<>();

		for (int dx = -SEARCH_RADIUS_XZ; dx <= SEARCH_RADIUS_XZ; dx++) {
			for (int dy = -SEARCH_RADIUS_Y; dy <= SEARCH_RADIUS_Y; dy++) {
				for (int dz = -SEARCH_RADIUS_XZ; dz <= SEARCH_RADIUS_XZ; dz++) {
					BlockPos offset = origin.add(dx, dy, dz);

					if (entity.getVillagerData()
							.profession()
							.value()
							.secondaryJobSites()
							.contains(world.getBlockState(offset).getBlock())) {
						secondaryPois.add(GlobalPos.create(dimension, offset));
					}
				}
			}
		}

		Brain<?> brain = entity.getBrain();

		if (secondaryPois.isEmpty()) {
			brain.forget(MemoryModuleType.SECONDARY_JOB_SITE);
		} else {
			brain.remember(MemoryModuleType.SECONDARY_JOB_SITE, secondaryPois);
		}
	}

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(MemoryModuleType.SECONDARY_JOB_SITE);
	}
}
