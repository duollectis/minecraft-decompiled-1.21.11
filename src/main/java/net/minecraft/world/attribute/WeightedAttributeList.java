package net.minecraft.world.attribute;

import it.unimi.dsi.fastutil.objects.Reference2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMaps;
import net.minecraft.util.math.Interpolator;

import java.util.Objects;

/**
 * Взвешенный список карт атрибутов окружения для пространственной интерполяции.
 * Используется при смешивании атрибутов нескольких биомов в одной точке мира.
 * <p>
 * Алгоритм интерполяции: последовательное взвешенное смешивание значений
 * с накоплением суммарного веса (running weighted average).
 */
public class WeightedAttributeList {

	private final Reference2DoubleArrayMap<EnvironmentAttributeMap> entries = new Reference2DoubleArrayMap<>();

	/** Очищает все записи. */
	public void clear() {
		entries.clear();
	}

	/**
	 * Добавляет карту атрибутов с заданным весом.
	 * Если карта уже присутствует — веса суммируются.
	 *
	 * @param weight вес карты атрибутов
	 * @param attributes карта атрибутов биома
	 * @return {@code this} для цепочки вызовов
	 */
	public WeightedAttributeList add(double weight, EnvironmentAttributeMap attributes) {
		entries.mergeDouble(attributes, weight, Double::sum);
		return this;
	}

	/**
	 * Вычисляет взвешенно интерполированное значение атрибута по всем добавленным картам.
	 * <p>
	 * Если список пуст — возвращает {@code defaultValue}.
	 * Если одна запись — применяет её карту напрямую без интерполяции.
	 * Иначе — последовательно смешивает значения через running weighted average.
	 *
	 * @param attribute запрашиваемый атрибут
	 * @param defaultValue значение по умолчанию (базовое значение атрибута)
	 * @return интерполированное значение
	 */
	public <Value> Value interpolate(EnvironmentAttribute<Value> attribute, Value defaultValue) {
		if (entries.isEmpty()) {
			return defaultValue;
		}

		if (entries.size() == 1) {
			EnvironmentAttributeMap singleMap = entries.keySet().iterator().next();
			return singleMap.apply(attribute, defaultValue);
		}

		Interpolator<Value> interpolator = attribute.getType().spatialLerp();
		Value accumulated = null;
		double totalWeight = 0.0;

		for (Reference2DoubleMap.Entry<EnvironmentAttributeMap> entry : Reference2DoubleMaps.fastIterable(entries)) {
			EnvironmentAttributeMap attributeMap = entry.getKey();
			double weight = entry.getDoubleValue();
			Value mapValue = attributeMap.apply(attribute, defaultValue);
			totalWeight += weight;

			if (accumulated == null) {
				accumulated = mapValue;
			} else {
				float blendFactor = (float) (weight / totalWeight);
				accumulated = interpolator.apply(blendFactor, accumulated, mapValue);
			}
		}

		return Objects.requireNonNull(accumulated);
	}
}
