package net.minecraft.util.math;

import com.mojang.serialization.Codec;
import net.minecraft.util.Util;

import java.util.List;

/**
 * Иммутабельный двумерный вектор с компонентами типа {@code float}.
 */
public class Vec2f {

	public static final Vec2f ZERO = new Vec2f(0.0F, 0.0F);
	public static final Vec2f SOUTH_EAST_UNIT = new Vec2f(1.0F, 1.0F);
	public static final Vec2f EAST_UNIT = new Vec2f(1.0F, 0.0F);
	public static final Vec2f WEST_UNIT = new Vec2f(-1.0F, 0.0F);
	public static final Vec2f SOUTH_UNIT = new Vec2f(0.0F, 1.0F);
	public static final Vec2f NORTH_UNIT = new Vec2f(0.0F, -1.0F);
	public static final Vec2f MAX_SOUTH_EAST = new Vec2f(Float.MAX_VALUE, Float.MAX_VALUE);
	public static final Vec2f MIN_SOUTH_EAST = new Vec2f(Float.MIN_VALUE, Float.MIN_VALUE);
	public static final Codec<Vec2f> CODEC = Codec.FLOAT
		.listOf()
		.comapFlatMap(
			rawList -> Util
				.decodeFixedLengthList(rawList, 2)
				.map(list -> new Vec2f((Float) list.get(0), (Float) list.get(1))),
			vec -> List.of(vec.x, vec.y)
		);

	public final float x;
	public final float y;

	public Vec2f(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Vec2f multiply(float value) {
		return new Vec2f(x * value, y * value);
	}

	public float dot(Vec2f vec) {
		return x * vec.x + y * vec.y;
	}

	public Vec2f add(Vec2f vec) {
		return new Vec2f(x + vec.x, y + vec.y);
	}

	public Vec2f add(float value) {
		return new Vec2f(x + value, y + value);
	}

	public boolean equals(Vec2f other) {
		return x == other.x && y == other.y;
	}

	public Vec2f normalize() {
		float len = MathHelper.sqrt(x * x + y * y);
		return len < 1.0E-4F ? ZERO : new Vec2f(x / len, y / len);
	}

	public float length() {
		return MathHelper.sqrt(x * x + y * y);
	}

	public float lengthSquared() {
		return x * x + y * y;
	}

	public float distanceSquared(Vec2f vec) {
		float dx = vec.x - x;
		float dy = vec.y - y;
		return dx * dx + dy * dy;
	}

	public Vec2f negate() {
		return new Vec2f(-x, -y);
	}
}
