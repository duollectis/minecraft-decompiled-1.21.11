package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.*;
import net.minecraft.obfuscate.DontObfuscate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JFR-событие сводной статистики сети. Записывается каждые 10 секунд и содержит
 * суммарное количество отправленных и полученных байт/пакетов для конкретного соединения.
 */
@Name("minecraft.NetworkSummary")
@Label("Network Summary")
@Category({"Minecraft", "Network"})
@StackTrace(false)
@Period("10 s")
@DontObfuscate
public class NetworkSummaryEvent extends Event {

	public static final String EVENT_NAME = "minecraft.NetworkSummary";
	public static final EventType TYPE = EventType.getEventType(NetworkSummaryEvent.class);
	@Name("remoteAddress")
	@Label("Remote Address")
	public final String remoteAddress;
	@Name("sentBytes")
	@Label("Sent Bytes")
	@DataAmount
	public long sentBytes;
	@Name("sentPackets")
	@Label("Sent Packets")
	public int sentPackets;
	@Name("receivedBytes")
	@Label("Received Bytes")
	@DataAmount
	public long receivedBytes;
	@Name("receivedPackets")
	@Label("Received Packets")
	public int receivedPackets;

	public NetworkSummaryEvent(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public static final class Names {

		public static final String REMOTE_ADDRESS = "remoteAddress";
		public static final String SENT_BYTES = "sentBytes";
		public static final String SENT_PACKETS = "sentPackets";
		public static final String RECEIVED_BYTES = "receivedBytes";
		public static final String RECEIVED_PACKETS = "receivedPackets";

		private Names() {
		}
	}

	public static final class Recorder {

		private final AtomicLong sentBytes = new AtomicLong();
		private final AtomicInteger sentPackets = new AtomicInteger();
		private final AtomicLong receivedBytes = new AtomicLong();
		private final AtomicInteger receivedPackets = new AtomicInteger();
		private final NetworkSummaryEvent event;

		public Recorder(String remoteAddress) {
			this.event = new NetworkSummaryEvent(remoteAddress);
			this.event.begin();
		}

		public void addSentPacket(int bytes) {
			this.sentPackets.incrementAndGet();
			this.sentBytes.addAndGet(bytes);
		}

		public void addReceivedPacket(int bytes) {
			this.receivedPackets.incrementAndGet();
			this.receivedBytes.addAndGet(bytes);
		}

		public void commit() {
			this.event.sentBytes = this.sentBytes.get();
			this.event.sentPackets = this.sentPackets.get();
			this.event.receivedBytes = this.receivedBytes.get();
			this.event.receivedPackets = this.receivedPackets.get();
			this.event.commit();
		}
	}
}
