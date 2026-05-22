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
 * Пакет сервер→клиент для добавления рецептов в книгу рецептов игрока.
 * Флаг {@code replace} указывает, нужно ли заменить весь список рецептов или дополнить его.
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

	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onRecipeBookAdd(this);
	}

	/**
	 * Запись рецепта в книге рецептов с битовыми флагами отображения.
	 * Флаг {@link #SHOW_NOTIFICATION} управляет всплывающим уведомлением,
	 * {@link #HIGHLIGHTED} — подсветкой нового рецепта в интерфейсе.
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
			this(display, (byte) ((showNotification ? SHOW_NOTIFICATION : 0) | (highlighted ? HIGHLIGHTED : 0)));
		}

		public boolean shouldShowNotification() {
			return (flags & SHOW_NOTIFICATION) != 0;
		}

		public boolean isHighlighted() {
			return (flags & HIGHLIGHTED) != 0;
		}
	}
}
