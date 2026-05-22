package net.minecraft.world;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import org.jspecify.annotations.Nullable;

import java.util.function.IntFunction;

/**
 * Уровень сложности игры.
 * Влияет на поведение мобов, урон, голод и другие игровые механики.
 */
public enum Difficulty implements StringIdentifiable {
	PEACEFUL(0, "peaceful"),
	EASY(1, "easy"),
	NORMAL(2, "normal"),
	HARD(3, "hard");

	public static final StringIdentifiable.EnumCodec<Difficulty> CODEC = StringIdentifiable.createCodec(Difficulty::values);
	private static final IntFunction<Difficulty> BY_ID = ValueLists.createIndexToValueFunction(
		Difficulty::getId, values(), ValueLists.OutOfBoundsHandling.WRAP
	);
	public static final PacketCodec<ByteBuf, Difficulty> PACKET_CODEC = PacketCodecs.indexed(BY_ID, Difficulty::getId);

	private final int id;
	private final String name;

	Difficulty(final int id, final String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String asString() {
		return name;
	}

	public Text getTranslatableName() {
		return Text.translatable("options.difficulty." + name);
	}

	public Text getInfo() {
		return Text.translatable("options.difficulty." + name + ".info");
	}

	@Deprecated
	public static Difficulty byId(int id) {
		return BY_ID.apply(id);
	}

	public static @Nullable Difficulty byName(String name) {
		return CODEC.byId(name);
	}
}
