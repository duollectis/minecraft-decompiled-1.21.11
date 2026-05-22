package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;

/**
 * Задача мозга, уменьшающая счётчик кулдауна в указанном модуле памяти на 1 каждый тик.
 * По достижении нуля забывает модуль памяти, тем самым снимая кулдаун.
 */
public class TickCooldownTask extends MultiTickTask<LivingEntity> {

	private final MemoryModuleType<Integer> cooldownModule;

	public TickCooldownTask(MemoryModuleType<Integer> cooldownModule) {
		super(ImmutableMap.of(cooldownModule, MemoryModuleState.VALUE_PRESENT));
		this.cooldownModule = cooldownModule;
	}

	private Optional<Integer> getRemainingCooldownTicks(LivingEntity entity) {
		return entity.getBrain().getOptionalRegisteredMemory(cooldownModule);
	}

	@Override
	protected boolean isTimeLimitExceeded(long time) {
		return false;
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, LivingEntity entity, long time) {
		Optional<Integer> remainingTicks = getRemainingCooldownTicks(entity);
		return remainingTicks.isPresent() && remainingTicks.get() > 0;
	}

	@Override
	protected void keepRunning(ServerWorld world, LivingEntity entity, long time) {
		Optional<Integer> remainingTicks = getRemainingCooldownTicks(entity);
		entity.getBrain().remember(cooldownModule, remainingTicks.get() - 1);
	}

	@Override
	protected void finishRunning(ServerWorld world, LivingEntity entity, long time) {
		entity.getBrain().forget(cooldownModule);
	}
}
