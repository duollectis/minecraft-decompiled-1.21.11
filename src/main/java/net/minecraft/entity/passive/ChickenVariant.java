package net.minecraft.entity.passive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.entity.VariantSelectorProvider;
import net.minecraft.entity.spawn.SpawnCondition;
import net.minecraft.entity.spawn.SpawnConditionSelectors;
import net.minecraft.entity.spawn.SpawnContext;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.util.ModelAndTexture;
import net.minecraft.util.StringIdentifiable;

public record ChickenVariant(ModelAndTexture<ChickenVariant.Model> modelAndTexture, SpawnConditionSelectors spawnConditions)
   implements VariantSelectorProvider<SpawnContext, SpawnCondition> {
   public static final Codec<ChickenVariant> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            ModelAndTexture.createMapCodec(ChickenVariant.Model.CODEC, ChickenVariant.Model.NORMAL).forGetter(ChickenVariant::modelAndTexture),
            SpawnConditionSelectors.CODEC.fieldOf("spawn_conditions").forGetter(ChickenVariant::spawnConditions)
         )
         .apply(instance, ChickenVariant::new)
   );
   public static final Codec<ChickenVariant> NETWORK_CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            ModelAndTexture.createMapCodec(ChickenVariant.Model.CODEC, ChickenVariant.Model.NORMAL).forGetter(ChickenVariant::modelAndTexture)
         )
         .apply(instance, ChickenVariant::new)
   );
   public static final Codec<RegistryEntry<ChickenVariant>> ENTRY_CODEC = RegistryFixedCodec.of(RegistryKeys.CHICKEN_VARIANT);
   public static final PacketCodec<RegistryByteBuf, RegistryEntry<ChickenVariant>> ENTRY_PACKET_CODEC = PacketCodecs.registryEntry(RegistryKeys.CHICKEN_VARIANT);

   private ChickenVariant(ModelAndTexture<ChickenVariant.Model> modelAndTexture) {
      this(modelAndTexture, SpawnConditionSelectors.EMPTY);
   }

   @Override
   public List<VariantSelectorProvider.Selector<SpawnContext, SpawnCondition>> getSelectors() {
      return this.spawnConditions.selectors();
   }

   public static enum Model implements StringIdentifiable {
      NORMAL("normal"),
      COLD("cold");

      public static final Codec<ChickenVariant.Model> CODEC = StringIdentifiable.createCodec(ChickenVariant.Model::values);
      private final String id;

      private Model(final String id) {
         this.id = id;
      }

      @Override
      public String asString() {
         return this.id;
      }
   }
}
