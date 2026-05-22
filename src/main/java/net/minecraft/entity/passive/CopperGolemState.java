package net.minecraft.entity.passive;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * Состояние медного голема: активен или застыл от окисления.
 */
public enum CopperGolemState implements StringIdentifiable {
	IDLE("idle", 0),
	GETTING_ITEM("getting_item", 1),
	GETTING_NO_ITEM("getting_no_item", 2),
	DROPPING_ITEM("dropping_item", 3),
	DROPPING_NO_ITEM("dropping_no_item", 4);

	public static final Codec<CopperGolemState> CODEC = StringIdentifiable.createCodec(CopperGolemState::values);
	private static final IntFunction<CopperGolemState> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
			CopperGolemState::getIndex, values(), ValueLists.OutOfBoundsHandling.ZERO
	);
	public static final PacketCodec<ByteBuf, CopperGolemState>
			PACKET_CODEC =
			PacketCodecs.indexed(INDEX_MAPPER, CopperGolemState::getIndex);
	private final String id;
	private final int index;

	private CopperGolemState(final String id, final int index) {
		this.id = id;
		this.index = index;
	}

	@Override
	public String asString() {
		return this.id;
	}

	private int getIndex() {
		return this.index;
	}
}
