package net.minecraft.test;

import java.util.stream.Stream;
import net.minecraft.registry.entry.RegistryEntry;

@FunctionalInterface
public interface TestInstanceFinder {
   Stream<RegistryEntry.Reference<TestInstance>> findTests();
}
