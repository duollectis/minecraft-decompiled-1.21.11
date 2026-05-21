package net.minecraft.world.attribute;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMaps;
import net.minecraft.util.math.Interpolator;

import java.util.Objects;

/**
 * {@code WeightedAttributeList}.
 */
public class WeightedAttributeList {

	private final Reference2DoubleArrayMap<EnvironmentAttributeMap> entries = new Reference2DoubleArrayMap();

	/**
	 * Clear.
	 */
	public void clear() {
		this.entries.clear();
	}

	/**
	 * Add.
	 *
	 * @param weight weight
	 * @param attributes attributes
	 *
	 * @return WeightedAttributeList — результат операции
	 */
	public WeightedAttributeList add(double weight, EnvironmentAttributeMap attributes) {
		this.entries.mergeDouble(attributes, weight, Double::sum);
		return this;
	}

	/**
	 * Interpolate.
	 *
	 * @param attribute attribute
	 * @param defaultValue default value
	 *
	 * @return Value — результат операции
	 */
	public <Value> Value interpolate(EnvironmentAttribute<Value> attribute, Value defaultValue) {
		if (this.entries.isEmpty()) {
			return defaultValue;
		}
		else if (this.entries.size() == 1) {
			EnvironmentAttributeMap
					environmentAttributeMap =
					(EnvironmentAttributeMap) this.entries.keySet().iterator().next();
			return environmentAttributeMap.apply(attribute, defaultValue);
		}
		else {
			Interpolator<Value> interpolator = attribute.getType().spatialLerp();
			Value object = null;
			double d = 0.0;
			ObjectIterator var7 = Reference2DoubleMaps.fastIterable(this.entries).iterator();

			while (var7.hasNext()) {
				Entry<EnvironmentAttributeMap> entry = (Entry<EnvironmentAttributeMap>) var7.next();
				EnvironmentAttributeMap environmentAttributeMap2 = (EnvironmentAttributeMap) entry.getKey();
				double e = entry.getDoubleValue();
				Value object2 = environmentAttributeMap2.apply(attribute, defaultValue);
				d += e;
				if (object == null) {
					object = object2;
				}
				else {
					float f = (float) (e / d);
					object = interpolator.apply(f, object, object2);
				}
			}

			return Objects.requireNonNull(object);
		}
	}
}
