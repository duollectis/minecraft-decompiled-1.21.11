package net.minecraft.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.entity.TestInstanceBlockEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контекст выполнения набора тестовых батчей.
 * Управляет последовательным запуском батчей, применением/откатом окружений,
 * принудительной загрузкой чанков и повторным запуском упавших тестов.
 */
public class TestRunContext {

	public static final int DEFAULT_TESTS_PER_ROW = 8;
	private static final Logger LOGGER = LogUtils.getLogger();

	final ServerWorld world;
	private final TestManager manager;
	private final List<GameTestState> states;
	private ImmutableList<GameTestBatch> batches;
	final List<BatchListener> batchListeners = Lists.newArrayList();
	private final List<GameTestState> toBeRetried = Lists.newArrayList();
	private final Batcher batcher;
	private boolean stopped = true;
	private @Nullable RegistryEntry<TestEnvironmentDefinition> environment;
	private final TestStructureSpawner reuseSpawner;
	private final TestStructureSpawner initialSpawner;
	final boolean stopAfterFailure;
	private final boolean clearBetweenBatches;

	protected TestRunContext(
		Batcher batcher,
		Collection<GameTestBatch> batches,
		ServerWorld world,
		TestManager manager,
		TestStructureSpawner reuseSpawner,
		TestStructureSpawner initialSpawner,
		boolean stopAfterFailure,
		boolean clearBetweenBatches
	) {
		this.world = world;
		this.manager = manager;
		this.batcher = batcher;
		this.reuseSpawner = reuseSpawner;
		this.initialSpawner = initialSpawner;
		this.batches = ImmutableList.copyOf(batches);
		this.stopAfterFailure = stopAfterFailure;
		this.clearBetweenBatches = clearBetweenBatches;
		this.states = this.batches.stream()
			.flatMap(batch -> batch.states().stream())
			.collect(Util.toArrayList());
		manager.setRunContext(this);
		this.states.forEach(state -> state.addListener(new StructureTestListener()));
	}

	public List<GameTestState> getStates() {
		return states;
	}

	public void start() {
		stopped = false;
		runBatch(0);
	}

	public void clear() {
		stopped = true;

		if (environment != null) {
			clearEnvironment();
		}
	}

	public void retry(GameTestState state) {
		GameTestState copy = state.copy();
		state.streamListeners().forEach(listener -> listener.onRetry(state, copy, this));
		states.add(copy);
		toBeRetried.add(copy);

		if (stopped) {
			onFinish();
		}
	}

	/**
	 * Запускает батч по индексу. Если индекс выходит за пределы — завершает прогон.
	 * Перед запуском батча очищает предыдущий (если включено {@code clearBetweenBatches}),
	 * применяет окружение и принудительно загружает чанки.
	 */
	void runBatch(int batchIndex) {
		if (batchIndex >= batches.size()) {
			clearEnvironment();
			onFinish();
			return;
		}

		if (batchIndex > 0 && clearBetweenBatches) {
			GameTestBatch previousBatch = batches.get(batchIndex - 1);
			previousBatch.states().forEach(state -> {
				TestInstanceBlockEntity blockEntity = state.getTestInstanceBlockEntity();
				TestInstanceUtil.clearArea(blockEntity.getBlockBox(), world);
				world.breakBlock(blockEntity.getPos(), false);
			});
		}

		GameTestBatch batch = batches.get(batchIndex);
		reuseSpawner.onBatch(world);
		initialSpawner.onBatch(world);

		Collection<GameTestState> prepared = prepareStructures(batch.states());
		LOGGER.info(
			"Running test environment '{}' batch {} ({} tests)...",
			batch.environment().getIdAsString(),
			batch.index(),
			prepared.size()
		);

		clearEnvironment();
		environment = batch.environment();
		environment.value().setup(world);
		batchListeners.forEach(listener -> listener.onStarted(batch));

		TestSet testSet = new TestSet();
		prepared.forEach(testSet::add);
		testSet.addListener(new TestListener() {
			private void onFinished(GameTestState state) {
				state.getTestInstanceBlockEntity().clearBarriers();

				if (testSet.isDone()) {
					batchListeners.forEach(listener -> listener.onFinished(batch));
					unforceAllChunks();
					runBatch(batchIndex + 1);
				}
			}

			private void unforceAllChunks() {
				LongSet forcedChunks = new LongArraySet(world.getForcedChunks());
				forcedChunks.forEach(chunkPos -> world.setChunkForced(
					ChunkPos.getPackedX(chunkPos),
					ChunkPos.getPackedZ(chunkPos),
					false
				));
			}

			@Override
			public void onStarted(GameTestState test) {
			}

			@Override
			public void onPassed(GameTestState test, TestRunContext context) {
				onFinished(test);
			}

			@Override
			public void onFailed(GameTestState test, TestRunContext context) {
				if (stopAfterFailure) {
					clearEnvironment();
					unforceAllChunks();
					TestManager.INSTANCE.clear();
					test.getTestInstanceBlockEntity().clearBarriers();
				} else {
					onFinished(test);
				}
			}

			@Override
			public void onRetry(GameTestState lastState, GameTestState nextState, TestRunContext context) {
			}
		});

		prepared.forEach(manager::start);
	}

	void clearEnvironment() {
		if (environment == null) {
			return;
		}

		environment.value().teardown(world);
		environment = null;
	}

	private void onFinish() {
		if (toBeRetried.isEmpty()) {
			batches = ImmutableList.of();
			stopped = true;
			return;
		}

		LOGGER.info(
			"Starting re-run of tests: {}",
			toBeRetried.stream()
				.map(state -> state.getId().toString())
				.collect(Collectors.joining(", "))
		);
		batches = ImmutableList.copyOf(batcher.batch(toBeRetried));
		toBeRetried.clear();
		stopped = false;
		runBatch(0);
	}

	public void addBatchListener(BatchListener batchListener) {
		batchListeners.add(batchListener);
	}

	private Collection<GameTestState> prepareStructures(Collection<GameTestState> oldStates) {
		return oldStates.stream()
			.map(this::prepareStructure)
			.flatMap(Optional::stream)
			.toList();
	}

	private Optional<GameTestState> prepareStructure(GameTestState oldState) {
		return oldState.getPos() == null
			? initialSpawner.spawnStructure(oldState)
			: reuseSpawner.spawnStructure(oldState);
	}

	/**
	 * Стратегия разбивки набора тестов на батчи.
	 */
	public interface Batcher {

		Collection<GameTestBatch> batch(Collection<GameTestState> states);
	}

	/**
	 * Строитель {@link TestRunContext} с fluent API.
	 */
	public static class Builder {

		private final ServerWorld world;
		private final TestManager manager = TestManager.INSTANCE;
		private Batcher batcher = Batches.defaultBatcher();
		private TestStructureSpawner reuseSpawner = TestStructureSpawner.REUSE;
		private TestStructureSpawner initialSpawner = TestStructureSpawner.NOOP;
		private final Collection<GameTestBatch> batches;
		private boolean stopAfterFailure = false;
		private boolean clearBetweenBatches = false;

		private Builder(Collection<GameTestBatch> batches, ServerWorld world) {
			this.batches = batches;
			this.world = world;
		}

		public static Builder of(Collection<GameTestBatch> batches, ServerWorld world) {
			return new Builder(batches, world);
		}

		public static Builder ofStates(Collection<GameTestState> states, ServerWorld world) {
			return of(Batches.defaultBatcher().batch(states), world);
		}

		public Builder stopAfterFailure() {
			stopAfterFailure = true;
			return this;
		}

		public Builder clearBetweenBatches() {
			clearBetweenBatches = true;
			return this;
		}

		public Builder initialSpawner(TestStructureSpawner initialSpawner) {
			this.initialSpawner = initialSpawner;
			return this;
		}

		public Builder reuseSpawner(TestStructurePlacer reuseSpawner) {
			this.reuseSpawner = reuseSpawner;
			return this;
		}

		public Builder batcher(Batcher batcher) {
			this.batcher = batcher;
			return this;
		}

		public TestRunContext build() {
			return new TestRunContext(
				batcher,
				batches,
				world,
				manager,
				reuseSpawner,
				initialSpawner,
				stopAfterFailure,
				clearBetweenBatches
			);
		}
	}

	/**
	 * Стратегия размещения структур тестов в мире.
	 */
	public interface TestStructureSpawner {

		TestStructureSpawner REUSE =
			oldState -> Optional.ofNullable(oldState.init()).map(state -> state.startCountdown(1));

		TestStructureSpawner NOOP = oldState -> Optional.empty();

		Optional<GameTestState> spawnStructure(GameTestState oldState);

		default void onBatch(ServerWorld world) {
		}
	}
}
