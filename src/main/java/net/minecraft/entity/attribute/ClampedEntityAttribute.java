package net.minecraft.entity.attribute;

import net.minecraft.util.math.MathHelper;

/**
 * Атрибут сущности с жёстко заданным допустимым диапазоном значений [{@code minValue}, {@code maxValue}].
 * Значение {@code NaN} трактуется как минимально допустимое.
 */
public class ClampedEntityAttribute extends EntityAttribute {

	private final double minValue;
	private final double maxValue;

	public ClampedEntityAttribute(String translationKey, double fallback, double minValue, double maxValue) {
		super(translationKey, fallback);
		if (minValue > maxValue) {
			throw new IllegalArgumentException("Minimum value cannot be bigger than maximum value!");
		}

		if (fallback < minValue) {
			throw new IllegalArgumentException("Default value cannot be lower than minimum value!");
		}

		if (fallback > maxValue) {
			throw new IllegalArgumentException("Default value cannot be bigger than maximum value!");
		}

		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	public double getMinValue() {
		return minValue;
	}

	public double getMaxValue() {
		return maxValue;
	}

	@Override
	public double clamp(double value) {
		return Double.isNaN(value) ? minValue : MathHelper.clamp(value, minValue, maxValue);
	}
}
