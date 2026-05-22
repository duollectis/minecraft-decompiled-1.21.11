package net.minecraft.entity.ai.brain;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;

import java.util.Optional;

/**
 * Результат выполнения {@link MemoryQuery} — обёртка над значением памяти,
 * позволяющая задаче читать и записывать память через типобезопасный API.
 *
 * @param <F>     тип функтора (Const, OptionalBox или IdF)
 * @param <Value> тип значения памяти
 */
public final class MemoryQueryResult<F extends K1, Value> {

	private final Brain<?> brain;
	private final MemoryModuleType<Value> memory;
	private final App<F, Value> value;

	public MemoryQueryResult(Brain<?> brain, MemoryModuleType<Value> memory, App<F, Value> value) {
		this.brain = brain;
		this.memory = memory;
		this.value = value;
	}

	public App<F, Value> getValue() {
		return value;
	}

	public void remember(Value value) {
		brain.remember(memory, Optional.of(value));
	}

	public void remember(Optional<Value> value) {
		brain.remember(memory, value);
	}

	public void remember(Value value, long expiry) {
		brain.remember(memory, value, expiry);
	}

	public void forget() {
		brain.forget(memory);
	}
}
