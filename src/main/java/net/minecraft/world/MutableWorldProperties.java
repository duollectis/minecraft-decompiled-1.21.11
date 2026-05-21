package net.minecraft.world;

/**
 * {@code MutableWorldProperties}.
 */
public interface MutableWorldProperties extends WorldProperties {

	void setSpawnPoint(WorldProperties.SpawnPoint spawnPoint);
}
