package net.minecraft.recipe;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * Числовой идентификатор рецепта для сетевого протокола.
 * Используется вместо строкового ключа реестра для экономии трафика.
 */
public record NetworkRecipeId(int index) {

	public static final PacketCodec<ByteBuf, NetworkRecipeId> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.VAR_INT,
		NetworkRecipeId::index,
		NetworkRecipeId::new
	);
}
