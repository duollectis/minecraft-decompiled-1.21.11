package net.minecraft.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * Тип анимации замаха оружием при атаке.
 * Определяет визуальное поведение руки игрока при использовании предмета.
 */
public enum SwingAnimationType implements StringIdentifiable {
	NONE(0, "none"),
	WHACK(1, "whack"),
	STAB(2, "stab");

	private static final IntFunction<SwingAnimationType> BY_PACKET_ID = ValueLists.createIndexToValueFunction(
		SwingAnimationType::getPacketId, values(), ValueLists.OutOfBoundsHandling.ZERO
	);
	public static final Codec<SwingAnimationType> CODEC = StringIdentifiable.createCodec(SwingAnimationType::values);
	public static final PacketCodec<ByteBuf, SwingAnimationType> PACKET_CODEC = PacketCodecs.indexed(
		BY_PACKET_ID, SwingAnimationType::getPacketId
	);

	private final int packetId;
	private final String name;

	SwingAnimationType(int packetId, String name) {
		this.packetId = packetId;
		this.name = name;
	}

	public int getPacketId() {
		return packetId;
	}

	@Override
	public String asString() {
		return name;
	}
}
