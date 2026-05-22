package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;

/**
 * Тип-единица (Unit type): единственное значение {@link #INSTANCE},
 * аналог {@code void} в контексте обобщённых типов.
 */
public enum Unit {
	INSTANCE;

	public static final Codec<Unit> CODEC = MapCodec.unitCodec(INSTANCE);
	public static final PacketCodec<ByteBuf, Unit> PACKET_CODEC = PacketCodec.unit(INSTANCE);
}
