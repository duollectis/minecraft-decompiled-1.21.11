package net.minecraft.item.consume;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * {@code UseAction}.
 */
public enum UseAction implements StringIdentifiable {
	NONE(0, "none"),
	EAT(1, "eat", true),
	DRINK(2, "drink", true),
	BLOCK(3, "block"),
	BOW(4, "bow"),
	TRIDENT(5, "trident"),
	CROSSBOW(6, "crossbow"),
	SPYGLASS(7, "spyglass"),
	TOOT_HORN(8, "toot_horn"),
	BRUSH(9, "brush"),
	BUNDLE(10, "bundle"),
	SPEAR(11, "spear", true);

	private static final IntFunction<UseAction>
			BY_ID =
			ValueLists.createIndexToValueFunction(UseAction::getId, values(), ValueLists.OutOfBoundsHandling.ZERO);
	public static final Codec<UseAction> CODEC = StringIdentifiable.createCodec(UseAction::values);
	public static final PacketCodec<ByteBuf, UseAction> PACKET_CODEC = PacketCodecs.indexed(BY_ID, UseAction::getId);
	private final int id;
	private final String name;
	private final boolean noOffset;

	private UseAction(final int id, final String name) {
		this(id, name, false);
	}

	private UseAction(final int id, final String name, final boolean noOffset) {
		this.id = id;
		this.name = name;
		this.noOffset = noOffset;
	}

	public int getId() {
		return this.id;
	}

	@Override
	public String asString() {
		return this.name;
	}

	public boolean hasNoOffset() {
		return this.noOffset;
	}
}
