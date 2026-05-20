package net.minecraft.block.vault;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.block.spawner.EntityDetector;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.registry.RegistryKey;

public record VaultConfig(
   RegistryKey<LootTable> lootTable,
   double activationRange,
   double deactivationRange,
   ItemStack keyItem,
   Optional<RegistryKey<LootTable>> overrideLootTableToDisplay,
   EntityDetector playerDetector,
   EntityDetector.Selector entitySelector
) {
   public static final String CONFIG_KEY = "config";
   public static VaultConfig DEFAULT = new VaultConfig();
   public static Codec<VaultConfig> codec = RecordCodecBuilder.<VaultConfig>create(
         instance -> instance.group(
               LootTable.TABLE_KEY.lenientOptionalFieldOf("loot_table", DEFAULT.lootTable()).forGetter(VaultConfig::lootTable),
               Codec.DOUBLE.lenientOptionalFieldOf("activation_range", DEFAULT.activationRange()).forGetter(VaultConfig::activationRange),
               Codec.DOUBLE.lenientOptionalFieldOf("deactivation_range", DEFAULT.deactivationRange()).forGetter(VaultConfig::deactivationRange),
               ItemStack.createOptionalCodec("key_item").forGetter(VaultConfig::keyItem),
               LootTable.TABLE_KEY.lenientOptionalFieldOf("override_loot_table_to_display").forGetter(VaultConfig::overrideLootTableToDisplay)
            )
            .apply(instance, VaultConfig::new)
      )
      .validate(VaultConfig::validate);

   private VaultConfig() {
      this(
         LootTables.TRIAL_CHAMBERS_REWARD_CHEST,
         4.0,
         4.5,
         new ItemStack(Items.TRIAL_KEY),
         Optional.empty(),
         EntityDetector.NON_SPECTATOR_PLAYERS,
         EntityDetector.Selector.IN_WORLD
      );
   }

   public VaultConfig(
      RegistryKey<LootTable> lootTable,
      double activationRange,
      double deactivationRange,
      ItemStack keyItem,
      Optional<RegistryKey<LootTable>> overrideLootTableToDisplay
   ) {
      this(lootTable, activationRange, deactivationRange, keyItem, overrideLootTableToDisplay, DEFAULT.playerDetector(), DEFAULT.entitySelector());
   }

   public EntityDetector playerDetector() {
      return SharedConstants.VAULT_DETECTS_SHEEP_AS_PLAYERS ? EntityDetector.SHEEP : this.playerDetector;
   }

   private static DataResult<VaultConfig> validate(VaultConfig config) {
      return config.activationRange > config.deactivationRange
         ? DataResult.error(
            () -> "Activation range must (" + config.activationRange + ") be less or equal to deactivation range (" + config.deactivationRange + ")"
         )
         : DataResult.success(config);
   }
}
