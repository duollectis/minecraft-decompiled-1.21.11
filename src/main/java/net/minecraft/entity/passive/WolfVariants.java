package net.minecraft.entity.passive;

import net.minecraft.entity.spawn.BiomeSpawnCondition;
import net.minecraft.entity.spawn.SpawnConditionSelectors;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

public class WolfVariants {
   public static final RegistryKey<WolfVariant> PALE = of("pale");
   public static final RegistryKey<WolfVariant> SPOTTED = of("spotted");
   public static final RegistryKey<WolfVariant> SNOWY = of("snowy");
   public static final RegistryKey<WolfVariant> BLACK = of("black");
   public static final RegistryKey<WolfVariant> ASHEN = of("ashen");
   public static final RegistryKey<WolfVariant> RUSTY = of("rusty");
   public static final RegistryKey<WolfVariant> WOODS = of("woods");
   public static final RegistryKey<WolfVariant> CHESTNUT = of("chestnut");
   public static final RegistryKey<WolfVariant> STRIPED = of("striped");
   public static final RegistryKey<WolfVariant> DEFAULT = PALE;

   private static RegistryKey<WolfVariant> of(String id) {
      return RegistryKey.of(RegistryKeys.WOLF_VARIANT, Identifier.ofVanilla(id));
   }

   private static void register(Registerable<WolfVariant> registry, RegistryKey<WolfVariant> key, String textureName, RegistryKey<Biome> biome) {
      register(registry, key, textureName, createSpawnConditions(RegistryEntryList.of(registry.getRegistryLookup(RegistryKeys.BIOME).getOrThrow(biome))));
   }

   private static void register(Registerable<WolfVariant> registry, RegistryKey<WolfVariant> key, String textureName, TagKey<Biome> biomeTag) {
      register(registry, key, textureName, createSpawnConditions(registry.getRegistryLookup(RegistryKeys.BIOME).getOrThrow(biomeTag)));
   }

   private static SpawnConditionSelectors createSpawnConditions(RegistryEntryList<Biome> requiredBiomes) {
      return SpawnConditionSelectors.createSingle(new BiomeSpawnCondition(requiredBiomes), 1);
   }

   private static void register(Registerable<WolfVariant> registry, RegistryKey<WolfVariant> key, String textureName, SpawnConditionSelectors spawnConditions) {
      Identifier identifier = Identifier.ofVanilla("entity/wolf/" + textureName);
      Identifier identifier2 = Identifier.ofVanilla("entity/wolf/" + textureName + "_tame");
      Identifier identifier3 = Identifier.ofVanilla("entity/wolf/" + textureName + "_angry");
      registry.register(
         key,
         new WolfVariant(
            new WolfVariant.WolfAssetInfo(
               new AssetInfo.TextureAssetInfo(identifier), new AssetInfo.TextureAssetInfo(identifier2), new AssetInfo.TextureAssetInfo(identifier3)
            ),
            spawnConditions
         )
      );
   }

   public static void bootstrap(Registerable<WolfVariant> registry) {
      register(registry, PALE, "wolf", SpawnConditionSelectors.createFallback(0));
      register(registry, SPOTTED, "wolf_spotted", BiomeTags.IS_SAVANNA);
      register(registry, SNOWY, "wolf_snowy", BiomeKeys.GROVE);
      register(registry, BLACK, "wolf_black", BiomeKeys.OLD_GROWTH_PINE_TAIGA);
      register(registry, ASHEN, "wolf_ashen", BiomeKeys.SNOWY_TAIGA);
      register(registry, RUSTY, "wolf_rusty", BiomeTags.IS_JUNGLE);
      register(registry, WOODS, "wolf_woods", BiomeKeys.FOREST);
      register(registry, CHESTNUT, "wolf_chestnut", BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA);
      register(registry, STRIPED, "wolf_striped", BiomeTags.IS_BADLANDS);
   }
}
