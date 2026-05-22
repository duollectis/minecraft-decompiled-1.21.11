package net.minecraft.test;

import net.minecraft.registry.entry.RegistryEntry;

import java.util.stream.Stream;

/**
 * Функциональный интерфейс для поиска экземпляров тестов в реестре.
 */
@FunctionalInterface
public interface TestInstanceFinder {

	Stream<RegistryEntry.Reference<TestInstance>> findTests();
}
