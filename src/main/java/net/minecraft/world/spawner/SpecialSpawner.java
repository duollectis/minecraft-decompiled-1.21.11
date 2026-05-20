package net.minecraft.world.spawner;

import net.minecraft.server.world.ServerWorld;

public interface SpecialSpawner {
   void spawn(ServerWorld world, boolean spawnMonsters);
}
