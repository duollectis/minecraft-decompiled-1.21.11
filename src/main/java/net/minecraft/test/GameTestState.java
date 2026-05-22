package net.minecraft.test;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.TestInstanceBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Состояние одного запуска игрового теста.
 * <p>
 * Хранит всю информацию о текущем прогоне: тик, флаги завершения/провала,
 * список слушателей, очередь задач по тикам и ссылку на блок-сущность теста.
 * Жизненный цикл: создание → {@link #init()} → тики через {@link #tick(TestRunContext)} →
 * завершение через {@link #completeIfSuccessful()} или {@link #fail}.
 */
public class GameTestState {

	private final RegistryEntry.Reference<TestInstance> instanceEntry;
	private @Nullable BlockPos testBlockPos;
	private final ServerWorld world;
	private final Collection<TestListener> listeners = Lists.newArrayList();
	private final int tickLimit;
	private final Collection<TimedTaskRunner> timedTaskRunners = Lists.newCopyOnWriteArrayList();
	private final Object2LongMap<Runnable> ticksByRunnables = new Object2LongOpenHashMap<>();
	private boolean initialized;
	private boolean tickedOnce;
	private int tick;
	private boolean started;
	private final TestAttemptConfig testAttemptConfig;
	private final Stopwatch stopwatch = Stopwatch.createUnstarted();
	private boolean completed;
	private final BlockRotation rotation;
	private @Nullable TestException exception;
	private @Nullable TestInstanceBlockEntity blockEntity;

	public GameTestState(
			RegistryEntry.Reference<TestInstance> instanceEntry,
			BlockRotation rotation,
			ServerWorld world,
			TestAttemptConfig testAttemptConfig
	) {
		this.instanceEntry = instanceEntry;
		this.world = world;
		this.testAttemptConfig = testAttemptConfig;
		this.tickLimit = instanceEntry.value().getMaxTicks();
		this.rotation = rotation;
	}

	public void setTestBlockPos(@Nullable BlockPos testBlockPos) {
		this.testBlockPos = testBlockPos;
	}

	/**
	 * Устанавливает отрицательный тик для отсчёта задержки перед стартом теста.
	 *
	 * @param additionalExpectedStopTime дополнительное время ожидания в тиках
	 * @return {@code this} для цепочки вызовов
	 */
	public GameTestState startCountdown(int additionalExpectedStopTime) {
		tick = -(instanceEntry.value().getSetupTicks() + additionalExpectedStopTime + 1);
		return this;
	}

	/**
	 * Немедленно инициализирует тест: размещает структуру, барьеры и уведомляет слушателей.
	 * Вызывается только один раз; повторные вызовы игнорируются.
	 */
	public void initializeImmediately() {
		if (initialized) {
			return;
		}

		TestInstanceBlockEntity entity = getTestInstanceBlockEntity();
		if (!entity.placeStructure()) {
			fail(Text.translatable("test.error.structure.failure", entity.getTestName().getString()));
		}

		initialized = true;
		entity.placeBarriers();
		BlockBox blockBox = entity.getBlockBox();
		world.getBlockTickScheduler().clearNextTicks(blockBox);
		world.clearUpdatesInArea(blockBox);
		listeners.forEach(listener -> listener.onStarted(this));
	}

	/**
	 * Выполняет один тик теста: проверяет инициализацию, запускает задачи по расписанию
	 * и уведомляет слушателей о завершении.
	 */
	public void tick(TestRunContext context) {
		if (isCompleted()) {
			return;
		}

		if (!initialized) {
			fail(Text.translatable("test.error.ticking_without_structure"));
		}

		if (blockEntity == null) {
			fail(Text.translatable("test.error.missing_block_entity"));
		}

		if (exception != null) {
			complete();
		}

		boolean chunksLoaded = blockEntity
				.getBlockBox()
				.streamChunkPos()
				.allMatch(world::shouldTickTestAt);

		if (tickedOnce || chunksLoaded) {
			tickedOnce = true;
			tickTests();

			if (isCompleted()) {
				if (exception != null) {
					listeners.forEach(listener -> listener.onFailed(this, context));
				} else {
					listeners.forEach(listener -> listener.onPassed(this, context));
				}
			}
		}
	}

	/**
	 * Планирует выполнение задачи на конкретный тик.
	 *
	 * @param tick     абсолютный номер тика
	 * @param runnable задача для выполнения
	 */
	public void runAtTick(long tick, Runnable runnable) {
		ticksByRunnables.put(runnable, tick);
	}

	public Identifier getId() {
		return instanceEntry.registryKey().getValue();
	}

	public @Nullable BlockPos getPos() {
		return testBlockPos;
	}

	public BlockPos getOrigin() {
		return blockEntity.getStartPos();
	}

	public Box getBoundingBox() {
		return getTestInstanceBlockEntity().getBox();
	}

	/**
	 * Возвращает блок-сущность теста, при необходимости загружая её из мира.
	 *
	 * @throws IllegalStateException если позиция не задана или блок-сущность не найдена
	 */
	public TestInstanceBlockEntity getTestInstanceBlockEntity() {
		if (blockEntity != null) {
			return blockEntity;
		}

		if (testBlockPos == null) {
			throw new IllegalStateException("This GameTestInfo has no position");
		}

		if (world.getBlockEntity(testBlockPos) instanceof TestInstanceBlockEntity entity) {
			blockEntity = entity;
		}

		if (blockEntity == null) {
			throw new IllegalStateException(
					"Could not find a test instance block entity at the given coordinate " + testBlockPos);
		}

		return blockEntity;
	}

	public ServerWorld getWorld() {
		return world;
	}

	public boolean isPassed() {
		return completed && exception == null;
	}

	public boolean isFailed() {
		return exception != null;
	}

	public boolean isStarted() {
		return started;
	}

	public boolean isCompleted() {
		return completed;
	}

	public long getElapsedMilliseconds() {
		return stopwatch.elapsed(TimeUnit.MILLISECONDS);
	}

	public void completeIfSuccessful() {
		if (exception != null) {
			return;
		}

		complete();
		Box box = getBoundingBox();
		List<Entity> entities = world.getEntitiesByClass(
				Entity.class,
				box.expand(1.0),
				entity -> !(entity instanceof PlayerEntity)
		);
		entities.forEach(entity -> entity.remove(Entity.RemovalReason.DISCARDED));
	}

	public void fail(Text message) {
		fail(new GameTestException(message, tick));
	}

	public void fail(TestException exception) {
		this.exception = exception;
	}

	public @Nullable TestException getThrowable() {
		return exception;
	}

	@Override
	public String toString() {
		return getId().toString();
	}

	public void addListener(TestListener listener) {
		listeners.add(listener);
	}

	/**
	 * Инициализирует тест на заданной позиции: размещает блок-сущность и структуру.
	 *
	 * @return {@code this} при успехе, {@code null} если блок-сущность не удалось создать
	 */
	public @Nullable GameTestState init() {
		TestInstanceBlockEntity entity = placeTestInstance(
				Objects.requireNonNull(testBlockPos),
				rotation,
				world
		);
		if (entity == null) {
			return null;
		}

		blockEntity = entity;
		initializeImmediately();
		return this;
	}

	public boolean isRequired() {
		return instanceEntry.value().isRequired();
	}

	public boolean isOptional() {
		return !instanceEntry.value().isRequired();
	}

	public Identifier getStructure() {
		return instanceEntry.value().getStructure();
	}

	public BlockRotation getRotation() {
		return instanceEntry.value().getData().rotation().rotate(rotation);
	}

	public TestInstance getInstance() {
		return instanceEntry.value();
	}

	public RegistryEntry.Reference<TestInstance> getInstanceEntry() {
		return instanceEntry;
	}

	public int getTickLimit() {
		return tickLimit;
	}

	public boolean isFlaky() {
		return instanceEntry.value().getMaxAttempts() > 1;
	}

	public int getMaxAttempts() {
		return instanceEntry.value().getMaxAttempts();
	}

	public int getRequiredSuccesses() {
		return instanceEntry.value().getRequiredSuccesses();
	}

	public TestAttemptConfig getTestAttemptConfig() {
		return testAttemptConfig;
	}

	public Stream<TestListener> streamListeners() {
		return listeners.stream();
	}

	public GameTestState copy() {
		GameTestState copy = new GameTestState(instanceEntry, rotation, world, testAttemptConfig);
		if (testBlockPos != null) {
			copy.setTestBlockPos(testBlockPos);
		}

		return copy;
	}

	int getTick() {
		return tick;
	}

	TimedTaskRunner createTimedTaskRunner() {
		TimedTaskRunner runner = new TimedTaskRunner(this);
		timedTaskRunners.add(runner);
		return runner;
	}

	private void complete() {
		if (completed) {
			return;
		}

		completed = true;
		if (stopwatch.isRunning()) {
			stopwatch.stop();
		}
	}

	/**
	 * Выполняет все задачи, запланированные на текущий тик, и проверяет лимит тиков.
	 */
	private void tickTests() {
		tick++;
		if (tick < 0) {
			return;
		}

		if (!started) {
			start();
		}

		var entryIterator = ticksByRunnables.object2LongEntrySet().iterator();
		while (entryIterator.hasNext()) {
			var entry = entryIterator.next();
			if (entry.getLongValue() <= tick) {
				try {
					entry.getKey().run();
				} catch (TestException testException) {
					fail(testException);
				} catch (Exception ex) {
					fail(new UnknownTestException(ex));
				}

				entryIterator.remove();
			}
		}

		if (tick > tickLimit) {
			if (timedTaskRunners.isEmpty()) {
				fail(new TickLimitExceededException(Text.translatable(
						"test.error.timeout.no_result",
						instanceEntry.value().getMaxTicks()
				)));
			} else {
				timedTaskRunners.forEach(runner -> runner.runReported(tick));
				if (exception == null) {
					fail(new TickLimitExceededException(Text.translatable(
							"test.error.timeout.no_sequences_finished",
							instanceEntry.value().getMaxTicks()
					)));
				}
			}
		} else {
			timedTaskRunners.forEach(runner -> runner.runSilently(tick));
		}
	}

	private void start() {
		if (started) {
			return;
		}

		started = true;
		stopwatch.start();
		getTestInstanceBlockEntity().setRunning();

		try {
			instanceEntry.value().start(new TestContext(this));
		} catch (TestException testException) {
			fail(testException);
		} catch (Exception ex) {
			fail(new UnknownTestException(ex));
		}
	}

	private @Nullable TestInstanceBlockEntity placeTestInstance(
			BlockPos pos,
			BlockRotation blockRotation,
			ServerWorld serverWorld
	) {
		serverWorld.setBlockState(pos, Blocks.TEST_INSTANCE_BLOCK.getDefaultState());
		if (!(serverWorld.getBlockEntity(pos) instanceof TestInstanceBlockEntity entity)) {
			return null;
		}

		RegistryKey<TestInstance> registryKey = instanceEntry.registryKey();
		Vec3i size = TestInstanceBlockEntity.getStructureSize(serverWorld, registryKey)
				.orElse(new Vec3i(1, 1, 1));
		entity.setData(new TestInstanceBlockEntity.Data(
				Optional.of(registryKey),
				size,
				blockRotation,
				false,
				TestInstanceBlockEntity.Status.CLEARED,
				Optional.empty()
		));
		return entity;
	}
}
