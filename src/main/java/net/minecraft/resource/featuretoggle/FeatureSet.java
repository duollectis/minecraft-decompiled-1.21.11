package net.minecraft.resource.featuretoggle;

import it.unimi.dsi.fastutil.HashCommon;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

/**
 * Иммутабельное множество активных флагов функций, представленное битовой маской {@code long}.
 *
 * <p>Все флаги в наборе должны принадлежать одной {@link FeatureUniverse}.
 * Пустой набор ({@link #empty()}) не привязан ни к какой вселенной и является
 * подмножеством любого другого набора.</p>
 */
public final class FeatureSet {

	private static final FeatureSet EMPTY = new FeatureSet(null, 0L);

	/** Максимальное количество флагов в одной вселенной (ограничено размером {@code long}). */
	public static final int MAX_FEATURE_FLAGS = 64;

	private final @Nullable FeatureUniverse universe;
	private final long featuresMask;

	private FeatureSet(@Nullable FeatureUniverse universe, long featuresMask) {
		this.universe = universe;
		this.featuresMask = featuresMask;
	}

	static FeatureSet of(FeatureUniverse universe, Collection<FeatureFlag> features) {
		if (features.isEmpty()) {
			return EMPTY;
		}

		long mask = combineMask(universe, 0L, features);
		return new FeatureSet(universe, mask);
	}

	public static FeatureSet empty() {
		return EMPTY;
	}

	public static FeatureSet of(FeatureFlag feature) {
		return new FeatureSet(feature.universe, feature.mask);
	}

	public static FeatureSet of(FeatureFlag feature1, FeatureFlag... features) {
		long mask = features.length == 0
				? feature1.mask
				: combineMask(feature1.universe, feature1.mask, Arrays.asList(features));

		return new FeatureSet(feature1.universe, mask);
	}

	/**
	 * Объединяет маски флагов, проверяя принадлежность к одной вселенной.
	 *
	 * @throws IllegalStateException если флаг принадлежит другой вселенной
	 */
	private static long combineMask(FeatureUniverse universe, long featuresMask, Iterable<FeatureFlag> newFeatures) {
		for (FeatureFlag featureFlag : newFeatures) {
			if (universe != featureFlag.universe) {
				throw new IllegalStateException(
						"Mismatched feature universe, expected '" + universe + "', but got '" + featureFlag.universe + "'"
				);
			}

			featuresMask |= featureFlag.mask;
		}

		return featuresMask;
	}

	public boolean contains(FeatureFlag feature) {
		return universe == feature.universe && (featuresMask & feature.mask) != 0L;
	}

	public boolean isEmpty() {
		return equals(EMPTY);
	}

	/**
	 * Проверяет, является ли этот набор подмножеством переданного.
	 *
	 * <p>Пустой набор является подмножеством любого другого набора.
	 * Наборы из разных вселенных не являются подмножествами друг друга.</p>
	 *
	 * @param features набор для сравнения
	 * @return {@code true}, если все флаги этого набора присутствуют в {@code features}
	 */
	public boolean isSubsetOf(FeatureSet features) {
		if (universe == null) {
			return true;
		}

		return universe == features.universe && (featuresMask & ~features.featuresMask) == 0L;
	}

	/**
	 * Проверяет, пересекаются ли два набора (имеют хотя бы один общий флаг).
	 *
	 * @param features набор для сравнения
	 * @return {@code true}, если наборы из одной вселенной и имеют общие флаги
	 */
	public boolean intersects(FeatureSet features) {
		return universe != null
				&& features.universe != null
				&& universe == features.universe
				&& (featuresMask & features.featuresMask) != 0L;
	}

	/**
	 * Возвращает объединение двух наборов (логическое ИЛИ масок).
	 *
	 * @param features набор для объединения
	 * @return новый набор, содержащий флаги из обоих наборов
	 * @throws IllegalArgumentException если наборы принадлежат разным вселенным
	 */
	public FeatureSet combine(FeatureSet features) {
		if (universe == null) {
			return features;
		}

		if (features.universe == null) {
			return this;
		}

		if (universe != features.universe) {
			throw new IllegalArgumentException(
					"Mismatched set elements: '" + universe + "' != '" + features.universe + "'"
			);
		}

		return new FeatureSet(universe, featuresMask | features.featuresMask);
	}

	/**
	 * Возвращает разность наборов (флаги этого набора, отсутствующие в {@code features}).
	 *
	 * @param features набор вычитаемых флагов
	 * @return новый набор без флагов из {@code features}
	 * @throws IllegalArgumentException если наборы принадлежат разным вселенным
	 */
	public FeatureSet subtract(FeatureSet features) {
		if (universe == null || features.universe == null) {
			return this;
		}

		if (universe != features.universe) {
			throw new IllegalArgumentException(
					"Mismatched set elements: '" + universe + "' != '" + features.universe + "'"
			);
		}

		long result = featuresMask & ~features.featuresMask;
		return result == 0L ? EMPTY : new FeatureSet(universe, result);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		return o instanceof FeatureSet other
				&& universe == other.universe
				&& featuresMask == other.featuresMask;
	}

	@Override
	public int hashCode() {
		return (int) HashCommon.mix(featuresMask);
	}
}
