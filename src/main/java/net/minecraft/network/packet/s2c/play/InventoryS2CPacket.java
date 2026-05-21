package net.minecraft.network.packet.s2c.play;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;

import java.util.List;

/**
 * Запись inventory s2 c packet.
 */
public record InventoryS2CPacket(
		int syncId,
		int revision,
		List<ItemStack> contents,
		ItemStack cursorStack
) implements Packet<ClientPlayPacketListener> {

	public static final PacketCodec<RegistryByteBuf, InventoryS2CPacket> CODEC = PacketCodec.tuple(
			PacketCodecs.SYNC_ID,
			InventoryS2CPacket::syncId,
			PacketCodecs.VAR_INT,
			InventoryS2CPacket::revision,
			ItemStack.OPTIONAL_LIST_PACKET_CODEC,
			InventoryS2CPacket::contents,
			ItemStack.OPTIONAL_PACKET_CODEC,
			InventoryS2CPacket::cursorStack,
			InventoryS2CPacket::new
	);

	@Override
	public PacketType<InventoryS2CPacket> getPacketType() {
		return PlayPackets.CONTAINER_SET_CONTENT;
	}

	/**
	 * Apply.
	 *
	 * @param clientPlayPacketListener client play packet listener
	 */
	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onInventory(this);
	}
}
