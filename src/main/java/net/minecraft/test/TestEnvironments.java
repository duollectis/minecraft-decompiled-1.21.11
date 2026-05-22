package net.minecraft.test;

import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Реестр встроенных окружений для тестов.
 * {@link #DEFAULT} — пустое окружение без каких-либо изменений мира.
 */
public interface TestEnvironments {

	String DEFAULT_ID = "default";

	RegistryKey<TestEnvironmentDefinition> DEFAULT = of(DEFAULT_ID);

	static void bootstrap(Registerable<TestEnvironmentDefinition> registry) {
		registry.register(DEFAULT, new TestEnvironmentDefinition.AllOf(List.of()));
	}

	private static RegistryKey<TestEnvironmentDefinition> of(String id) {
		return RegistryKey.of(RegistryKeys.TEST_ENVIRONMENT, Identifier.ofVanilla(id));
	}
}
