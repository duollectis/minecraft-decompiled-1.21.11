package net.minecraft.entity.spawn;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;

public record BiomeSpawnCondition(RegistryEntryList<Biome> requiredBiomes) implements SpawnCondition {
   public static final MapCodec<BiomeSpawnCondition> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(RegistryCodecs.entryList(RegistryKeys.BIOME).fieldOf("biomes").forGetter(BiomeSpawnCondition::requiredBiomes))
         .apply(instance, BiomeSpawnCondition::new)
   );

   public boolean test(SpawnContext spawnContext) {
      return this.requiredBiomes.contains(spawnContext.biome());
   }

   @Override
   public MapCodec<BiomeSpawnCondition> getCodec() {
      return CODEC;
   }
}
