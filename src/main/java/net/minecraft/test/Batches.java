package net.minecraft.test;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockRotation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Утилитарный класс для разбивки тестовых состояний на батчи ({@link GameTestBatch}).
 * <p>
 * Группирует тесты по окружению ({@link TestEnvironmentDefinition}) и нарезает
 * каждую группу на части фиксированного размера {@link #BATCH_SIZE}.
 */
public class Batches {

	private static final int BATCH_SIZE = 50;

	/**
	 * Декоратор по умолчанию: создаёт одно состояние без поворота для каждого инстанса.
	 */
	public static final Decorator DEFAULT_DECORATOR = (instance, world) -> Stream.of(
			new GameTestState(instance, BlockRotation.NONE, world, TestAttemptConfig.once())
	);

	/**
	 * Разбивает коллекцию инстансов на батчи с помощью заданного декоратора.
	 *
	 * @param instances  коллекция ссылок на тестовые инстансы
	 * @param decorator  декоратор, преобразующий инстанс в поток состояний
	 * @param world      серверный мир, в котором будут выполняться тесты
	 * @return список батчей, сгруппированных по окружению
	 */
	public static List<GameTestBatch> batch(
			Collection<RegistryEntry.Reference<TestInstance>> instances,
			Decorator decorator,
			ServerWorld world
	) {
		Map<RegistryEntry<TestEnvironmentDefinition>, List<GameTestState>> byEnvironment = instances.stream()
				.flatMap(instance -> decorator.decorate(instance, world))
				.collect(Collectors.groupingBy(state -> state.getInstance().getEnvironment()));

		return byEnvironment.entrySet().stream().flatMap(entry -> {
			RegistryEntry<TestEnvironmentDefinition> environment = entry.getKey();
			List<GameTestState> states = entry.getValue();
			return Streams.mapWithIndex(
					Lists.partition(states, BATCH_SIZE).stream(),
					(partition, index) -> create(partition, environment, (int) index)
			);
		}).toList();
	}

	/**
	 * @return батчер с размером батча по умолчанию ({@value #BATCH_SIZE})
	 */
	public static TestRunContext.Batcher defaultBatcher() {
		return batcher(BATCH_SIZE);
	}

	/**
	 * Создаёт батчер с заданным максимальным размером батча.
	 *
	 * @param batchSize максимальное количество тестов в одном батче
	 */
	public static TestRunContext.Batcher batcher(int batchSize) {
		return states -> {
			Map<RegistryEntry<TestEnvironmentDefinition>, List<GameTestState>> byEnvironment = states.stream()
					.filter(Objects::nonNull)
					.collect(Collectors.groupingBy(state -> state.getInstance().getEnvironment()));

			return byEnvironment.entrySet().stream().flatMap(entry -> {
				RegistryEntry<TestEnvironmentDefinition> environment = entry.getKey();
				List<GameTestState> partition = entry.getValue();
				return Streams.mapWithIndex(
						Lists.partition(partition, batchSize).stream(),
						(chunk, index) -> create(List.copyOf(chunk), environment, (int) index)
				);
			}).toList();
		};
	}

	/**
	 * Создаёт батч из набора состояний, окружения и порядкового номера.
	 *
	 * @param states      состояния тестов, входящие в батч
	 * @param environment окружение, общее для всех тестов батча
	 * @param index       порядковый номер батча внутри группы окружения
	 */
	public static GameTestBatch create(
			Collection<GameTestState> states,
			RegistryEntry<TestEnvironmentDefinition> environment,
			int index
	) {
		return new GameTestBatch(index, states, environment);
	}

	/**
	 * Декоратор, преобразующий ссылку на тестовый инстанс в поток {@link GameTestState}.
	 * Позволяет создавать несколько состояний из одного инстанса (например, для верификации).
	 */
	@FunctionalInterface
	public interface Decorator {

		Stream<GameTestState> decorate(RegistryEntry.Reference<TestInstance> instance, ServerWorld world);
	}
}
