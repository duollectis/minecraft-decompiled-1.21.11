package net.minecraft.network.listener;

import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;

/**
 * Базовый контракт для всех слушателей сетевых пакетов.
 * Определяет сторону соединения ({@link NetworkSide}), фазу протокола ({@link NetworkPhase}),
 * обработку ошибок и заполнение отчётов о сбоях.
 */
public interface PacketListener {

	NetworkSide getSide();

	NetworkPhase getPhase();

	void onDisconnected(DisconnectionInfo info);

	default void onPacketException(Packet packet, Exception exception) throws CrashException {
		throw NetworkThreadUtils.createCrashException(exception, packet, this);
	}

	default DisconnectionInfo createDisconnectionInfo(Text reason, Throwable exception) {
		return new DisconnectionInfo(reason);
	}

	boolean isConnectionOpen();

	default boolean accepts(Packet<?> packet) {
		return isConnectionOpen();
	}

	default void fillCrashReport(CrashReport report) {
		CrashReportSection section = report.addElement("Connection");
		section.add("Protocol", () -> getPhase().getId());
		section.add("Flow", () -> getSide().toString());
		addCustomCrashReportInfo(report, section);
	}

	default void addCustomCrashReportInfo(CrashReport report, CrashReportSection section) {
	}
}
