package net.minecraft.entity.ai.brain;

import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.Const.Mu;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.OptionalBox;
import com.mojang.datafixers.util.Unit;
import org.jspecify.annotations.Nullable;

/**
 * Запрос к памяти мозга с типизированным результатом.
 * Используется в {@link net.minecraft.entity.ai.brain.task.TaskTriggerer} для декларативного
 * описания требований задачи к состоянию памяти.
 *
 * <p>Три варианта запроса:
 * <ul>
 *   <li>{@link Absent} — память должна отсутствовать</li>
 *   <li>{@link Optional} — память может присутствовать или отсутствовать</li>
 *   <li>{@link MemoryValue} — память должна присутствовать</li>
 * </ul>
 *
 * @param <F>   тип функтора результата (Const, OptionalBox или IdF)
 * @param <Val> тип значения памяти
 */
public interface MemoryQuery<F extends K1, Val> {

	MemoryModuleType<Val> memory();

	MemoryModuleState getState();

	/**
	 * Преобразует текущее значение памяти в типизированный результат запроса.
	 * Возвращает {@code null}, если условие запроса не выполнено.
	 */
	@Nullable MemoryQueryResult<F, Val> toQueryResult(Brain<?> brain, java.util.Optional<Val> value);

	/** Запрос, требующий отсутствия значения в памяти. */
	record Absent<V>(MemoryModuleType<V> memory) implements MemoryQuery<Mu<Unit>, V> {

		@Override
		public MemoryModuleState getState() {
			return MemoryModuleState.VALUE_ABSENT;
		}

		@Override
		public MemoryQueryResult<Mu<Unit>, V> toQueryResult(Brain<?> brain, java.util.Optional<V> value) {
			return value.isPresent() ? null : new MemoryQueryResult<>(brain, memory, Const.create(Unit.INSTANCE));
		}
	}

	/** Запрос, допускающий любое состояние памяти (присутствует или отсутствует). */
	record Optional<V>(MemoryModuleType<V> memory) implements MemoryQuery<OptionalBox.Mu, V> {

		@Override
		public MemoryModuleState getState() {
			return MemoryModuleState.REGISTERED;
		}

		@Override
		public MemoryQueryResult<OptionalBox.Mu, V> toQueryResult(
				Brain<?> brain,
				java.util.Optional<V> value
		) {
			return new MemoryQueryResult<>(brain, memory, OptionalBox.create(value));
		}
	}

	/** Запрос, требующий наличия значения в памяти. */
	record MemoryValue<V>(MemoryModuleType<V> memory) implements MemoryQuery<IdF.Mu, V> {

		@Override
		public MemoryModuleState getState() {
			return MemoryModuleState.VALUE_PRESENT;
		}

		@Override
		public MemoryQueryResult<IdF.Mu, V> toQueryResult(
				Brain<?> brain,
				java.util.Optional<V> value
		) {
			return value.isEmpty() ? null : new MemoryQueryResult<>(brain, memory, IdF.create(value.get()));
		}
	}
}
