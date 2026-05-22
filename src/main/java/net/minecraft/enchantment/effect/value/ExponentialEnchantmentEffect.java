package net.minecraft.enchantment.effect.value;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.effect.EnchantmentValueEffect;
import net.minecraft.util.math.random.Random;

/**
 * Эффект зачарования, умножающий входное значение на {@code base^exponent},
 * где оба параметра зависят от уровня зачарования.
 */
public record ExponentialEnchantmentEffect(
	EnchantmentLevelBasedValue base,
	EnchantmentLevelBasedValue exponent
) implements EnchantmentValueEffect {

	public static final MapCodec<ExponentialEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			EnchantmentLevelBasedValue.CODEC.fieldOf("base").forGetter(ExponentialEnchantmentEffect::base),
			EnchantmentLevelBasedValue.CODEC.fieldOf("exponent").forGetter(ExponentialEnchantmentEffect::exponent)
		)
		.apply(instance, ExponentialEnchantmentEffect::new)
	);

	@Override
	public float apply(int level, Random random, float inputValue) {
		return (float) (inputValue * Math.pow(base.getValue(level), exponent.getValue(level)));
	}

	@Override
	public MapCodec<ExponentialEnchantmentEffect> getCodec() {
		return CODEC;
	}

}
