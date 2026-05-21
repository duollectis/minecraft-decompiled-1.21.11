package net.minecraft.entity.ai.brain;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;

import java.util.Optional;

/**
 * {@code MemoryQueryResult}.
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
		return this.value;
	}

	/**
	 * Remember.
	 *
	 * @param value value
	 */
	public void remember(Value value) {
		this.brain.remember(this.memory, Optional.of(value));
	}

	/**
	 * Remember.
	 *
	 * @param value value
	 */
	public void remember(Optional<Value> value) {
		this.brain.remember(this.memory, value);
	}

	/**
	 * Remember.
	 *
	 * @param value value
	 * @param expiry expiry
	 */
	public void remember(Value value, long expiry) {
		this.brain.remember(this.memory, value, expiry);
	}

	/**
	 * Forget.
	 */
	public void forget() {
		this.brain.forget(this.memory);
	}
}
