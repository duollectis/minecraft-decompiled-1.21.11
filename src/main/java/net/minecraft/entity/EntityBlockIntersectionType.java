package net.minecraft.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * Тип пересечения сущности с блоком окружающей среды.
 * Используется для определения, в каком состоянии находится сущность
 * относительно блоков: внутри твёрдого блока, в жидкости или в воздухе.
 * Цвет используется для отладочной визуализации.
 */
public enum EntityBlockIntersectionType {
	IN_BLOCK(0, 0x5FE0_FF00),
	IN_FLUID(1, 0x5FE0_00FF),
	IN_AIR(2, 0x6030_B0F3);

	private static final IntFunction<EntityBlockIntersectionType> BY_ID =
		ValueLists.<EntityBlockIntersectionType>createIndexToValueFunction(
			type -> type.id, values(), ValueLists.OutOfBoundsHandling.ZERO
		);

	public static final PacketCodec<ByteBuf, EntityBlockIntersectionType> PACKET_CODEC =
		PacketCodecs.indexed(BY_ID, type -> type.id);

	private final int id;
	private final int color;

	EntityBlockIntersectionType(int id, int color) {
		this.id = id;
		this.color = color;
	}

	public int getColor() {
		return color;
	}
}
