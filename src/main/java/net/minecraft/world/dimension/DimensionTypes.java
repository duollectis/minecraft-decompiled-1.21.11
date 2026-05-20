package net.minecraft.world.dimension;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class DimensionTypes {
   public static final RegistryKey<DimensionType> OVERWORLD = of("overworld");
   public static final RegistryKey<DimensionType> THE_NETHER = of("the_nether");
   public static final RegistryKey<DimensionType> THE_END = of("the_end");
   public static final RegistryKey<DimensionType> OVERWORLD_CAVES = of("overworld_caves");

   private static RegistryKey<DimensionType> of(String id) {
      return RegistryKey.of(RegistryKeys.DIMENSION_TYPE, Identifier.ofVanilla(id));
   }
}
