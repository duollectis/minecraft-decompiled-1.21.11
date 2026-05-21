package net.minecraft.dialog;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * {@code AfterAction}.
 */
public enum AfterAction implements StringIdentifiable {
	CLOSE(0, "close"),
	NONE(1, "none"),
	WAIT_FOR_RESPONSE(2, "wait_for_response");

	public static final IntFunction<AfterAction> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
			(AfterAction afterAction) -> afterAction.index, values(), ValueLists.OutOfBoundsHandling.ZERO
	);
	public static final StringIdentifiable.EnumCodec<AfterAction>
			CODEC =
			StringIdentifiable.createCodec(AfterAction::values);
	public static final PacketCodec<ByteBuf, AfterAction>
			PACKET_CODEC =
			PacketCodecs.indexed(INDEX_MAPPER, afterAction -> afterAction.index);
	private final int index;
	private final String id;

	private AfterAction(final int index, final String id) {
		this.index = index;
		this.id = id;
	}

	@Override
	public String asString() {
		return this.id;
	}

	public boolean canUnpause() {
		return this == CLOSE || this == WAIT_FOR_RESPONSE;
	}
}
