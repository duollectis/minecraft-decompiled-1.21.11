package net.minecraft.test;

import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/**
 * Реестр встроенных экземпляров тестов.
 * {@link #ALWAYS_PASS} — базовый тест, который всегда проходит успешно.
 */
public interface TestInstances {

	RegistryKey<TestInstance> ALWAYS_PASS = of("always_pass");

	static void bootstrap(Registerable<TestInstance> registry) {
		RegistryEntryLookup<Consumer<TestContext>> functionLookup =
			registry.getRegistryLookup(RegistryKeys.TEST_FUNCTION);
		RegistryEntryLookup<TestEnvironmentDefinition> environmentLookup =
			registry.getRegistryLookup(RegistryKeys.TEST_ENVIRONMENT);

		registry.register(
			ALWAYS_PASS,
			new FunctionTestInstance(
				BuiltinTestFunctions.ALWAYS_PASS,
				new TestData<>(
					environmentLookup.getOrThrow(TestEnvironments.DEFAULT),
					Identifier.ofVanilla("empty"),
					1,
					1,
					false
				)
			)
		);
	}

	private static RegistryKey<TestInstance> of(String id) {
		return RegistryKey.of(RegistryKeys.TEST_INSTANCE, Identifier.ofVanilla(id));
	}
}
