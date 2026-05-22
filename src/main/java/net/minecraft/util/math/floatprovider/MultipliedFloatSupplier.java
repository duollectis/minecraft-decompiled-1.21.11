package net.minecraft.util.math.floatprovider;

import net.minecraft.util.math.random.Random;

import java.util.Arrays;

/**
 * Перемножает результаты нескольких {@link FloatSupplier} и возвращает их произведение.
 */
public class MultipliedFloatSupplier implements FloatSupplier {

	private final FloatSupplier[] multipliers;

	public MultipliedFloatSupplier(FloatSupplier... multipliers) {
		this.multipliers = multipliers;
	}

	@Override
	public float get(Random random) {
		float result = 1.0F;

		for (FloatSupplier supplier : multipliers) {
			result *= supplier.get(random);
		}

		return result;
	}

	@Override
	public String toString() {
		return "MultipliedFloats" + Arrays.toString((Object[]) multipliers);
	}
}
