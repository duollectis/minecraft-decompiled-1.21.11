package net.minecraft.network.packet.c2s.login;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ServerLoginPacketListener;
import net.minecraft.network.packet.LoginPackets;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;

/**
 * Класс enter configuration c2 s packet.
 */
public class EnterConfigurationC2SPacket implements Packet<ServerLoginPacketListener> {

	public static final EnterConfigurationC2SPacket INSTANCE = new EnterConfigurationC2SPacket();
	public static final PacketCodec<ByteBuf, EnterConfigurationC2SPacket> CODEC = PacketCodec.unit(INSTANCE);

	private EnterConfigurationC2SPacket() {
	}

	@Override
	public PacketType<EnterConfigurationC2SPacket> getPacketType() {
		return LoginPackets.LOGIN_ACKNOWLEDGED;
	}

	/**
	 * Apply.
	 *
	 * @param serverLoginPacketListener server login packet listener
	 */
	public void apply(ServerLoginPacketListener serverLoginPacketListener) {
		serverLoginPacketListener.onEnterConfiguration(this);
	}

	@Override
	public boolean transitionsNetworkState() {
		return true;
	}
}
