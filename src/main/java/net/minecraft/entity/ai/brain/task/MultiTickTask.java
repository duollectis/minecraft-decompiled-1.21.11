package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Базовый класс многотиковой задачи мозга.
 * Задача выполняется в течение случайного времени в диапазоне [{@code minRunTime}, {@code maxRunTime}]
 * и требует наличия определённых состояний памяти для запуска.
 *
 * @param <E> тип сущности
 */
public abstract class MultiTickTask<E extends LivingEntity> implements Task<E> {

	public static final int DEFAULT_RUN_TIME = 60;

	protected final Map<MemoryModuleType<?>, MemoryModuleState> requiredMemoryStates;
	private MultiTickTask.Status status = MultiTickTask.Status.STOPPED;
	private long endTime;
	private final int minRunTime;
	private final int maxRunTime;

	public MultiTickTask(Map<MemoryModuleType<?>, MemoryModuleState> requiredMemoryState) {
		this(requiredMemoryState, DEFAULT_RUN_TIME);
	}

	public MultiTickTask(Map<MemoryModuleType<?>, MemoryModuleState> requiredMemoryState, int runTime) {
		this(requiredMemoryState, runTime, runTime);
	}

	public MultiTickTask(
			Map<MemoryModuleType<?>, MemoryModuleState> requiredMemoryState,
			int minRunTime,
			int maxRunTime
	) {
		this.minRunTime = minRunTime;
		this.maxRunTime = maxRunTime;
		requiredMemoryStates = requiredMemoryState;
	}

	@Override
	public MultiTickTask.Status getStatus() {
		return status;
	}

	@Override
	public final boolean tryStarting(ServerWorld world, E entity, long time) {
		if (!hasRequiredMemoryState(entity) || !shouldRun(world, entity)) {
			return false;
		}

		status = MultiTickTask.Status.RUNNING;
		int runTime = minRunTime + world.getRandom().nextInt(maxRunTime + 1 - minRunTime);
		endTime = time + runTime;
		run(world, entity, time);
		return true;
	}

	protected void run(ServerWorld world, E entity, long time) {
	}

	@Override
	public final void tick(ServerWorld world, E entity, long time) {
		if (!isTimeLimitExceeded(time) && shouldKeepRunning(world, entity, time)) {
			keepRunning(world, entity, time);
		} else {
			stop(world, entity, time);
		}
	}

	protected void keepRunning(ServerWorld world, E entity, long time) {
	}

	@Override
	public final void stop(ServerWorld world, E entity, long time) {
		status = MultiTickTask.Status.STOPPED;
		finishRunning(world, entity, time);
	}

	protected void finishRunning(ServerWorld world, E entity, long time) {
	}

	/** Переопределяется подклассами для продления выполнения задачи сверх минимального времени. */
	protected boolean shouldKeepRunning(ServerWorld world, E entity, long time) {
		return false;
	}

	protected boolean isTimeLimitExceeded(long time) {
		return time > endTime;
	}

	/** Переопределяется подклассами для добавления дополнительных условий запуска задачи. */
	protected boolean shouldRun(ServerWorld world, E entity) {
		return true;
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	protected boolean hasRequiredMemoryState(E entity) {
		for (Entry<MemoryModuleType<?>, MemoryModuleState> entry : requiredMemoryStates.entrySet()) {
			MemoryModuleType<?> memoryType = entry.getKey();
			MemoryModuleState requiredState = entry.getValue();

			if (!entity.getBrain().isMemoryInState(memoryType, requiredState)) {
				return false;
			}
		}

		return true;
	}

	public enum Status {
		STOPPED,
		RUNNING
	}
}
