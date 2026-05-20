package net.minecraft.structure.rule.blockentity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

public class AppendLootRuleBlockEntityModifier implements RuleBlockEntityModifier {
   public static final MapCodec<AppendLootRuleBlockEntityModifier> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(LootTable.TABLE_KEY.fieldOf("loot_table").forGetter(modifier -> modifier.lootTable))
         .apply(instance, AppendLootRuleBlockEntityModifier::new)
   );
   private final RegistryKey<LootTable> lootTable;

   public AppendLootRuleBlockEntityModifier(RegistryKey<LootTable> lootTable) {
      this.lootTable = lootTable;
   }

   @Override
   public NbtCompound modifyBlockEntityNbt(Random random, @Nullable NbtCompound nbt) {
      NbtCompound nbtCompound = nbt == null ? new NbtCompound() : nbt.copy();
      nbtCompound.put("LootTable", LootTable.TABLE_KEY, this.lootTable);
      nbtCompound.putLong("LootTableSeed", random.nextLong());
      return nbtCompound;
   }

   @Override
   public RuleBlockEntityModifierType<?> getType() {
      return RuleBlockEntityModifierType.APPEND_LOOT;
   }
}
