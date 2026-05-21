package net.minecraft.network.packet.c2s.play;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.recipe.NetworkRecipeId;

/**
 * Запись recipe book data c2 s packet.
 */
public record RecipeBookDataC2SPacket(NetworkRecipeId recipeId) implements Packet<ServerPlayPacketListener> {

	public static final PacketCodec<PacketByteBuf, RecipeBookDataC2SPacket> CODEC = PacketCodec.tuple(
			NetworkRecipeId.PACKET_CODEC, RecipeBookDataC2SPacket::recipeId, RecipeBookDataC2SPacket::new
	);

	@Override
	public PacketType<RecipeBookDataC2SPacket> getPacketType() {
		return PlayPackets.RECIPE_BOOK_SEEN_RECIPE;
	}

	/**
	 * Apply.
	 *
	 * @param serverPlayPacketListener server play packet listener
	 */
	public void apply(ServerPlayPacketListener serverPlayPacketListener) {
		serverPlayPacketListener.onRecipeBookData(this);
	}
}
