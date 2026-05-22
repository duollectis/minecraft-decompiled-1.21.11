package net.minecraft.network.packet.c2s.common;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ServerCommonPacketListener;
import net.minecraft.network.packet.CommonPackets;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;

import java.util.UUID;

/**
 * Пакет C→S, сообщающий серверу о статусе загрузки ресурс-пака.
 * Перечисление {@link Status} описывает все возможные состояния: от принятия до ошибки загрузки.
 */
public record ResourcePackStatusC2SPacket(
		UUID id,
		ResourcePackStatusC2SPacket.Status status
) implements Packet<ServerCommonPacketListener> {

	public static final PacketCodec<PacketByteBuf, ResourcePackStatusC2SPacket> CODEC = Packet.createCodec(
			ResourcePackStatusC2SPacket::write, ResourcePackStatusC2SPacket::new
	);

	private ResourcePackStatusC2SPacket(PacketByteBuf buf) {
		this(buf.readUuid(), buf.readEnumConstant(ResourcePackStatusC2SPacket.Status.class));
	}

	private void write(PacketByteBuf buf) {
		buf.writeUuid(id);
		buf.writeEnumConstant(status);
	}

	@Override
	public PacketType<ResourcePackStatusC2SPacket> getPacketType() {
		return CommonPackets.RESOURCE_PACK;
	}

	@Override
	public void apply(ServerCommonPacketListener listener) {
		listener.onResourcePackStatus(this);
	}

	public enum Status {
		SUCCESSFULLY_LOADED,
		DECLINED,
		FAILED_DOWNLOAD,
		ACCEPTED,
		DOWNLOADED,
		INVALID_URL,
		FAILED_RELOAD,
		DISCARDED;

		public boolean hasFinished() {
			return this != ACCEPTED && this != DOWNLOADED;
		}
	}
}
