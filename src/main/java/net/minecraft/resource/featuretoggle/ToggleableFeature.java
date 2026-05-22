package net.minecraft.resource.featuretoggle;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.Set;

/**
 * Маркерный интерфейс для игровых объектов, которые могут быть включены или отключены
 * через систему флагов функций ({@link FeatureFlag}).
 *
 * <p>Реализующий класс должен объявить, какие флаги необходимы для его активации,
 * через метод {@link #getRequiredFeatures()}. Объект считается активным, если все
 * его обязательные флаги присутствуют в переданном наборе {@link FeatureSet}.</p>
 *
 * <p>{@link #FEATURE_ENABLED_REGISTRY_KEYS} содержит список реестров, объекты которых
 * участвуют в проверке включённости функций при загрузке мира.</p>
 */
public interface ToggleableFeature {

	/** Реестры, объекты которых проверяются на включённость при загрузке мира. */
	Set<RegistryKey<? extends Registry<? extends ToggleableFeature>>> FEATURE_ENABLED_REGISTRY_KEYS = Set.of(
			RegistryKeys.ITEM,
			RegistryKeys.BLOCK,
			RegistryKeys.ENTITY_TYPE,
			RegistryKeys.GAME_RULE,
			RegistryKeys.SCREEN_HANDLER,
			RegistryKeys.POTION,
			RegistryKeys.STATUS_EFFECT
	);

	/** @return набор флагов, необходимых для активации этого объекта */
	FeatureSet getRequiredFeatures();

	/**
	 * Проверяет, активен ли объект при заданном наборе включённых флагов.
	 *
	 * @param enabledFeatures набор активных флагов функций
	 * @return {@code true}, если все обязательные флаги присутствуют в {@code enabledFeatures}
	 */
	default boolean isEnabled(FeatureSet enabledFeatures) {
		return getRequiredFeatures().isSubsetOf(enabledFeatures);
	}
}
