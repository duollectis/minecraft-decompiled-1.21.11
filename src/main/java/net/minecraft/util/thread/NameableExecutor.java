package net.minecraft.util.thread;

import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import net.minecraft.SharedConstants;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Обёртка над {@link ExecutorService}, добавляющая поддержку именованных Tracy-зон
 * и переименования потока на время выполнения задачи (только в режиме разработки).
 */
public record NameableExecutor(ExecutorService service) implements Executor {

	/**
	 * Возвращает {@link Executor}, оборачивающий каждую задачу в Tracy-зону с именем {@code name}.
	 * В режиме разработки дополнительно переименовывает поток на время выполнения.
	 * Если Tracy недоступен и не режим разработки — возвращает сам {@link #service} напрямую.
	 */
	public Executor named(String name) {
		if (SharedConstants.isDevelopment) {
			return runnable -> service.execute(() -> {
				Thread thread = Thread.currentThread();
				String previousName = thread.getName();
				thread.setName(name);

				try (Zone zone = TracyClient.beginZone(name, SharedConstants.isDevelopment)) {
					runnable.run();
				}
				finally {
					thread.setName(previousName);
				}
			});
		}

		if (TracyClient.isAvailable()) {
			return runnable -> service.execute(() -> {
				try (Zone zone = TracyClient.beginZone(name, SharedConstants.isDevelopment)) {
					runnable.run();
				}
			});
		}

		return service;
	}

	@Override
	public void execute(Runnable runnable) {
		service.execute(wrapForTracy(runnable));
	}

	public void shutdown(long time, TimeUnit unit) {
		service.shutdown();

		boolean terminated;

		try {
			terminated = service.awaitTermination(time, unit);
		}
		catch (InterruptedException interrupted) {
			terminated = false;
		}

		if (!terminated) {
			service.shutdownNow();
		}
	}

	private static Runnable wrapForTracy(Runnable runnable) {
		if (!TracyClient.isAvailable()) {
			return runnable;
		}

		return () -> {
			try (Zone zone = TracyClient.beginZone("task", SharedConstants.isDevelopment)) {
				runnable.run();
			}
		};
	}
}
