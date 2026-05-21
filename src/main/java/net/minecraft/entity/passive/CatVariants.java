package net.minecraft.entity.passive;

import net.minecraft.entity.VariantSelectorProvider;
import net.minecraft.entity.spawn.MoonBrightnessSpawnCondition;
import net.minecraft.entity.spawn.SpawnConditionSelectors;
import net.minecraft.entity.spawn.StructureSpawnCondition;
import net.minecraft.predicate.NumberRange;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

import java.util.List;

/**
 * {@code CatVariants}.
 */
public interface CatVariants {

	RegistryKey<CatVariant> TABBY = of("tabby");

	RegistryKey<CatVariant> BLACK = of("black");

	RegistryKey<CatVariant> RED = of("red");

	RegistryKey<CatVariant> SIAMESE = of("siamese");

	RegistryKey<CatVariant> BRITISH_SHORTHAIR = of("british_shorthair");

	RegistryKey<CatVariant> CALICO = of("calico");

	RegistryKey<CatVariant> PERSIAN = of("persian");

	RegistryKey<CatVariant> RAGDOLL = of("ragdoll");

	RegistryKey<CatVariant> WHITE = of("white");

	RegistryKey<CatVariant> JELLIE = of("jellie");

	RegistryKey<CatVariant> ALL_BLACK = of("all_black");

	private static RegistryKey<CatVariant> of(String id) {
		return RegistryKey.of(RegistryKeys.CAT_VARIANT, Identifier.ofVanilla(id));
	}

	static void bootstrap(Registerable<CatVariant> registry) {
		RegistryEntryLookup<Structure> registryEntryLookup = registry.getRegistryLookup(RegistryKeys.STRUCTURE);
		register(registry, TABBY, "entity/cat/tabby");
		register(registry, BLACK, "entity/cat/black");
		register(registry, RED, "entity/cat/red");
		register(registry, SIAMESE, "entity/cat/siamese");
		register(registry, BRITISH_SHORTHAIR, "entity/cat/british_shorthair");
		register(registry, CALICO, "entity/cat/calico");
		register(registry, PERSIAN, "entity/cat/persian");
		register(registry, RAGDOLL, "entity/cat/ragdoll");
		register(registry, WHITE, "entity/cat/white");
		register(registry, JELLIE, "entity/cat/jellie");
		register(
				registry,
				ALL_BLACK,
				"entity/cat/all_black",
				new SpawnConditionSelectors(
						List.of(
								new VariantSelectorProvider.Selector<>(
										new StructureSpawnCondition(registryEntryLookup.getOrThrow(StructureTags.CATS_SPAWN_AS_BLACK)),
										1
								),
								new VariantSelectorProvider.Selector<>(
										new MoonBrightnessSpawnCondition(NumberRange.DoubleRange.atLeast(0.9)), 0)
						)
				)
		);
	}

	private static void register(Registerable<CatVariant> registry, RegistryKey<CatVariant> key, String assetId) {
		register(registry, key, assetId, SpawnConditionSelectors.createFallback(0));
	}

	private static void register(
			Registerable<CatVariant> registry,
			RegistryKey<CatVariant> key,
			String assetId,
			SpawnConditionSelectors spawnConditions
	) {
		registry.register(
				key,
				new CatVariant(new AssetInfo.TextureAssetInfo(Identifier.ofVanilla(assetId)), spawnConditions)
		);
	}
}
