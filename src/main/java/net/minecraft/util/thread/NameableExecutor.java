package net.minecraft.util.thread;

import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import net.minecraft.SharedConstants;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * {@code NameableExecutor}.
 */
public record NameableExecutor(ExecutorService service) implements Executor {

	/**
	 * Named.
	 *
	 * @param name name
	 *
	 * @return Executor — результат операции
	 */
	public Executor named(String name) {
		if (SharedConstants.isDevelopment) {
			return runnable -> this.service.execute(() -> {
				Thread thread = Thread.currentThread();
				String string2 = thread.getName();
				thread.setName(name);

				try {
					Zone zone = TracyClient.beginZone(name, SharedConstants.isDevelopment);

					try {
						runnable.run();
					}
					catch (Throwable var12) {
						if (zone != null) {
							try {
								zone.close();
							}
							catch (Throwable var11) {
								var12.addSuppressed(var11);
							}
						}

						throw var12;
					}

					if (zone != null) {
						zone.close();
					}
				}
				finally {
					thread.setName(string2);
				}
			});
		}
		else {
			if (TracyClient.isAvailable()) {
				return runnable -> this.service.execute(() -> {
					Zone zone = TracyClient.beginZone(name, SharedConstants.isDevelopment);

					try {
						runnable.run();
					}
					catch (Throwable var6) {
						if (zone != null) {
							try {
								zone.close();
							}
							catch (Throwable var5) {
								var6.addSuppressed(var5);
							}
						}

						throw var6;
					}

					if (zone != null) {
						zone.close();
					}
				});
			}

			return this.service;
		}
	}

	@Override
	public void execute(Runnable runnable) {
		this.service.execute(wrapForTracy(runnable));
	}

	/**
	 * Shutdown.
	 *
	 * @param time time
	 * @param unit unit
	 */
	public void shutdown(long time, TimeUnit unit) {
		this.service.shutdown();

		boolean bl;
		try {
			bl = this.service.awaitTermination(time, unit);
		}
		catch (InterruptedException var6) {
			bl = false;
		}

		if (!bl) {
			this.service.shutdownNow();
		}
	}

	private static Runnable wrapForTracy(Runnable runnable) {
		return !TracyClient.isAvailable() ? runnable : () -> {
			Zone zone = TracyClient.beginZone("task", SharedConstants.isDevelopment);

			try {
				runnable.run();
			}
			catch (Throwable var5) {
				if (zone != null) {
					try {
						zone.close();
					}
					catch (Throwable var4) {
						var5.addSuppressed(var4);
					}
				}

				throw var5;
			}

			if (zone != null) {
				zone.close();
			}
		};
	}
}
