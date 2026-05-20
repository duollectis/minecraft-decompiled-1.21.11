package net.minecraft.loot.provider.number;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;

public record StorageLootNumberProvider(Identifier storage, NbtPathArgumentType.NbtPath path) implements LootNumberProvider {
   public static final MapCodec<StorageLootNumberProvider> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Identifier.CODEC.fieldOf("storage").forGetter(StorageLootNumberProvider::storage),
            NbtPathArgumentType.NbtPath.CODEC.fieldOf("path").forGetter(StorageLootNumberProvider::path)
         )
         .apply(instance, StorageLootNumberProvider::new)
   );

   @Override
   public LootNumberProviderType getType() {
      return LootNumberProviderTypes.STORAGE;
   }

   private Number getNumber(LootContext context, Number fallback) {
      NbtCompound nbtCompound = context.getWorld().getServer().getDataCommandStorage().get(this.storage);

      try {
         List<NbtElement> list = this.path.get(nbtCompound);
         if (list.size() == 1 && list.getFirst() instanceof AbstractNbtNumber abstractNbtNumber) {
            return abstractNbtNumber.numberValue();
         }
      } catch (CommandSyntaxException var7) {
      }

      return fallback;
   }

   @Override
   public float nextFloat(LootContext context) {
      return this.getNumber(context, 0.0F).floatValue();
   }

   @Override
   public int nextInt(LootContext context) {
      return this.getNumber(context, 0).intValue();
   }
}
