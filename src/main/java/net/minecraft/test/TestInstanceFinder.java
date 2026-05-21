package net.minecraft.test;

import net.minecraft.registry.entry.RegistryEntry;

import java.util.stream.Stream;

@FunctionalInterface
/**
 * {@code TestInstanceFinder}.
 */
public interface TestInstanceFinder {

	Stream<RegistryEntry.Reference<TestInstance>> findTests();
}
