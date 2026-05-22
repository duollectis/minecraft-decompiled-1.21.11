package net.minecraft.enchantment.provider;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;

/**
 * Реестровые ключи и bootstrap-метод для провайдеров зачарований торговли
 * в рамках экспериментального ребаланса торговли с жителями.
 * Каждый провайдер привязан к конкретному биому кузнеца и уровню торговли.
 */
public interface TradeRebalanceEnchantmentProviders {

	RegistryKey<EnchantmentProvider> DESERT_ARMORER_BOOTS_4 = EnchantmentProviders.of("trades/desert_armorer_boots_4");
	RegistryKey<EnchantmentProvider> DESERT_ARMORER_LEGGINGS_4 = EnchantmentProviders.of("trades/desert_armorer_leggings_4");
	RegistryKey<EnchantmentProvider> DESERT_ARMORER_CHESTPLATE_4 = EnchantmentProviders.of("trades/desert_armorer_chestplate_4");
	RegistryKey<EnchantmentProvider> DESERT_ARMORER_HELMET_4 = EnchantmentProviders.of("trades/desert_armorer_helmet_4");
	RegistryKey<EnchantmentProvider> DESERT_ARMORER_LEGGINGS_5 = EnchantmentProviders.of("trades/desert_armorer_leggings_5");
	RegistryKey<EnchantmentProvider> DESERT_ARMORER_CHESTPLATE_5 = EnchantmentProviders.of("trades/desert_armorer_chestplate_5");

	RegistryKey<EnchantmentProvider> PLAINS_ARMORER_BOOTS_4 = EnchantmentProviders.of("trades/plains_armorer_boots_4");
	RegistryKey<EnchantmentProvider> PLAINS_ARMORER_LEGGINGS_4 = EnchantmentProviders.of("trades/plains_armorer_leggings_4");
	RegistryKey<EnchantmentProvider> PLAINS_ARMORER_CHESTPLATE_4 = EnchantmentProviders.of("trades/plains_armorer_chestplate_4");
	RegistryKey<EnchantmentProvider> PLAINS_ARMORER_HELMET_4 = EnchantmentProviders.of("trades/plains_armorer_helmet_4");
	RegistryKey<EnchantmentProvider> PLAINS_ARMORER_BOOTS_5 = EnchantmentProviders.of("trades/plains_armorer_boots_5");
	RegistryKey<EnchantmentProvider> PLAINS_ARMORER_LEGGINGS_5 = EnchantmentProviders.of("trades/plains_armorer_leggings_5");

	RegistryKey<EnchantmentProvider> SAVANNA_ARMORER_BOOTS_4 = EnchantmentProviders.of("trades/savanna_armorer_boots_4");
	RegistryKey<EnchantmentProvider> SAVANNA_ARMORER_LEGGINGS_4 = EnchantmentProviders.of("trades/savanna_armorer_leggings_4");
	RegistryKey<EnchantmentProvider> SAVANNA_ARMORER_CHESTPLATE_4 = EnchantmentProviders.of("trades/savanna_armorer_chestplate_4");
	RegistryKey<EnchantmentProvider> SAVANNA_ARMORER_HELMET_4 = EnchantmentProviders.of("trades/savanna_armorer_helmet_4");
	RegistryKey<EnchantmentProvider> SAVANNA_ARMORER_CHESTPLATE_5 = EnchantmentProviders.of("trades/savanna_armorer_chestplate_5");
	RegistryKey<EnchantmentProvider> SAVANNA_ARMORER_HELMET_5 = EnchantmentProviders.of("trades/savanna_armorer_helmet_5");

	RegistryKey<EnchantmentProvider> SNOW_ARMORER_BOOTS_4 = EnchantmentProviders.of("trades/snow_armorer_boots_4");
	RegistryKey<EnchantmentProvider> SNOW_ARMORER_HELMET_4 = EnchantmentProviders.of("trades/snow_armorer_helmet_4");
	RegistryKey<EnchantmentProvider> SNOW_ARMORER_BOOTS_5 = EnchantmentProviders.of("trades/snow_armorer_boots_5");
	RegistryKey<EnchantmentProvider> SNOW_ARMORER_HELMET_5 = EnchantmentProviders.of("trades/snow_armorer_helmet_5");

	RegistryKey<EnchantmentProvider> JUNGLE_ARMORER_BOOTS_4 = EnchantmentProviders.of("trades/jungle_armorer_boots_4");
	RegistryKey<EnchantmentProvider> JUNGLE_ARMORER_LEGGINGS_4 = EnchantmentProviders.of("trades/jungle_armorer_leggings_4");
	RegistryKey<EnchantmentProvider> JUNGLE_ARMORER_CHESTPLATE_4 = EnchantmentProviders.of("trades/jungle_armorer_chestplate_4");
	RegistryKey<EnchantmentProvider> JUNGLE_ARMORER_HELMET_4 = EnchantmentProviders.of("trades/jungle_armorer_helmet_4");
	RegistryKey<EnchantmentProvider> JUNGLE_ARMORER_BOOTS_5 = EnchantmentProviders.of("trades/jungle_armorer_boots_5");
	RegistryKey<EnchantmentProvider> JUNGLE_ARMORER_HELMET_5 = EnchantmentProviders.of("trades/jungle_armorer_helmet_5");

	RegistryKey<EnchantmentProvider> SWAMP_ARMORER_BOOTS_4 = EnchantmentProviders.of("trades/swamp_armorer_boots_4");
	RegistryKey<EnchantmentProvider> SWAMP_ARMORER_LEGGINGS_4 = EnchantmentProviders.of("trades/swamp_armorer_leggings_4");
	RegistryKey<EnchantmentProvider> SWAMP_ARMORER_CHESTPLATE_4 = EnchantmentProviders.of("trades/swamp_armorer_chestplate_4");
	RegistryKey<EnchantmentProvider> SWAMP_ARMORER_HELMET_4 = EnchantmentProviders.of("trades/swamp_armorer_helmet_4");
	RegistryKey<EnchantmentProvider> SWAMP_ARMORER_BOOTS_5 = EnchantmentProviders.of("trades/swamp_armorer_boots_5");
	RegistryKey<EnchantmentProvider> SWAMP_ARMORER_HELMET_5 = EnchantmentProviders.of("trades/swamp_armorer_helmet_5");

	RegistryKey<EnchantmentProvider> TAIGA_ARMORER_LEGGINGS_5 = EnchantmentProviders.of("trades/taiga_armorer_leggings_5");
	RegistryKey<EnchantmentProvider> TAIGA_ARMORER_CHESTPLATE_5 = EnchantmentProviders.of("trades/taiga_armorer_chestplate_5");

	static void bootstrap(Registerable<EnchantmentProvider> registry) {
		RegistryEntryLookup<Enchantment> enchantments = registry.getRegistryLookup(RegistryKeys.ENCHANTMENT);

		IntProvider level1 = ConstantIntProvider.create(1);

		registry.register(DESERT_ARMORER_BOOTS_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.THORNS), level1));
		registry.register(DESERT_ARMORER_LEGGINGS_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.THORNS), level1));
		registry.register(DESERT_ARMORER_CHESTPLATE_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.THORNS), level1));
		registry.register(DESERT_ARMORER_HELMET_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.THORNS), level1));
		registry.register(DESERT_ARMORER_LEGGINGS_5, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.THORNS), level1));
		registry.register(DESERT_ARMORER_CHESTPLATE_5, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.THORNS), level1));

		registry.register(PLAINS_ARMORER_BOOTS_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.PROTECTION), level1));
		registry.register(PLAINS_ARMORER_LEGGINGS_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.PROTECTION), level1));
		registry.register(PLAINS_ARMORER_CHESTPLATE_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.PROTECTION), level1));
		registry.register(PLAINS_ARMORER_HELMET_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.PROTECTION), level1));
		registry.register(PLAINS_ARMORER_BOOTS_5, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.PROTECTION), level1));
		registry.register(PLAINS_ARMORER_LEGGINGS_5, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.PROTECTION), level1));

		registry.register(SAVANNA_ARMORER_BOOTS_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.BINDING_CURSE), level1));
		registry.register(SAVANNA_ARMORER_LEGGINGS_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.BINDING_CURSE), level1));
		registry.register(SAVANNA_ARMORER_CHESTPLATE_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.BINDING_CURSE), level1));
		registry.register(SAVANNA_ARMORER_HELMET_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.BINDING_CURSE), level1));
		registry.register(SAVANNA_ARMORER_CHESTPLATE_5, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.BINDING_CURSE), level1));
		registry.register(SAVANNA_ARMORER_HELMET_5, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.BINDING_CURSE), level1));

		registry.register(SNOW_ARMORER_BOOTS_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.FROST_WALKER), level1));
		registry.register(SNOW_ARMORER_HELMET_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.AQUA_AFFINITY), level1));
		registry.register(SNOW_ARMORER_BOOTS_5, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.FROST_WALKER), level1));
		registry.register(SNOW_ARMORER_HELMET_5, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.AQUA_AFFINITY), level1));

		registry.register(JUNGLE_ARMORER_BOOTS_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.UNBREAKING), level1));
		registry.register(JUNGLE_ARMORER_LEGGINGS_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.UNBREAKING), level1));
		registry.register(JUNGLE_ARMORER_CHESTPLATE_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.UNBREAKING), level1));
		registry.register(JUNGLE_ARMORER_HELMET_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.UNBREAKING), level1));
		registry.register(JUNGLE_ARMORER_BOOTS_5, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.FEATHER_FALLING), level1));
		registry.register(JUNGLE_ARMORER_HELMET_5, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION), level1));

		registry.register(SWAMP_ARMORER_BOOTS_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.MENDING), level1));
		registry.register(SWAMP_ARMORER_LEGGINGS_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.MENDING), level1));
		registry.register(SWAMP_ARMORER_CHESTPLATE_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.MENDING), level1));
		registry.register(SWAMP_ARMORER_HELMET_4, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.MENDING), level1));
		registry.register(SWAMP_ARMORER_BOOTS_5, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.DEPTH_STRIDER), level1));
		registry.register(SWAMP_ARMORER_HELMET_5, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.RESPIRATION), level1));

		registry.register(TAIGA_ARMORER_LEGGINGS_5, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.BLAST_PROTECTION), level1));
		registry.register(TAIGA_ARMORER_CHESTPLATE_5, new SingleEnchantmentProvider(enchantments.getOrThrow(Enchantments.BLAST_PROTECTION), level1));
	}

}
