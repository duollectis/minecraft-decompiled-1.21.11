package net.minecraft.network.packet.s2c.play;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;

/**
 * Запись set cursor item s2 c packet.
 */
public record SetCursorItemS2CPacket(ItemStack contents) implements Packet<ClientPlayPacketListener> {

	public static final PacketCodec<RegistryByteBuf, SetCursorItemS2CPacket> CODEC = PacketCodec.tuple(
			ItemStack.OPTIONAL_PACKET_CODEC, SetCursorItemS2CPacket::contents, SetCursorItemS2CPacket::new
	);

	@Override
	public PacketType<SetCursorItemS2CPacket> getPacketType() {
		return PlayPackets.SET_CURSOR_ITEM;
	}

	/**
	 * Apply.
	 *
	 * @param clientPlayPacketListener client play packet listener
	 */
	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onSetCursorItem(this);
	}
}
