package net.minecraft.world.level.storage;

import net.minecraft.world.SaveProperties;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;

/**
 * Результат разбора level.dat: свойства сохранения и конфигурация измерений.
 * Используется как промежуточный контейнер при загрузке мира до финальной
 * инициализации серверных реестров.
 */
public record ParsedSaveProperties(
	SaveProperties properties,
	DimensionOptionsRegistryHolder.DimensionsConfig dimensions
) {
}
