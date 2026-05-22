package net.minecraft.world.storage;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

/**
 * Составной ключ хранилища, однозначно идентифицирующий файл данных чанков.
 * Комбинирует имя уровня, измерение и тип данных (например, "poi", "entities").
 */
public record StorageKey(String level, RegistryKey<World> dimension, String type) {

	public StorageKey withSuffix(String suffix) {
		return new StorageKey(level, dimension, type + suffix);
	}
}
