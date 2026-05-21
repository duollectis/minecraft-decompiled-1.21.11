package net.minecraft.util;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * {@code CachedMapper}.
 */
public class CachedMapper<K, V> {

	private final Function<K, V> mapper;
	private @Nullable K cachedInput = (K) null;
	private @Nullable V cachedOutput;

	public CachedMapper(Function<K, V> mapper) {
		this.mapper = mapper;
	}

	/**
	 * Map.
	 *
	 * @param input input
	 *
	 * @return V — результат операции
	 */
	public V map(K input) {
		if (this.cachedOutput == null || !Objects.equals(this.cachedInput, input)) {
			this.cachedOutput = this.mapper.apply(input);
			this.cachedInput = input;
		}

		return this.cachedOutput;
	}
}
