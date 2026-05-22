package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.GlobalPos;

import java.util.Optional;

/**
 * Базовая задача мозга жителя, выполняющей работу на рабочем месте.
 * Воспроизводит звук работы, обновляет время последней работы и при необходимости пополняет запасы торговли.
 * Расширяется {@link FarmerWorkTask} для специфики фермера.
 */
public class VillagerWorkTask extends MultiTickTask<VillagerEntity> {

	private static final int RUN_TIME = 300;
	private static final double MAX_DISTANCE = 1.73;
	private static final int WORK_CHANCE = 2;

	private long lastCheckedTime;

	public VillagerWorkTask() {
		super(ImmutableMap.of(
				MemoryModuleType.JOB_SITE,
				MemoryModuleState.VALUE_PRESENT,
				MemoryModuleType.LOOK_TARGET,
				MemoryModuleState.REGISTERED
		));
	}

	@Override
	protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
		if (world.getTime() - lastCheckedTime < RUN_TIME) {
			return false;
		}

		if (world.random.nextInt(WORK_CHANCE) != 0) {
			return false;
		}

		lastCheckedTime = world.getTime();
		GlobalPos jobSite = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE).get();
		return jobSite.dimension() == world.getRegistryKey()
				&& jobSite.pos().isWithinDistance(entity.getEntityPos(), MAX_DISTANCE);
	}

	@Override
	protected void run(ServerWorld world, VillagerEntity entity, long time) {
		Brain<VillagerEntity> brain = entity.getBrain();
		brain.remember(MemoryModuleType.LAST_WORKED_AT_POI, time);
		brain.getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE)
		     .ifPresent(pos -> brain.remember(MemoryModuleType.LOOK_TARGET, new BlockPosLookTarget(pos.pos())));
		entity.playWorkSound();
		performAdditionalWork(world, entity);

		if (entity.shouldRestock(world)) {
			entity.restock();
		}
	}

	protected void performAdditionalWork(ServerWorld world, VillagerEntity entity) {
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
		Optional<GlobalPos> jobSiteOpt = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE);

		if (jobSiteOpt.isEmpty()) {
			return false;
		}

		GlobalPos jobSite = jobSiteOpt.get();
		return jobSite.dimension() == world.getRegistryKey()
				&& jobSite.pos().isWithinDistance(entity.getEntityPos(), MAX_DISTANCE);
	}
}
