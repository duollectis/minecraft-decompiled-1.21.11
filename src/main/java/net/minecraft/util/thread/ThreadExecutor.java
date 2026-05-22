package net.minecraft.util.thread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.profiler.SampleType;
import net.minecraft.util.profiler.Sampler;
import org.slf4j.Logger;

import javax.annotation.CheckReturnValue;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Однопоточный исполнитель с собственной очередью задач. Задачи выполняются строго
 * на «владеющем» потоке (определяется через {@link #getThread()}). Если вызов происходит
 * из другого потока, задача ставится в очередь; если с того же потока — выполняется немедленно.
 * Поддерживает Tracy-зоны для профилирования и регистрируется в {@link ExecutorSampling}.
 */
public abstract class ThreadExecutor<R extends Runnable> implements SampleableExecutor, TaskExecutor<R>, Executor {

	public static final long YIELD_INTERVAL_NS = 100000L;

	private static final Logger LOGGER = LogUtils.getLogger();

	private final String name;
	private final Queue<R> tasks = Queues.newConcurrentLinkedQueue();
	private int executionsInProgress;

	protected ThreadExecutor(String name) {
		this.name = name;
		ExecutorSampling.INSTANCE.add(this);
	}

	protected abstract boolean canExecute(R task);

	public boolean isOnThread() {
		return Thread.currentThread() == getThread();
	}

	protected abstract Thread getThread();

	protected boolean shouldExecuteAsync() {
		return !isOnThread();
	}

	public int getTaskCount() {
		return tasks.size();
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Выполняет задачу немедленно, если вызов происходит с владеющего потока,
	 * иначе ставит её в очередь и возвращает {@link CompletableFuture} для ожидания результата.
	 */
	public <V> CompletableFuture<V> submit(Supplier<V> task) {
		return shouldExecuteAsync()
			? CompletableFuture.supplyAsync(task, this)
			: CompletableFuture.completedFuture(task.get());
	}

	private CompletableFuture<Void> submitAsync(Runnable runnable) {
		return CompletableFuture.supplyAsync(
			() -> {
				runnable.run();
				return null;
			},
			this
		);
	}

	/**
	 * Выполняет задачу немедленно, если вызов происходит с владеющего потока,
	 * иначе ставит её в очередь асинхронно.
	 */
	@CheckReturnValue
	public CompletableFuture<Void> submit(Runnable task) {
		if (shouldExecuteAsync()) {
			return submitAsync(task);
		}

		task.run();
		return CompletableFuture.completedFuture(null);
	}

	public void submitAndJoin(Runnable runnable) {
		if (!isOnThread()) {
			submitAsync(runnable).join();
			return;
		}

		runnable.run();
	}

	@Override
	public void send(R runnable) {
		tasks.add(runnable);
		LockSupport.unpark(getThread());
	}

	@Override
	public void execute(Runnable runnable) {
		R task = createTask(runnable);

		if (shouldExecuteAsync()) {
			send(task);
		}
		else {
			executeTask(task);
		}
	}

	public void executeSync(Runnable runnable) {
		execute(runnable);
	}

	protected void cancelTasks() {
		tasks.clear();
	}

	protected void runTasks() {
		while (runTask()) {
		}
	}

	protected boolean isExecutionInProgress() {
		return executionsInProgress > 0;
	}

	public boolean runTask() {
		R runnable = tasks.peek();

		if (runnable == null) {
			return false;
		}

		if (!isExecutionInProgress() && !canExecute(runnable)) {
			return false;
		}

		executeTask(tasks.remove());
		return true;
	}

	/**
	 * Выполняет задачи из очереди до тех пор, пока {@code stopCondition} не вернёт {@code true}.
	 * Если очередь пуста — уступает процессор через {@link #waitForTasks()}.
	 */
	public void runTasks(BooleanSupplier stopCondition) {
		executionsInProgress++;

		try {
			while (!stopCondition.getAsBoolean()) {
				if (!runTask()) {
					waitForTasks();
				}
			}
		}
		finally {
			executionsInProgress--;
		}
	}

	protected void waitForTasks() {
		Thread.yield();
		LockSupport.parkNanos("waiting for tasks", YIELD_INTERVAL_NS);
	}

	protected void executeTask(R task) {
		try {
			try (Zone zone = TracyClient.beginZone("Task", SharedConstants.isDevelopment)) {
				task.run();
			}
		}
		catch (Exception exception) {
			LOGGER.error(LogUtils.FATAL_MARKER, "Error executing task on {}", getName(), exception);

			if (isMemoryError(exception)) {
				throw exception;
			}
		}
	}

	@Override
	public List<Sampler> createSamplers() {
		return ImmutableList.of(Sampler.create(
			name + "-pending-tasks",
			SampleType.EVENT_LOOPS,
			this::getTaskCount
		));
	}

	public static boolean isMemoryError(Throwable exception) {
		return exception instanceof CrashException crashException
			? isMemoryError(crashException.getCause())
			: exception instanceof OutOfMemoryError || exception instanceof StackOverflowError;
	}
}
