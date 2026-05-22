package net.minecraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.dedicated.DedicatedServerWatchdog;
import net.minecraft.util.crash.CrashReport;

import java.io.File;
import java.time.Duration;

/**
 * Сторожевой поток клиента: если завершение игры зависает дольше {@link #SHUTDOWN_TIMEOUT},
 * принудительно сохраняет crash-репорт и завершает процесс.
 */
@Environment(EnvType.CLIENT)
public class ClientWatchdog {

	private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(15L);

	/**
	 * Запускает фоновый поток-сторож, который ждёт {@link #SHUTDOWN_TIMEOUT} и,
	 * если завершение не произошло, сохраняет crash-репорт с дампом стека главного потока.
	 *
	 * @param runDir   рабочая директория игры для сохранения crash-репорта
	 * @param threadId идентификатор главного потока для дампа стека
	 */
	public static void shutdownClient(File runDir, long threadId) {
		Thread watchdog = new Thread(() -> {
			try {
				Thread.sleep(SHUTDOWN_TIMEOUT);
			} catch (InterruptedException ignored) {
				return;
			}

			CrashReport crashReport = DedicatedServerWatchdog.createCrashReport("Client shutdown", threadId);
			MinecraftClient.saveCrashReport(runDir, crashReport);
		});

		watchdog.setDaemon(true);
		watchdog.setName("Client shutdown watchdog");
		watchdog.start();
	}
}
