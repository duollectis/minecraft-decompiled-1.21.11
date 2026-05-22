package net.minecraft.enchantment.provider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;

import java.util.function.Function;

/**
 * Провайдер зачарований: определяет, какие зачарования и с каким уровнем
 * будут применены к предмету при его генерации (спавн моба, рейд, торговля и т.д.).
 */
public interface EnchantmentProvider {

	Codec<EnchantmentProvider> CODEC = Registries.ENCHANTMENT_PROVIDER_TYPE
		.getCodec()
		.dispatch(EnchantmentProvider::getCodec, Function.identity());

	/**
	 * Добавляет зачарования в билдер компонента предмета.
	 *
	 * @param stack предмет, для которого генерируются зачарования
	 * @param componentBuilder билдер компонента зачарований
	 * @param random источник случайности
	 * @param localDifficulty локальная сложность в точке спавна
	 */
	void provideEnchantments(
		ItemStack stack,
		ItemEnchantmentsComponent.Builder componentBuilder,
		Random random,
		LocalDifficulty localDifficulty
	);

	MapCodec<? extends EnchantmentProvider> getCodec();

}
