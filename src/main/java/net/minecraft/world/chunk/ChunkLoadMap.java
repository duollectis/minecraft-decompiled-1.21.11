package net.minecraft.world.chunk;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Карта требуемых статусов загрузки чанков в радиусе вокруг точки спавна.
 * Используется при начальной загрузке мира для определения, какие чанки
 * и до какого статуса нужно загрузить.
 */
public interface ChunkLoadMap {

	void initSpawnPos(RegistryKey<World> world, ChunkPos spawnPos);

	@Nullable ChunkStatus getStatus(int x, int z);

	int getRadius();
}
