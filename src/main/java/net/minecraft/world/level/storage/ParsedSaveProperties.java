package net.minecraft.world.level.storage;

import net.minecraft.world.SaveProperties;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;

/**
 * {@code ParsedSaveProperties}.
 */
public record ParsedSaveProperties(
		SaveProperties properties,
		DimensionOptionsRegistryHolder.DimensionsConfig dimensions
) {
}
