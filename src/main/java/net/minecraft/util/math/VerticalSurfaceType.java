package net.minecraft.util.math;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

/**
 * Тип вертикальной поверхности блока: потолок или пол.
 * Используется при генерации мира для определения направления размещения объектов.
 */
public enum VerticalSurfaceType implements StringIdentifiable {
	CEILING(Direction.UP, 1, "ceiling"),
	FLOOR(Direction.DOWN, -1, "floor");

	public static final Codec<VerticalSurfaceType> CODEC = StringIdentifiable.createCodec(VerticalSurfaceType::values);

	private final Direction direction;
	private final int offset;
	private final String name;

	VerticalSurfaceType(final Direction direction, final int offset, final String name) {
		this.direction = direction;
		this.offset = offset;
		this.name = name;
	}

	public Direction getDirection() {
		return direction;
	}

	public int getOffset() {
		return offset;
	}

	@Override
	public String asString() {
		return name;
	}
}
