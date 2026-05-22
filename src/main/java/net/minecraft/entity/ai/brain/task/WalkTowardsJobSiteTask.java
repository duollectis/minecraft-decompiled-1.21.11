package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.poi.PointOfInterestStorage;

import java.util.Optional;

/**
 * Задача мозга жителя, идущего к потенциальному рабочему месту.
 * По завершении освобождает тикет POI и сбрасывает память о потенциальном рабочем месте.
 */
public class WalkTowardsJobSiteTask extends MultiTickTask<VillagerEntity> {

	private static final int RUN_TIME = 1200;
	private final float speed;

	public WalkTowardsJobSiteTask(float speed) {
		super(ImmutableMap.of(MemoryModuleType.POTENTIAL_JOB_SITE, MemoryModuleState.VALUE_PRESENT), RUN_TIME);
		this.speed = speed;
	}

	@Override
	protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
		return entity.getBrain()
		             .getFirstPossibleNonCoreActivity()
		             .map(activity -> activity == Activity.IDLE || activity == Activity.WORK || activity == Activity.PLAY)
		             .orElse(true);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
		return entity.getBrain().hasMemoryModule(MemoryModuleType.POTENTIAL_JOB_SITE);
	}

	@Override
	protected void keepRunning(ServerWorld world, VillagerEntity entity, long time) {
		TargetUtil.walkTowards(
				entity,
				entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get().pos(),
				speed,
				1
		);
	}

	@Override
	protected void finishRunning(ServerWorld world, VillagerEntity entity, long time) {
		Optional<GlobalPos> jobSiteOpt = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.POTENTIAL_JOB_SITE);

		jobSiteOpt.ifPresent(pos -> {
			BlockPos jobSitePos = pos.pos();
			ServerWorld jobSiteWorld = world.getServer().getWorld(pos.dimension());

			if (jobSiteWorld == null) {
				return;
			}

			PointOfInterestStorage poiStorage = jobSiteWorld.getPointOfInterestStorage();

			if (poiStorage.test(jobSitePos, poiType -> true)) {
				poiStorage.releaseTicket(jobSitePos);
			}

			world.getSubscriptionTracker().onPoiUpdated(jobSitePos);
		});

		entity.getBrain().forget(MemoryModuleType.POTENTIAL_JOB_SITE);
	}
}
