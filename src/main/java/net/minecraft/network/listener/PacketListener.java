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
 * Интерфейс packet listener.
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
		return this.isConnectionOpen();
	}

	default void fillCrashReport(CrashReport report) {
		CrashReportSection crashReportSection = report.addElement("Connection");
		crashReportSection.add("Protocol", () -> this.getPhase().getId());
		crashReportSection.add("Flow", () -> this.getSide().toString());
		this.addCustomCrashReportInfo(report, crashReportSection);
	}

	default void addCustomCrashReportInfo(CrashReport report, CrashReportSection section) {
	}
}
