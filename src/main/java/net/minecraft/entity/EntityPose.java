package net.minecraft.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * Поза сущности, определяющая форму хитбокса и анимацию.
 * Каждая поза имеет числовой индекс для сетевой передачи и строковый идентификатор
 * для сериализации в NBT/JSON.
 */
@Getter
public enum EntityPose implements StringIdentifiable {
	STANDING(0, "standing"),
	GLIDING(1, "fall_flying"),
	SLEEPING(2, "sleeping"),
	SWIMMING(3, "swimming"),
	SPIN_ATTACK(4, "spin_attack"),
	CROUCHING(5, "crouching"),
	LONG_JUMPING(6, "long_jumping"),
	DYING(7, "dying"),
	CROAKING(8, "croaking"),
	USING_TONGUE(9, "using_tongue"),
	SITTING(10, "sitting"),
	ROARING(11, "roaring"),
	SNIFFING(12, "sniffing"),
	EMERGING(13, "emerging"),
	DIGGING(14, "digging"),
	SLIDING(15, "sliding"),
	SHOOTING(16, "shooting"),
	INHALING(17, "inhaling");

	public static final IntFunction<EntityPose> INDEX_TO_VALUE = ValueLists.<EntityPose>createIndexToValueFunction(
		e -> e.index, values(), ValueLists.OutOfBoundsHandling.ZERO
	);
	public static final Codec<EntityPose> CODEC = StringIdentifiable.createCodec(EntityPose::values);
	public static final PacketCodec<ByteBuf, EntityPose> PACKET_CODEC =
		PacketCodecs.indexed(INDEX_TO_VALUE, e -> e.index);

	private final int index;
	private final String name;

	EntityPose(int index, String name) {
		this.index = index;
		this.name = name;
	}

	@Override
	public String asString() {
		return name;
	}
}
