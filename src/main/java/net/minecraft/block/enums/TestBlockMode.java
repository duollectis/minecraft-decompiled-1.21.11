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
 * Режим работы тестового блока (Test Block) в игровых тестах.
 * Определяет поведение блока при выполнении автоматизированных тестов структур.
 */
public enum TestBlockMode implements StringIdentifiable {

	/** Запускает тест при активации. */
	START(0, "start"),
	/** Логирует результат теста без завершения. */
	LOG(1, "log"),
	/** Помечает тест как проваленный. */
	FAIL(2, "fail"),
	/** Помечает тест как успешно пройденный. */
	ACCEPT(3, "accept");

	private static final IntFunction<TestBlockMode> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
			(TestBlockMode mode) -> mode.index,
			values(),
			ValueLists.OutOfBoundsHandling.ZERO
	);

	public static final Codec<TestBlockMode> CODEC = StringIdentifiable.createCodec(TestBlockMode::values);
	public static final PacketCodec<ByteBuf, TestBlockMode> PACKET_CODEC = PacketCodecs.indexed(
			INDEX_MAPPER,
			mode -> mode.index
	);

	private final int index;
	private final String id;
	private final Text name;
	private final Text info;

	TestBlockMode(final int index, final String id) {
		this.index = index;
		this.id = id;
		this.name = Text.translatable("test_block.mode." + id);
		this.info = Text.translatable("test_block.mode_info." + id);
	}

	@Override
	public String asString() {
		return id;
	}

	public Text getName() {
		return name;
	}

	public Text getInfo() {
		return info;
	}
}
