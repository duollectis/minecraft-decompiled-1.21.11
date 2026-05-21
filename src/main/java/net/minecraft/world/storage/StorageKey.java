package net.minecraft.world.storage;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

/**
 * {@code StorageKey}.
 */
public record StorageKey(String level, RegistryKey<World> dimension, String type) {

	/**
	 * With suffix.
	 *
	 * @param suffix suffix
	 *
	 * @return StorageKey — результат операции
	 */
	public StorageKey withSuffix(String suffix) {
		return new StorageKey(this.level, this.dimension, this.type + suffix);
	}
}
