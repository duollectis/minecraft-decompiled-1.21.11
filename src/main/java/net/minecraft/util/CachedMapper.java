package net.minecraft.util;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * Кэширующий маппер, запоминающий последний результат вычисления.
 * <p>
 * Оптимизирует повторные вызовы с одинаковым входным значением:
 * если входное значение не изменилось, возвращает закэшированный результат
 * без повторного вызова функции-маппера.
 *
 * @param <K> тип входного значения
 * @param <V> тип выходного значения
 */
public class CachedMapper<K, V> {

	private final Function<K, V> mapper;
	private @Nullable K cachedInput;
	private @Nullable V cachedOutput;

	public CachedMapper(Function<K, V> mapper) {
		this.mapper = mapper;
	}

	/**
	 * Возвращает результат применения маппера к входному значению.
	 * Если входное значение совпадает с предыдущим, возвращает закэшированный результат.
	 *
	 * @param input входное значение
	 * @return результат маппинга
	 */
	public V map(K input) {
		if (cachedOutput == null || !Objects.equals(cachedInput, input)) {
			cachedOutput = mapper.apply(input);
			cachedInput = input;
		}

		return cachedOutput;
	}
}
