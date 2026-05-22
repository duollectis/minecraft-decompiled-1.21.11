package net.minecraft.data.loottable.vanilla;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.data.loottable.LootTableGenerator;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.trim.*;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootTableEntry;
import net.minecraft.loot.function.SetComponentsLootFunction;
import net.minecraft.loot.function.SetEnchantmentsLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

import java.util.function.BiConsumer;

/**
 * {@code VanillaEquipmentLootTableGenerator}.
 */
public record VanillaEquipmentLootTableGenerator(RegistryWrapper.WrapperLookup registries) implements LootTableGenerator {

	@Override
	public void accept(BiConsumer<RegistryKey<LootTable>, LootTable.Builder> lootTableBiConsumer) {
		RegistryWrapper.Impl<ArmorTrimPattern> trimPatternLookup = registries.getOrThrow(RegistryKeys.TRIM_PATTERN);
		RegistryWrapper.Impl<ArmorTrimMaterial> trimMaterialLookup = registries.getOrThrow(RegistryKeys.TRIM_MATERIAL);
		RegistryWrapper.Impl<Enchantment> enchantmentLookup = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		ArmorTrim
				armorTrim =
				new ArmorTrim(trimMaterialLookup.getOrThrow(ArmorTrimMaterials.COPPER), trimPatternLookup.getOrThrow(ArmorTrimPatterns.FLOW));
		ArmorTrim
				armorTrim2 =
				new ArmorTrim(trimMaterialLookup.getOrThrow(ArmorTrimMaterials.COPPER), trimPatternLookup.getOrThrow(ArmorTrimPatterns.BOLT));
		lootTableBiConsumer.accept(
				LootTables.TRIAL_CHAMBER_EQUIPMENT,
				LootTable.builder()
				         .pool(
						         LootPool.builder()
						                 .rolls(ConstantLootNumberProvider.create(1.0F))
						                 .with(
								                 LootTableEntry
										                 .builder(createEquipmentTableBuilder(
												                 Items.CHAINMAIL_HELMET,
												                 Items.CHAINMAIL_CHESTPLATE,
												                 armorTrim2,
												                 enchantmentLookup
										                 ).build())
										                 .weight(4)
						                 )
						                 .with(LootTableEntry
								                 .builder(createEquipmentTableBuilder(
										                 Items.IRON_HELMET,
										                 Items.IRON_CHESTPLATE,
										                 armorTrim,
										                 enchantmentLookup
								                 ).build())
								                 .weight(2))
						                 .with(LootTableEntry
								                 .builder(createEquipmentTableBuilder(
										                 Items.DIAMOND_HELMET,
										                 Items.DIAMOND_CHESTPLATE,
										                 armorTrim,
										                 enchantmentLookup
								                 ).build())
								                 .weight(1))
				         )
		);
		lootTableBiConsumer.accept(
				LootTables.TRIAL_CHAMBER_MELEE_EQUIPMENT,
				LootTable.builder()
				         .pool(LootPool
						         .builder()
						         .rolls(ConstantLootNumberProvider.create(1.0F))
						         .with(LootTableEntry.builder(LootTables.TRIAL_CHAMBER_EQUIPMENT)))
				         .pool(
						         LootPool.builder()
						                 .rolls(ConstantLootNumberProvider.create(1.0F))
						                 .with(ItemEntry.builder(Items.IRON_SWORD).weight(4))
						                 .with(
								                 ItemEntry.builder(Items.IRON_SWORD)
								                          .apply(
										                          new SetEnchantmentsLootFunction.Builder()
												                          .enchantment(
														                          enchantmentLookup.getOrThrow(Enchantments.SHARPNESS),
														                          ConstantLootNumberProvider.create(1.0F)
												                          )
								                          )
						                 )
						                 .with(
								                 ItemEntry.builder(Items.IRON_SWORD)
								                          .apply(
										                          new SetEnchantmentsLootFunction.Builder()
												                          .enchantment(
														                          enchantmentLookup.getOrThrow(Enchantments.KNOCKBACK),
														                          ConstantLootNumberProvider.create(1.0F)
												                          )
								                          )
						                 )
						                 .with(ItemEntry.builder(Items.DIAMOND_SWORD))
				         )
		);
		lootTableBiConsumer.accept(
				LootTables.TRIAL_CHAMBER_RANGED_EQUIPMENT,
				LootTable.builder()
				         .pool(LootPool
						         .builder()
						         .rolls(ConstantLootNumberProvider.create(1.0F))
						         .with(LootTableEntry.builder(LootTables.TRIAL_CHAMBER_EQUIPMENT)))
				         .pool(
						         LootPool.builder()
						                 .rolls(ConstantLootNumberProvider.create(1.0F))
						                 .with(ItemEntry.builder(Items.BOW).weight(2))
						                 .with(
								                 ItemEntry.builder(Items.BOW)
								                          .apply(
										                          new SetEnchantmentsLootFunction.Builder().enchantment(
												                          enchantmentLookup.getOrThrow(Enchantments.POWER),
												                          ConstantLootNumberProvider.create(1.0F)
										                          )
								                          )
						                 )
						                 .with(
								                 ItemEntry.builder(Items.BOW)
								                          .apply(
										                          new SetEnchantmentsLootFunction.Builder().enchantment(
												                          enchantmentLookup.getOrThrow(Enchantments.PUNCH),
												                          ConstantLootNumberProvider.create(1.0F)
										                          )
								                          )
						                 )
				         )
		);
	}

	public static LootTable.Builder createEquipmentTableBuilder(
			Item helmet, Item chestplate, ArmorTrim trim, RegistryWrapper.Impl<Enchantment> enchantmentRegistryWrapper
	) {
		return LootTable.builder()
		                .pool(
				                LootPool.builder()
				                        .rolls(ConstantLootNumberProvider.create(1.0F))
				                        .conditionally(RandomChanceLootCondition.builder(0.5F))
				                        .with(
						                        ItemEntry.builder(helmet)
						                                 .apply(SetComponentsLootFunction.builder(
								                                 DataComponentTypes.TRIM,
								                                 trim
						                                 ))
						                                 .apply(
								                                 new SetEnchantmentsLootFunction.Builder()
										                                 .enchantment(
												                                 enchantmentRegistryWrapper.getOrThrow(
														                                 Enchantments.PROTECTION),
												                                 ConstantLootNumberProvider.create(4.0F)
										                                 )
										                                 .enchantment(
												                                 enchantmentRegistryWrapper.getOrThrow(
														                                 Enchantments.PROJECTILE_PROTECTION),
												                                 ConstantLootNumberProvider.create(4.0F)
										                                 )
										                                 .enchantment(
												                                 enchantmentRegistryWrapper.getOrThrow(
														                                 Enchantments.FIRE_PROTECTION),
												                                 ConstantLootNumberProvider.create(4.0F)
										                                 )
						                                 )
				                        )
		                )
		                .pool(
				                LootPool.builder()
				                        .rolls(ConstantLootNumberProvider.create(1.0F))
				                        .conditionally(RandomChanceLootCondition.builder(0.5F))
				                        .with(
						                        ItemEntry.builder(chestplate)
						                                 .apply(SetComponentsLootFunction.builder(
								                                 DataComponentTypes.TRIM,
								                                 trim
						                                 ))
						                                 .apply(
								                                 new SetEnchantmentsLootFunction.Builder()
										                                 .enchantment(
												                                 enchantmentRegistryWrapper.getOrThrow(
														                                 Enchantments.PROTECTION),
												                                 ConstantLootNumberProvider.create(4.0F)
										                                 )
										                                 .enchantment(
												                                 enchantmentRegistryWrapper.getOrThrow(
														                                 Enchantments.PROJECTILE_PROTECTION),
												                                 ConstantLootNumberProvider.create(4.0F)
										                                 )
										                                 .enchantment(
												                                 enchantmentRegistryWrapper.getOrThrow(
														                                 Enchantments.FIRE_PROTECTION),
												                                 ConstantLootNumberProvider.create(4.0F)
										                                 )
						                                 )
				                        )
		                );
	}
}
