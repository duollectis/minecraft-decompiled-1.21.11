package net.minecraft.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.enchantment.effect.value.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.random.Random;

import java.util.function.Function;

/**
 * Эффект зачарования, модифицирующий числовое значение (урон, опыт, количество снарядов и т.д.).
 * Применяется через цепочку вызовов {@link #apply} с накоплением результата.
 */
public interface EnchantmentValueEffect {

	Codec<EnchantmentValueEffect> CODEC = Registries.ENCHANTMENT_VALUE_EFFECT_TYPE
			.getCodec()
			.dispatch(EnchantmentValueEffect::getCodec, Function.identity());

	/**
	 * Регистрирует все реализации эффектов в реестре и возвращает реализацию по умолчанию.
	 * Вызывается при инициализации реестра {@code ENCHANTMENT_VALUE_EFFECT_TYPE}.
	 */
	static MapCodec<? extends EnchantmentValueEffect> registerAndGetDefault(
			Registry<MapCodec<? extends EnchantmentValueEffect>> registry
	) {
		Registry.register(registry, "add", AddEnchantmentEffect.CODEC);
		Registry.register(registry, "all_of", AllOfEnchantmentEffects.ValueEffects.CODEC);
		Registry.register(registry, "multiply", MultiplyEnchantmentEffect.CODEC);
		Registry.register(registry, "remove_binomial", RemoveBinomialEnchantmentEffect.CODEC);
		Registry.register(registry, "exponential", ExponentialEnchantmentEffect.CODEC);

		return Registry.register(registry, "set", SetEnchantmentEffect.CODEC);
	}

	float apply(int level, Random random, float inputValue);

	MapCodec<? extends EnchantmentValueEffect> getCodec();
}
