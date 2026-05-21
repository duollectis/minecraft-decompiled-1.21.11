package net.minecraft.util.profiling.jfr.sample;

import jdk.jfr.consumer.RecordedEvent;

/**
 * {@code PacketSample}.
 */
public record PacketSample(String side, String protocolId, String packetId) {

	/**
	 * From event.
	 *
	 * @param event event
	 *
	 * @return PacketSample — результат операции
	 */
	public static PacketSample fromEvent(RecordedEvent event) {
		return new PacketSample(
				event.getString("packetDirection"),
				event.getString("protocolId"),
				event.getString("packetId")
		);
	}
}
