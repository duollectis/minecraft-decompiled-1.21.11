package net.minecraft.world.chunk;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * {@code ChunkLoadMap}.
 */
public interface ChunkLoadMap {

	void initSpawnPos(RegistryKey<World> world, ChunkPos spawnPos);

	@Nullable ChunkStatus getStatus(int x, int z);

	int getRadius();
}
