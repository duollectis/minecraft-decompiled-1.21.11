package net.minecraft.world.gen.chunk;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.Registry;

/**
 * Регистрирует все встроенные типы генераторов чанков в реестр кодеков.
 */
public class ChunkGenerators {

	/**
	 * Регистрирует генераторы {@code noise}, {@code flat} и {@code debug},
	 * возвращая кодек последнего как значение по умолчанию.
	 */
	public static MapCodec<? extends ChunkGenerator> registerAndGetDefault(
			Registry<MapCodec<? extends ChunkGenerator>> registry
	) {
		Registry.register(registry, "noise", NoiseChunkGenerator.CODEC);
		Registry.register(registry, "flat", FlatChunkGenerator.CODEC);
		return Registry.register(registry, "debug", DebugChunkGenerator.CODEC);
	}
}
