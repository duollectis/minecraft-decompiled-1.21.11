package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Задача мозга жителя, переключающая его в режим паники при наличии угрозы или урона.
 * Каждые {@code GOLEM_SUMMON_INTERVAL} тиков пытается призвать железного голема.
 */
public class PanicTask extends MultiTickTask<VillagerEntity> {

	private static final long GOLEM_SUMMON_INTERVAL = 100L;

	public PanicTask() {
		super(ImmutableMap.of());
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
		return wasHurt(entity) || isHostileNearby(entity);
	}

	@Override
	protected void run(ServerWorld world, VillagerEntity entity, long time) {
		if (!wasHurt(entity) && !isHostileNearby(entity)) {
			return;
		}

		Brain<?> brain = entity.getBrain();

		if (!brain.hasActivity(Activity.PANIC)) {
			brain.forget(MemoryModuleType.PATH);
			brain.forget(MemoryModuleType.WALK_TARGET);
			brain.forget(MemoryModuleType.LOOK_TARGET);
			brain.forget(MemoryModuleType.BREED_TARGET);
			brain.forget(MemoryModuleType.INTERACTION_TARGET);
		}

		brain.doExclusively(Activity.PANIC);
	}

	@Override
	protected void keepRunning(ServerWorld world, VillagerEntity entity, long time) {
		if (time % GOLEM_SUMMON_INTERVAL == 0L) {
			entity.summonGolem(world, time, 3);
		}
	}

	public static boolean isHostileNearby(LivingEntity entity) {
		return entity.getBrain().hasMemoryModule(MemoryModuleType.NEAREST_HOSTILE);
	}

	public static boolean wasHurt(LivingEntity entity) {
		return entity.getBrain().hasMemoryModule(MemoryModuleType.HURT_BY);
	}
}
