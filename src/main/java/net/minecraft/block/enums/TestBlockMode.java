package net.minecraft.block.enums;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * {@code TestBlockMode}.
 */
public enum TestBlockMode implements StringIdentifiable {
	START(0, "start"),
	LOG(1, "log"),
	FAIL(2, "fail"),
	ACCEPT(3, "accept");

	private static final IntFunction<TestBlockMode> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
			(TestBlockMode mode) -> mode.index, values(), ValueLists.OutOfBoundsHandling.ZERO
	);
	public static final Codec<TestBlockMode> CODEC = StringIdentifiable.createCodec(TestBlockMode::values);
	public static final PacketCodec<ByteBuf, TestBlockMode>
			PACKET_CODEC =
			PacketCodecs.indexed(INDEX_MAPPER, mode -> mode.index);
	private final int index;
	private final String id;
	private final Text name;
	private final Text info;

	private TestBlockMode(final int index, final String id) {
		this.index = index;
		this.id = id;
		this.name = Text.translatable("test_block.mode." + id);
		this.info = Text.translatable("test_block.mode_info." + id);
	}

	@Override
	public String asString() {
		return this.id;
	}

	public Text getName() {
		return this.name;
	}

	public Text getInfo() {
		return this.info;
	}
}
