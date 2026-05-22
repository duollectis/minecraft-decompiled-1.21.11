package net.minecraft.enchantment.provider;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;

/**
 * Провайдер зачарований, масштабирующий стоимость зачарования в зависимости от локальной сложности.
 * Итоговая стоимость: {@code minCost + random(0, clampedDifficulty * maxCostSpan)}.
 */
public record ByCostWithDifficultyEnchantmentProvider(
	RegistryEntryList<Enchantment> enchantments,
	int minCost,
	int maxCostSpan
) implements EnchantmentProvider {

	public static final int MAX_COST = 10000;

	public static final MapCodec<ByCostWithDifficultyEnchantmentProvider> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			RegistryCodecs.entryList(RegistryKeys.ENCHANTMENT)
				.fieldOf("enchantments")
				.forGetter(ByCostWithDifficultyEnchantmentProvider::enchantments),
			Codecs.rangedInt(1, MAX_COST)
				.fieldOf("min_cost")
				.forGetter(ByCostWithDifficultyEnchantmentProvider::minCost),
			Codecs.rangedInt(0, MAX_COST)
				.fieldOf("max_cost_span")
				.forGetter(ByCostWithDifficultyEnchantmentProvider::maxCostSpan)
		)
		.apply(instance, ByCostWithDifficultyEnchantmentProvider::new)
	);

	@Override
	public void provideEnchantments(
		ItemStack stack,
		ItemEnchantmentsComponent.Builder componentBuilder,
		Random random,
		LocalDifficulty localDifficulty
	) {
		float difficulty = localDifficulty.getClampedLocalDifficulty();
		int rolledCost = MathHelper.nextBetween(random, minCost, minCost + (int) (difficulty * maxCostSpan));

		for (EnchantmentLevelEntry entry : EnchantmentHelper.generateEnchantments(random, stack, rolledCost, enchantments.stream())) {
			componentBuilder.add(entry.enchantment(), entry.level());
		}
	}

	@Override
	public MapCodec<ByCostWithDifficultyEnchantmentProvider> getCodec() {
		return CODEC;
	}

}
