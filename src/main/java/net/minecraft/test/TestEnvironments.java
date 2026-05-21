package net.minecraft.test;

import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * {@code TestEnvironments}.
 */
public interface TestEnvironments {

	String DEFAULT_ID = "default";

	RegistryKey<TestEnvironmentDefinition> DEFAULT = of("default");

	private static RegistryKey<TestEnvironmentDefinition> of(String id) {
		return RegistryKey.of(RegistryKeys.TEST_ENVIRONMENT, Identifier.ofVanilla(id));
	}

	static void bootstrap(Registerable<TestEnvironmentDefinition> registry) {
		registry.register(DEFAULT, new TestEnvironmentDefinition.AllOf(List.of()));
	}
}
