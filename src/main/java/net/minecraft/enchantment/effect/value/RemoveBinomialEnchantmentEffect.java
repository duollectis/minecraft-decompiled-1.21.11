package net.minecraft.enchantment.effect.value;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.effect.EnchantmentValueEffect;
import net.minecraft.util.math.random.Random;

/**
 * Эффект зачарования, убирающий из входного значения случайное количество единиц
 * по биномиальному распределению с вероятностью успеха {@code chance}.
 *
 * <p>При больших значениях (> 128) и подходящих параметрах используется нормальная аппроксимация
 * биномиального распределения для производительности.
 */
public record RemoveBinomialEnchantmentEffect(EnchantmentLevelBasedValue chance) implements EnchantmentValueEffect {

	public static final MapCodec<RemoveBinomialEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(EnchantmentLevelBasedValue.CODEC.fieldOf("chance").forGetter(RemoveBinomialEnchantmentEffect::chance))
			.apply(instance, RemoveBinomialEnchantmentEffect::new)
	);

	@Override
	public float apply(int level, Random random, float inputValue) {
		float probability = chance.getValue(level);
		int removed = computeRemovedCount(inputValue, probability, random);

		return inputValue - removed;
	}

	/**
	 * Вычисляет количество удаляемых единиц по биномиальному распределению.
	 * При больших значениях применяется нормальная аппроксимация (ЦПТ).
	 */
	private int computeRemovedCount(float inputValue, float probability, Random random) {
		boolean useNormalApproximation = inputValue > 128.0F
			&& inputValue * probability >= 20.0F
			&& inputValue * (1.0F - probability) >= 20.0F;

		if (useNormalApproximation) {
			double mean = Math.floor(inputValue * probability);
			double stdDev = Math.sqrt(inputValue * probability * (1.0F - probability));
			int approximated = (int) Math.round(mean + random.nextGaussian() * stdDev);

			return Math.clamp((long) approximated, 0, (int) inputValue);
		}

		int count = 0;

		for (int trial = 0; trial < inputValue; trial++) {
			if (random.nextFloat() < probability) {
				count++;
			}
		}

		return count;
	}

	@Override
	public MapCodec<RemoveBinomialEnchantmentEffect> getCodec() {
		return CODEC;
	}

}
