package net.minecraft.data.loottable.vanilla;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.data.loottable.LootTableGenerator;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.EntityPropertiesLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.AlternativeEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.entry.LootTableEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.EntityTypePredicate;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

public record VanillaChargedCreeperLootTableGenerator(RegistryWrapper.WrapperLookup registries) implements LootTableGenerator {
   private static final List<VanillaChargedCreeperLootTableGenerator.Table> TABLES = List.of(
      new VanillaChargedCreeperLootTableGenerator.Table(LootTables.PIGLIN_CHARGED_CREEPER, EntityType.PIGLIN, Items.PIGLIN_HEAD),
      new VanillaChargedCreeperLootTableGenerator.Table(LootTables.CREEPER_CHARGED_CREEPER, EntityType.CREEPER, Items.CREEPER_HEAD),
      new VanillaChargedCreeperLootTableGenerator.Table(LootTables.SKELETON_CHARGED_CREEPER, EntityType.SKELETON, Items.SKELETON_SKULL),
      new VanillaChargedCreeperLootTableGenerator.Table(LootTables.WITHER_SKELETON_CHARGED_CREEPER, EntityType.WITHER_SKELETON, Items.WITHER_SKELETON_SKULL),
      new VanillaChargedCreeperLootTableGenerator.Table(LootTables.ZOMBIE_CHARGED_CREEPER, EntityType.ZOMBIE, Items.ZOMBIE_HEAD)
   );

   @Override
   public void accept(BiConsumer<RegistryKey<LootTable>, LootTable.Builder> lootTableBiConsumer) {
      RegistryEntryLookup<EntityType<?>> registryEntryLookup = this.registries.getOrThrow(RegistryKeys.ENTITY_TYPE);
      List<LootPoolEntry.Builder<?>> list = new ArrayList<>(TABLES.size());

      for (VanillaChargedCreeperLootTableGenerator.Table table : TABLES) {
         lootTableBiConsumer.accept(
            table.lootTable, LootTable.builder().pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1.0F)).with(ItemEntry.builder(table.item)))
         );
         LootCondition.Builder builder = EntityPropertiesLootCondition.builder(
            LootContext.EntityReference.THIS, EntityPredicate.Builder.create().type(EntityTypePredicate.create(registryEntryLookup, table.entityType))
         );
         list.add(LootTableEntry.builder(table.lootTable).conditionally(builder));
      }

      lootTableBiConsumer.accept(
         LootTables.ROOT_CHARGED_CREEPER,
         LootTable.builder()
            .pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1.0F)).with(AlternativeEntry.builder(list.toArray(LootPoolEntry.Builder[]::new))))
      );
   }

   record Table(RegistryKey<LootTable> lootTable, EntityType<?> entityType, Item item) {
   }
}
