package net.minecraft.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * Редкость предмета, определяющая цвет его названия в интерфейсе.
 */
public enum Rarity implements StringIdentifiable {
	COMMON(0, "common", Formatting.WHITE),
	UNCOMMON(1, "uncommon", Formatting.YELLOW),
	RARE(2, "rare", Formatting.AQUA),
	EPIC(3, "epic", Formatting.LIGHT_PURPLE);

	public static final Codec<Rarity> CODEC = StringIdentifiable.createBasicCodec(Rarity::values);
	public static final IntFunction<Rarity> ID_TO_VALUE = ValueLists.createIndexToValueFunction(
		(Rarity value) -> value.index, values(), ValueLists.OutOfBoundsHandling.ZERO
	);
	public static final PacketCodec<ByteBuf, Rarity> PACKET_CODEC = PacketCodecs.indexed(
		ID_TO_VALUE, value -> value.index
	);

	private final int index;
	private final String name;
	private final Formatting formatting;

	Rarity(int index, String name, Formatting formatting) {
		this.index = index;
		this.name = name;
		this.formatting = formatting;
	}

	public Formatting getFormatting() {
		return formatting;
	}

	@Override
	public String asString() {
		return name;
	}
}
