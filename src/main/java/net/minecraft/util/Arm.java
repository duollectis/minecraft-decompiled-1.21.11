package net.minecraft.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * Рука игрока — основная или вспомогательная.
 * Используется для определения, какой рукой выполняется действие,
 * а также для настройки доминирующей руки в параметрах игрока.
 */
public enum Arm implements StringIdentifiable {
	LEFT(0, "left", "options.mainHand.left"),
	RIGHT(1, "right", "options.mainHand.right");

	public static final Codec<Arm> CODEC = StringIdentifiable.createCodec(Arm::values);
	private static final IntFunction<Arm> BY_ID = ValueLists.<Arm>createIndexToValueFunction(
		arm -> arm.id, values(), ValueLists.OutOfBoundsHandling.ZERO
	);
	public static final PacketCodec<ByteBuf, Arm> PACKET_CODEC = PacketCodecs.indexed(BY_ID, arm -> arm.id);

	private final int id;
	private final String name;
	private final Text text;

	Arm(int id, String name, String translationKey) {
		this.id = id;
		this.name = name;
		this.text = Text.translatable(translationKey);
	}

	/** @return противоположная рука */
	public Arm getOpposite() {
		return switch (this) {
			case LEFT -> RIGHT;
			case RIGHT -> LEFT;
		};
	}

	public Text getText() {
		return text;
	}

	@Override
	public String asString() {
		return name;
	}
}
