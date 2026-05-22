package net.minecraft.entity.spawn;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.Registry;

/**
 * Утилитарный класс для регистрации всех встроенных типов условий спауна.
 * Регистрирует {@link StructureSpawnCondition}, {@link MoonBrightnessSpawnCondition}
 * и {@link BiomeSpawnCondition} в реестре типов условий спауна.
 */
public final class SpawnConditions {

	private SpawnConditions() {}

	/**
	 * Регистрирует все встроенные типы условий спауна и возвращает последний зарегистрированный кодек.
	 * Вызывается при инициализации реестра {@code SPAWN_CONDITION_TYPE}.
	 *
	 * @param registry реестр кодеков условий спауна
	 * @return кодек последнего зарегистрированного типа (biome)
	 */
	public static MapCodec<? extends SpawnCondition> registerAndGetDefault(
		Registry<MapCodec<? extends SpawnCondition>> registry
	) {
		Registry.register(registry, "structure", StructureSpawnCondition.CODEC);
		Registry.register(registry, "moon_brightness", MoonBrightnessSpawnCondition.CODEC);

		return Registry.register(registry, "biome", BiomeSpawnCondition.CODEC);
	}
}
