package net.minecraft.entity.spawn;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.attribute.EnvironmentAttributeAccess;
import net.minecraft.world.biome.Biome;

/**
 * {@code SpawnContext}.
 */
public record SpawnContext(
		BlockPos pos,
		ServerWorldAccess world,
		EnvironmentAttributeAccess environmentAttributes,
		RegistryEntry<Biome> biome
) {

	/**
	 * Of.
	 *
	 * @param world world
	 * @param pos pos
	 *
	 * @return SpawnContext — результат операции
	 */
	public static SpawnContext of(ServerWorldAccess world, BlockPos pos) {
		RegistryEntry<Biome> registryEntry = world.getBiome(pos);
		return new SpawnContext(pos, world, world.getEnvironmentAttributes(), registryEntry);
	}
}
