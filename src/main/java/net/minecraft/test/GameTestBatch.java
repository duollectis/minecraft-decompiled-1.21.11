package net.minecraft.test;

import net.minecraft.registry.entry.RegistryEntry;

import java.util.Collection;

/**
 * Батч тестов — группа {@link GameTestState}, объединённых общим окружением
 * {@link TestEnvironmentDefinition} и выполняемых последовательно.
 *
 * @param index       порядковый номер батча внутри группы окружения
 * @param states      состояния тестов, входящих в батч (не может быть пустым)
 * @param environment окружение, применяемое перед запуском батча
 */
public record GameTestBatch(
		int index,
		Collection<GameTestState> states,
		RegistryEntry<TestEnvironmentDefinition> environment
) {

	public GameTestBatch {
		if (states.isEmpty()) {
			throw new IllegalArgumentException("A GameTestBatch must include at least one GameTestInfo!");
		}
	}
}
