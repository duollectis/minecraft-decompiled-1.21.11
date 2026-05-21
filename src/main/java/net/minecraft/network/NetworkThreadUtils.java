package net.minecraft.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Утилиты для безопасной обработки пакетов в контексте главного потока сервера.
 * <p>Обеспечивает перенаправление обработки пакетов на главный поток через {@link PacketApplyBatcher},
 * а также формирование детализированных отчётов о сбоях при обработке пакетов.
 */
public class NetworkThreadUtils {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Принудительно переносит обработку пакета на главный поток серверного мира.
	 *
	 * @param packet   пакет для обработки
	 * @param listener слушатель пакетов
	 * @param world    серверный мир, чей батчер используется
	 * @throws OffThreadException если вызов произошёл не в главном потоке
	 */
	public static <T extends PacketListener> void forceMainThread(
			Packet<T> packet,
			T listener,
			ServerWorld world
	) throws OffThreadException {
		forceMainThread(packet, listener, world.getServer().getPacketApplyBatcher());
	}

	/**
	 * Принудительно переносит обработку пакета на поток, обслуживаемый батчером.
	 *
	 * @param packet   пакет для обработки
	 * @param listener слушатель пакетов
	 * @param batcher  батчер, управляющий очередью пакетов
	 * @throws OffThreadException если вызов произошёл не в нужном потоке
	 */
	public static <T extends PacketListener> void forceMainThread(
			Packet<T> packet,
			T listener,
			PacketApplyBatcher batcher
	) throws OffThreadException {
		if (batcher.isOnThread()) {
			return;
		}

		batcher.add(listener, packet);
		throw OffThreadException.INSTANCE;
	}

	/**
	 * Оборачивает исключение обработки пакета в {@link CrashException} с подробным отчётом.
	 *
	 * @param exception исходное исключение
	 * @param packet    пакет, при обработке которого возникло исключение
	 * @param listener  слушатель, обрабатывавший пакет
	 * @return {@link CrashException} с заполненным отчётом
	 */
	public static <T extends PacketListener> CrashException createCrashException(
			Exception exception,
			Packet<T> packet,
			T listener
	) {
		if (exception instanceof CrashException crashException) {
			fillCrashReport(crashException.getReport(), listener, packet);
			return crashException;
		}

		CrashReport report = CrashReport.create(exception, "Main thread packet handler");
		fillCrashReport(report, listener, packet);
		return new CrashException(report);
	}

	/**
	 * Заполняет секцию отчёта о сбое информацией о пакете и слушателе.
	 *
	 * @param report   отчёт о сбое для заполнения
	 * @param listener слушатель, обрабатывавший пакет
	 * @param packet   пакет, вызвавший сбой (может быть {@code null})
	 */
	public static <T extends PacketListener> void fillCrashReport(
			CrashReport report,
			T listener,
			@Nullable Packet<T> packet
	) {
		if (packet != null) {
			CrashReportSection section = report.addElement("Incoming Packet");
			section.add("Type", () -> packet.getPacketType().toString());
			section.add("Is Terminal", () -> Boolean.toString(packet.transitionsNetworkState()));
			section.add("Is Skippable", () -> Boolean.toString(packet.isWritingErrorSkippable()));
		}

		listener.fillCrashReport(report);
	}
}
