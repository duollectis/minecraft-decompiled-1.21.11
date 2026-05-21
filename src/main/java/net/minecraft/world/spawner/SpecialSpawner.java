package net.minecraft.world.spawner;

import net.minecraft.server.world.ServerWorld;

/**
 * {@code SpecialSpawner}.
 */
public interface SpecialSpawner {

	void spawn(ServerWorld world, boolean spawnMonsters);
}
