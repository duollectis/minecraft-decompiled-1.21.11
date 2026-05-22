package net.minecraft.entity.ai.brain.task;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.WeightedList;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Составная задача мозга, объединяющая несколько подзадач.
 * Поддерживает упорядоченный или перемешанный порядок запуска ({@link Order})
 * и режимы выполнения одной или всех подзадач ({@link RunMode}).
 *
 * @param <E> тип сущности
 */
public class CompositeTask<E extends LivingEntity> implements Task<E> {

	private final Map<MemoryModuleType<?>, MemoryModuleState> requiredMemoryState;
	private final Set<MemoryModuleType<?>> memoriesToForgetWhenStopped;
	private final CompositeTask.Order order;
	private final CompositeTask.RunMode runMode;
	private final WeightedList<Task<? super E>> tasks = new WeightedList<>();
	private MultiTickTask.Status status = MultiTickTask.Status.STOPPED;

	public CompositeTask(
			Map<MemoryModuleType<?>, MemoryModuleState> requiredMemoryState,
			Set<MemoryModuleType<?>> memoriesToForgetWhenStopped,
			CompositeTask.Order order,
			CompositeTask.RunMode runMode,
			List<Pair<? extends Task<? super E>, Integer>> tasks
	) {
		this.requiredMemoryState = requiredMemoryState;
		this.memoriesToForgetWhenStopped = memoriesToForgetWhenStopped;
		this.order = order;
		this.runMode = runMode;
		tasks.forEach(task -> this.tasks.add((Task) task.getFirst(), (Integer) task.getSecond()));
	}

	@Override
	public MultiTickTask.Status getStatus() {
		return status;
	}

	private boolean shouldStart(E entity) {
		for (Entry<MemoryModuleType<?>, MemoryModuleState> entry : requiredMemoryState.entrySet()) {
			if (!entity.getBrain().isMemoryInState(entry.getKey(), entry.getValue())) {
				return false;
			}
		}

		return true;
	}

	@Override
	public final boolean tryStarting(ServerWorld world, E entity, long time) {
		if (!shouldStart(entity)) {
			return false;
		}

		status = MultiTickTask.Status.RUNNING;
		order.apply(tasks);
		runMode.run(tasks.stream(), world, entity, time);
		return true;
	}

	@Override
	public final void tick(ServerWorld world, E entity, long time) {
		tasks.stream()
				.filter(task -> task.getStatus() == MultiTickTask.Status.RUNNING)
				.forEach(task -> task.tick(world, entity, time));

		if (tasks.stream().noneMatch(task -> task.getStatus() == MultiTickTask.Status.RUNNING)) {
			stop(world, entity, time);
		}
	}

	@Override
	public final void stop(ServerWorld world, E entity, long time) {
		status = MultiTickTask.Status.STOPPED;
		tasks.stream()
				.filter(task -> task.getStatus() == MultiTickTask.Status.RUNNING)
				.forEach(task -> task.stop(world, entity, time));
		memoriesToForgetWhenStopped.forEach(entity.getBrain()::forget);
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public String toString() {
		Set<? extends Task<? super E>> running = tasks.stream()
				.filter(task -> task.getStatus() == MultiTickTask.Status.RUNNING)
				.collect(Collectors.toSet());
		return "(" + getClass().getSimpleName() + "): " + running;
	}

	public enum Order {
		ORDERED(list -> {}),
		SHUFFLED(WeightedList::shuffle);

		private final Consumer<WeightedList<?>> listModifier;

		private Order(final Consumer<WeightedList<?>> listModifier) {
			this.listModifier = listModifier;
		}

		public void apply(WeightedList<?> list) {
			listModifier.accept(list);
		}
	}

	public enum RunMode {
		RUN_ONE {
			@Override
			public <E extends LivingEntity> void run(
					Stream<Task<? super E>> tasks,
					ServerWorld world,
					E entity,
					long time
			) {
				tasks
						.filter(task -> task.getStatus() == MultiTickTask.Status.STOPPED)
						.filter(task -> task.tryStarting(world, entity, time))
						.findFirst();
			}
		},
		TRY_ALL {
			@Override
			public <E extends LivingEntity> void run(
					Stream<Task<? super E>> tasks,
					ServerWorld world,
					E entity,
					long time
			) {
				tasks
						.filter(task -> task.getStatus() == MultiTickTask.Status.STOPPED)
						.forEach(task -> task.tryStarting(world, entity, time));
			}
		};

		public abstract <E extends LivingEntity> void run(
				Stream<Task<? super E>> tasks,
				ServerWorld world,
				E entity,
				long time
		);
	}
}
