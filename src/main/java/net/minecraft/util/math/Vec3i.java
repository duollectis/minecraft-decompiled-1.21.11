package net.minecraft.util.math;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Vector3i;

import java.util.stream.IntStream;

/**
 * Иммутабельный трёхмерный вектор с целочисленными компонентами.
 * Является базовым классом для {@code BlockPos} и других позиционных типов.
 */
@Unmodifiable
public class Vec3i implements Comparable<Vec3i> {

	public static final Codec<Vec3i> CODEC = Codec.INT_STREAM
		.comapFlatMap(
			stream -> Util
				.decodeFixedLengthIntArray(stream, 3)
				.map(coordinates -> new Vec3i(coordinates[0], coordinates[1], coordinates[2])),
			vec -> IntStream.of(vec.getX(), vec.getY(), vec.getZ())
		);

	public static final PacketCodec<ByteBuf, Vec3i> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.VAR_INT,
		Vec3i::getX,
		PacketCodecs.VAR_INT,
		Vec3i::getY,
		PacketCodecs.VAR_INT,
		Vec3i::getZ,
		Vec3i::new
	);

	public static final Vec3i ZERO = new Vec3i(0, 0, 0);

	private int x;
	private int y;
	private int z;

	public static Codec<Vec3i> createOffsetCodec(int maxAbsValue) {
		return CODEC.validate(
			vec -> Math.abs(vec.getX()) < maxAbsValue
				&& Math.abs(vec.getY()) < maxAbsValue
				&& Math.abs(vec.getZ()) < maxAbsValue
				? DataResult.success(vec)
				: DataResult.error(() -> "Position out of range, expected at most " + maxAbsValue + ": " + vec)
		);
	}

	public Vec3i(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof Vec3i vec3i)) {
			return false;
		}

		return getX() == vec3i.getX() && getY() == vec3i.getY() && getZ() == vec3i.getZ();
	}

	@Override
	public int hashCode() {
		return (getY() + getZ() * 31) * 31 + getX();
	}

	@Override
	public int compareTo(Vec3i vec3i) {
		if (getY() != vec3i.getY()) {
			return getY() - vec3i.getY();
		}

		return getZ() != vec3i.getZ() ? getZ() - vec3i.getZ() : getX() - vec3i.getX();
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	protected Vec3i setX(int x) {
		this.x = x;
		return this;
	}

	protected Vec3i setY(int y) {
		this.y = y;
		return this;
	}

	protected Vec3i setZ(int z) {
		this.z = z;
		return this;
	}

	public Vec3i add(int x, int y, int z) {
		return x == 0 && y == 0 && z == 0
			? this
			: new Vec3i(getX() + x, getY() + y, getZ() + z);
	}

	public Vec3i add(Vec3i vec) {
		return add(vec.getX(), vec.getY(), vec.getZ());
	}

	public Vec3i subtract(Vec3i vec) {
		return add(-vec.getX(), -vec.getY(), -vec.getZ());
	}

	public Vec3i multiply(int scale) {
		if (scale == 1) {
			return this;
		}

		return scale == 0 ? ZERO : new Vec3i(getX() * scale, getY() * scale, getZ() * scale);
	}

	public Vec3i multiply(int scaleX, int scaleY, int scaleZ) {
		return new Vec3i(getX() * scaleX, getY() * scaleY, getZ() * scaleZ);
	}

	public Vec3i up() {
		return up(1);
	}

	public Vec3i up(int distance) {
		return offset(Direction.UP, distance);
	}

	public Vec3i down() {
		return down(1);
	}

	public Vec3i down(int distance) {
		return offset(Direction.DOWN, distance);
	}

	public Vec3i north() {
		return north(1);
	}

	public Vec3i north(int distance) {
		return offset(Direction.NORTH, distance);
	}

	public Vec3i south() {
		return south(1);
	}

	public Vec3i south(int distance) {
		return offset(Direction.SOUTH, distance);
	}

	public Vec3i west() {
		return west(1);
	}

	public Vec3i west(int distance) {
		return offset(Direction.WEST, distance);
	}

	public Vec3i east() {
		return east(1);
	}

	public Vec3i east(int distance) {
		return offset(Direction.EAST, distance);
	}

	public Vec3i offset(Direction direction) {
		return offset(direction, 1);
	}

	public Vec3i offset(Direction direction, int distance) {
		return distance == 0
			? this
			: new Vec3i(
				getX() + direction.getOffsetX() * distance,
				getY() + direction.getOffsetY() * distance,
				getZ() + direction.getOffsetZ() * distance
			);
	}

	public Vec3i offset(Direction.Axis axis, int distance) {
		if (distance == 0) {
			return this;
		}

		int dx = axis == Direction.Axis.X ? distance : 0;
		int dy = axis == Direction.Axis.Y ? distance : 0;
		int dz = axis == Direction.Axis.Z ? distance : 0;

		return new Vec3i(getX() + dx, getY() + dy, getZ() + dz);
	}

	public Vec3i crossProduct(Vec3i vec) {
		return new Vec3i(
			getY() * vec.getZ() - getZ() * vec.getY(),
			getZ() * vec.getX() - getX() * vec.getZ(),
			getX() * vec.getY() - getY() * vec.getX()
		);
	}

	public boolean isWithinDistance(Vec3i vec, double distance) {
		return getSquaredDistance(vec) < MathHelper.square(distance);
	}

	public boolean isWithinDistance(Position pos, double distance) {
		return getSquaredDistance(pos) < MathHelper.square(distance);
	}

	public double getSquaredDistance(Vec3i vec) {
		return getSquaredDistance(vec.getX(), vec.getY(), vec.getZ());
	}

	public double getSquaredDistance(Position pos) {
		return getSquaredDistanceFromCenter(pos.getX(), pos.getY(), pos.getZ());
	}

	public double getSquaredDistanceFromCenter(double x, double y, double z) {
		double dx = getX() + 0.5 - x;
		double dy = getY() + 0.5 - y;
		double dz = getZ() + 0.5 - z;
		return dx * dx + dy * dy + dz * dz;
	}

	public double getSquaredDistance(double x, double y, double z) {
		double dx = getX() - x;
		double dy = getY() - y;
		double dz = getZ() - z;
		return dx * dx + dy * dy + dz * dz;
	}

	public int getManhattanDistance(Vec3i vec) {
		int dx = Math.abs(vec.getX() - getX());
		int dy = Math.abs(vec.getY() - getY());
		int dz = Math.abs(vec.getZ() - getZ());
		return dx + dy + dz;
	}

	public int getChebyshevDistance(Vec3i vec) {
		int dx = Math.abs(getX() - vec.getX());
		int dy = Math.abs(getY() - vec.getY());
		int dz = Math.abs(getZ() - vec.getZ());
		return Math.max(Math.max(dx, dy), dz);
	}

	public int getComponentAlongAxis(Direction.Axis axis) {
		return axis.choose(x, y, z);
	}

	public Vector3i asVector3i() {
		return new Vector3i(x, y, z);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("x", getX())
			.add("y", getY())
			.add("z", getZ())
			.toString();
	}

	public String toShortString() {
		return getX() + ", " + getY() + ", " + getZ();
	}
}
