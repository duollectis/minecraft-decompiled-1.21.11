package net.minecraft.resource.featuretoggle;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.Set;

/**
 * {@code ToggleableFeature}.
 */
public interface ToggleableFeature {

	Set<RegistryKey<? extends Registry<? extends ToggleableFeature>>> FEATURE_ENABLED_REGISTRY_KEYS = Set.of(
			RegistryKeys.ITEM,
			RegistryKeys.BLOCK,
			RegistryKeys.ENTITY_TYPE,
			RegistryKeys.GAME_RULE,
			RegistryKeys.SCREEN_HANDLER,
			RegistryKeys.POTION,
			RegistryKeys.STATUS_EFFECT
	);

	FeatureSet getRequiredFeatures();

	default boolean isEnabled(FeatureSet enabledFeatures) {
		return this.getRequiredFeatures().isSubsetOf(enabledFeatures);
	}
}
