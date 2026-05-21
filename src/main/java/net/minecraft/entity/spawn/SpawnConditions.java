package net.minecraft.entity.spawn;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.Registry;

/**
 * {@code SpawnConditions}.
 */
public class SpawnConditions {

	/**
	 * Регистрирует and get default.
	 *
	 * @param registry registry
	 *
	 * @return MapCodec — результат операции
	 */
	public static MapCodec<? extends SpawnCondition> registerAndGetDefault(Registry<MapCodec<? extends SpawnCondition>> registry) {
		Registry.register(registry, "structure", StructureSpawnCondition.CODEC);
		Registry.register(registry, "moon_brightness", MoonBrightnessSpawnCondition.CODEC);
		return Registry.register(registry, "biome", BiomeSpawnCondition.CODEC);
	}
}
