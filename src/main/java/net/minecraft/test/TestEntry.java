package net.minecraft.test;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public record TestEntry(
   Map<Identifier, TestData<RegistryKey<TestEnvironmentDefinition>>> tests, RegistryKey<Consumer<TestContext>> functionKey, Consumer<TestContext> function
) {
   public TestEntry(Map<Identifier, TestData<RegistryKey<TestEnvironmentDefinition>>> tests, Identifier functionId, Consumer<TestContext> function) {
      this(tests, RegistryKey.of(RegistryKeys.TEST_FUNCTION, functionId), function);
   }

   public TestEntry(Identifier id, TestData<RegistryKey<TestEnvironmentDefinition>> data, Consumer<TestContext> function) {
      this(Map.of(id, data), id, function);
   }
}
