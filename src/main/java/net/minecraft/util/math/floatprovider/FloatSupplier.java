package net.minecraft.util.math.floatprovider;

import net.minecraft.util.math.random.Random;

/**
 * Функциональный интерфейс для генерации случайного значения типа {@code float}.
 */
public interface FloatSupplier {

	float get(Random random);
}
