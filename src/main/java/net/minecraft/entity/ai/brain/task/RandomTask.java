package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;

import java.util.List;
import java.util.Map;

/**
 * Составная задача, случайно выбирающая одну из подзадач для выполнения.
 * Является удобной обёрткой над {@link CompositeTask} с режимами {@code SHUFFLED} и {@code RUN_ONE}.
 *
 * @param <E> тип сущности
 */
public class RandomTask<E extends LivingEntity> extends CompositeTask<E> {

	public RandomTask(List<Pair<? extends Task<? super E>, Integer>> tasks) {
		this(ImmutableMap.of(), tasks);
	}

	public RandomTask(
			Map<MemoryModuleType<?>, MemoryModuleState> requiredMemoryState,
			List<Pair<? extends Task<? super E>, Integer>> tasks
	) {
		super(
				requiredMemoryState,
				ImmutableSet.of(),
				CompositeTask.Order.SHUFFLED,
				CompositeTask.RunMode.RUN_ONE,
				tasks
		);
	}
}
