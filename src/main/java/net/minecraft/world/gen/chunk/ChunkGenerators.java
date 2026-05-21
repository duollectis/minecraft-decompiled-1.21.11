package net.minecraft.world.gen.chunk;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.Registry;

/**
 * {@code ChunkGenerators}.
 */
public class ChunkGenerators {

	/**
	 * Регистрирует and get default.
	 *
	 * @param registry registry
	 *
	 * @return MapCodec — результат операции
	 */
	public static MapCodec<? extends ChunkGenerator> registerAndGetDefault(Registry<MapCodec<? extends ChunkGenerator>> registry) {
		Registry.register(registry, "noise", NoiseChunkGenerator.CODEC);
		Registry.register(registry, "flat", FlatChunkGenerator.CODEC);
		return Registry.register(registry, "debug", DebugChunkGenerator.CODEC);
	}
}
