package net.minecraft.test;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Запись, связывающая набор тестовых данных с конкретной функцией-потребителем {@link TestContext}.
 * Используется при регистрации тестов через {@link TestFunctionProvider}.
 */
public record TestEntry(
	Map<Identifier, TestData<RegistryKey<TestEnvironmentDefinition>>> tests,
	RegistryKey<Consumer<TestContext>> functionKey,
	Consumer<TestContext> function
) {

	public TestEntry(
		Map<Identifier, TestData<RegistryKey<TestEnvironmentDefinition>>> tests,
		Identifier functionId,
		Consumer<TestContext> function
	) {
		this(tests, RegistryKey.of(RegistryKeys.TEST_FUNCTION, functionId), function);
	}

	public TestEntry(
		Identifier id,
		TestData<RegistryKey<TestEnvironmentDefinition>> data,
		Consumer<TestContext> function
	) {
		this(Map.of(id, data), id, function);
	}
}
