package net.minecraft.loot.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.context.ContextParameter;

import java.util.Set;

/**
 * Условие лута: срабатывает с вероятностью, зависящей от уровня зачарования атакующей сущности.
 * Если атакующий не зачарован или отсутствует — используется базовая вероятность {@code unenchantedChance}.
 */
public record RandomChanceWithEnchantedBonusLootCondition(
	float unenchantedChance,
	EnchantmentLevelBasedValue enchantedChance,
	RegistryEntry<Enchantment> enchantment
) implements LootCondition {

	public static final MapCodec<RandomChanceWithEnchantedBonusLootCondition> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Codec.floatRange(0.0F, 1.0F)
				.fieldOf("unenchanted_chance")
				.forGetter(RandomChanceWithEnchantedBonusLootCondition::unenchantedChance),
			EnchantmentLevelBasedValue.CODEC
				.fieldOf("enchanted_chance")
				.forGetter(RandomChanceWithEnchantedBonusLootCondition::enchantedChance),
			Enchantment.ENTRY_CODEC
				.fieldOf("enchantment")
				.forGetter(RandomChanceWithEnchantedBonusLootCondition::enchantment)
		).apply(instance, RandomChanceWithEnchantedBonusLootCondition::new)
	);

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.RANDOM_CHANCE_WITH_ENCHANTED_BONUS;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of(LootContextParameters.ATTACKING_ENTITY);
	}

	@Override
	public boolean test(LootContext lootContext) {
		Entity attacker = lootContext.get(LootContextParameters.ATTACKING_ENTITY);
		int enchantLevel = attacker instanceof LivingEntity livingEntity
			? EnchantmentHelper.getEquipmentLevel(enchantment, livingEntity)
			: 0;
		float chance = enchantLevel > 0 ? enchantedChance.getValue(enchantLevel) : unenchantedChance;
		return lootContext.getRandom().nextFloat() < chance;
	}

	public static LootCondition.Builder builder(
		RegistryWrapper.WrapperLookup registries,
		float base,
		float perLevelAboveFirst
	) {
		RegistryWrapper.Impl<Enchantment> enchantmentRegistry = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		return () -> new RandomChanceWithEnchantedBonusLootCondition(
			base,
			new EnchantmentLevelBasedValue.Linear(base + perLevelAboveFirst, perLevelAboveFirst),
			enchantmentRegistry.getOrThrow(Enchantments.LOOTING)
		);
	}
}
