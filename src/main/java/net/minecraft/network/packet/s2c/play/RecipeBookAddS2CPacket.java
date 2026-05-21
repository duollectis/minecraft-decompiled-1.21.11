package net.minecraft.network.packet.s2c.play;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.recipe.RecipeDisplayEntry;

import java.util.List;

/**
 * Запись recipe book add s2 c packet.
 */
public record RecipeBookAddS2CPacket(
		List<RecipeBookAddS2CPacket.Entry> entries,
		boolean replace
) implements Packet<ClientPlayPacketListener> {

	public static final PacketCodec<RegistryByteBuf, RecipeBookAddS2CPacket> CODEC = PacketCodec.tuple(
			RecipeBookAddS2CPacket.Entry.PACKET_CODEC.collect(PacketCodecs.toList()),
			RecipeBookAddS2CPacket::entries,
			PacketCodecs.BOOLEAN,
			RecipeBookAddS2CPacket::replace,
			RecipeBookAddS2CPacket::new
	);

	@Override
	public PacketType<RecipeBookAddS2CPacket> getPacketType() {
		return PlayPackets.RECIPE_BOOK_ADD;
	}

	/**
	 * Apply.
	 *
	 * @param clientPlayPacketListener client play packet listener
	 */
	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onRecipeBookAdd(this);
	}

	/**
	 * Запись entry.
	 */
	public record Entry(RecipeDisplayEntry contents, byte flags) {

		public static final byte SHOW_NOTIFICATION = 1;
		public static final byte HIGHLIGHTED = 2;
		public static final PacketCodec<RegistryByteBuf, RecipeBookAddS2CPacket.Entry> PACKET_CODEC = PacketCodec.tuple(
				RecipeDisplayEntry.PACKET_CODEC,
				RecipeBookAddS2CPacket.Entry::contents,
				PacketCodecs.BYTE,
				RecipeBookAddS2CPacket.Entry::flags,
				RecipeBookAddS2CPacket.Entry::new
		);

		public Entry(RecipeDisplayEntry display, boolean showNotification, boolean highlighted) {
			this(display, (byte) ((showNotification ? 1 : 0) | (highlighted ? 2 : 0)));
		}

		/**
		 * Определяет, следует ли show notification.
		 *
		 * @return boolean — результат операции
		 */
		public boolean shouldShowNotification() {
			return (this.flags & 1) != 0;
		}

		public boolean isHighlighted() {
			return (this.flags & 2) != 0;
		}
	}
}
