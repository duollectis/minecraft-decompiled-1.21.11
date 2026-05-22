package net.minecraft.util.dynamic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.function.Function;

/**
 * Иммутабельный диапазон значений с включёнными границами.
 * Используется для валидации числовых параметров через кодеки.
 *
 * @param <T>          тип значений (должен реализовывать {@link Comparable})
 * @param minInclusive нижняя граница диапазона (включительно)
 * @param maxInclusive верхняя граница диапазона (включительно)
 */
public record Range<T extends Comparable<T>>(T minInclusive, T maxInclusive) {

	public static final Codec<Range<Integer>> CODEC = createCodec(Codec.INT);

	public Range {
		if (minInclusive.compareTo(maxInclusive) > 0) {
			throw new IllegalArgumentException("min_inclusive must be less than or equal to max_inclusive");
		}
	}

	public Range(T value) {
		this(value, value);
	}

	public static <T extends Comparable<T>> Codec<Range<T>> createCodec(Codec<T> elementCodec) {
		return Codecs.createCodecForPairObject(
			elementCodec,
			"min_inclusive",
			"max_inclusive",
			Range::validate,
			Range::minInclusive,
			Range::maxInclusive
		);
	}

	/**
	 * Создаёт кодек с дополнительной проверкой, что диапазон укладывается
	 * в допустимые границы [{@code minInclusive}, {@code maxInclusive}].
	 */
	public static <T extends Comparable<T>> Codec<Range<T>> createRangedCodec(
		Codec<T> codec,
		T minInclusive,
		T maxInclusive
	) {
		return createCodec(codec).validate(range -> {
			if (range.minInclusive().compareTo(minInclusive) < 0) {
				return DataResult.error(
					() -> "Range limit too low, expected at least " + minInclusive
						+ " [" + range.minInclusive() + "-" + range.maxInclusive() + "]"
				);
			}

			return range.maxInclusive().compareTo(maxInclusive) > 0
				? DataResult.error(
					() -> "Range limit too high, expected at most " + maxInclusive
						+ " [" + range.minInclusive() + "-" + range.maxInclusive() + "]"
				)
				: DataResult.success(range);
		});
	}

	public static <T extends Comparable<T>> DataResult<Range<T>> validate(T minInclusive, T maxInclusive) {
		return minInclusive.compareTo(maxInclusive) <= 0
			? DataResult.success(new Range<>(minInclusive, maxInclusive))
			: DataResult.error(() -> "min_inclusive must be less than or equal to max_inclusive");
	}

	@SuppressWarnings("unchecked")
	public <S extends Comparable<S>> Range<S> map(Function<? super T, ? extends S> f) {
		return new Range<>((S) f.apply(minInclusive), (S) f.apply(maxInclusive));
	}

	public boolean contains(T value) {
		return value.compareTo(minInclusive) >= 0 && value.compareTo(maxInclusive) <= 0;
	}

	public boolean contains(Range<T> other) {
		return other.minInclusive().compareTo(minInclusive) >= 0
			&& other.maxInclusive.compareTo(maxInclusive) <= 0;
	}

	@Override
	public String toString() {
		return "[" + minInclusive + ", " + maxInclusive + "]";
	}
}
