package net.minecraft.network.packet.c2s.config;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ServerConfigurationPacketListener;
import net.minecraft.network.packet.ConfigPackets;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;

/**
 * Пакет C→S, сигнализирующий серверу о готовности клиента завершить фазу конфигурации.
 * После его получения сервер переводит соединение в игровое состояние (PLAY).
 */
public class ReadyC2SPacket implements Packet<ServerConfigurationPacketListener> {

	public static final ReadyC2SPacket INSTANCE = new ReadyC2SPacket();
	public static final PacketCodec<ByteBuf, ReadyC2SPacket> CODEC = PacketCodec.unit(INSTANCE);

	private ReadyC2SPacket() {
	}

	@Override
	public PacketType<ReadyC2SPacket> getPacketType() {
		return ConfigPackets.FINISH_CONFIGURATION_C2S;
	}

	@Override
	public void apply(ServerConfigurationPacketListener listener) {
		listener.onReady(this);
	}

	@Override
	public boolean transitionsNetworkState() {
		return true;
	}
}
