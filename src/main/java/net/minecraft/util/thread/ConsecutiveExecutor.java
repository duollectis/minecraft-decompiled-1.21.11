package net.minecraft.util.thread;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.SampleType;
import net.minecraft.util.profiler.Sampler;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Базовый исполнитель, гарантирующий последовательное (не параллельное) выполнение задач.
 * Использует атомарный конечный автомат ({@link Status}) для управления состоянием:
 * задача из очереди запускается только тогда, когда исполнитель переходит из {@code SLEEPING} в {@code RUNNING}.
 * Это исключает одновременное выполнение нескольких задач из одной очереди.
 */
public abstract class ConsecutiveExecutor<T extends Runnable> implements SampleableExecutor, TaskExecutor<T>, Runnable {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final AtomicReference<Status> status = new AtomicReference<>(Status.SLEEPING);
	private final TaskQueue<T> queue;
	private final Executor executor;
	private final String name;

	public ConsecutiveExecutor(TaskQueue<T> queue, Executor executor, String name) {
		this.executor = executor;
		this.queue = queue;
		this.name = name;
		ExecutorSampling.INSTANCE.add(this);
	}

	private boolean canRun() {
		return !isClosed() && !queue.isEmpty();
	}

	@Override
	public void close() {
		status.set(Status.CLOSED);
	}

	private boolean runOnce() {
		if (!isRunning()) {
			return false;
		}

		Runnable runnable = queue.poll();

		if (runnable == null) {
			return false;
		}

		Util.runInNamedZone(runnable, name);
		return true;
	}

	@Override
	public void run() {
		try {
			runOnce();
		}
		finally {
			sleep();
			scheduleSelf();
		}
	}

	public void runAll() {
		try {
			while (runOnce()) {
			}
		}
		finally {
			sleep();
			scheduleSelf();
		}
	}

	@Override
	public void send(T runnable) {
		queue.add(runnable);
		scheduleSelf();
	}

	private void scheduleSelf() {
		if (!canRun() || !wakeUp()) {
			return;
		}

		try {
			executor.execute(this);
		}
		catch (RejectedExecutionException firstReject) {
			try {
				executor.execute(this);
			}
			catch (RejectedExecutionException secondReject) {
				LOGGER.error("Could not schedule ConsecutiveExecutor", secondReject);
			}
		}
	}

	public int queueSize() {
		return queue.getSize();
	}

	public boolean hasQueuedTasks() {
		return isRunning() && !queue.isEmpty();
	}

	@Override
	public String toString() {
		return name + " " + status.get() + " " + queue.isEmpty();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<Sampler> createSamplers() {
		return ImmutableList.of(Sampler.create(
			name + "-queue-size",
			SampleType.CONSECUTIVE_EXECUTORS,
			this::queueSize
		));
	}

	private boolean wakeUp() {
		return status.compareAndSet(Status.SLEEPING, Status.RUNNING);
	}

	private void sleep() {
		status.compareAndSet(Status.RUNNING, Status.SLEEPING);
	}

	private boolean isRunning() {
		return status.get() == Status.RUNNING;
	}

	private boolean isClosed() {
		return status.get() == Status.CLOSED;
	}

	enum Status {
		SLEEPING,
		RUNNING,
		CLOSED;
	}
}
