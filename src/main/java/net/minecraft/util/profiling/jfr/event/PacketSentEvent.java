package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import net.minecraft.obfuscate.DontObfuscate;

import java.net.SocketAddress;

@Name("minecraft.PacketSent")
@Label("Network Packet Sent")
@DontObfuscate
/**
 * {@code PacketSentEvent}.
 */
public class PacketSentEvent extends PacketEvent {

	public static final String NAME = "minecraft.PacketSent";
	public static final EventType TYPE = EventType.getEventType(PacketSentEvent.class);

	public PacketSentEvent(String string, String string2, String string3, SocketAddress socketAddress, int i) {
		super(string, string2, string3, socketAddress, i);
	}
}
