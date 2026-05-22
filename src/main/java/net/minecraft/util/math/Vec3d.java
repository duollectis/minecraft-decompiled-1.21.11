package net.minecraft.util.math;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.EnumSet;
import java.util.List;

/**
 * Иммутабельный трёхмерный вектор с компонентами типа {@code double}.
 * Реализует интерфейс {@link Position} для совместимости с системой координат мира.
 */
public class Vec3d implements Position {

	public static final Codec<Vec3d> CODEC = Codec.DOUBLE
		.listOf()
		.comapFlatMap(
			coordinates -> Util.decodeFixedLengthList(coordinates, 3)
				.map(coords -> new Vec3d(
					(Double) coords.get(0),
					(Double) coords.get(1),
					(Double) coords.get(2)
				)),
			vec -> List.of(vec.getX(), vec.getY(), vec.getZ())
		);

	public static final PacketCodec<ByteBuf, Vec3d> PACKET_CODEC = new PacketCodec<>() {
		@Override
		public Vec3d decode(ByteBuf byteBuf) {
			return PacketByteBuf.readVec3d(byteBuf);
		}

		@Override
		public void encode(ByteBuf byteBuf, Vec3d vec3d) {
			PacketByteBuf.writeVec3d(byteBuf, vec3d);
		}
	};

	public static final Vec3d ZERO = new Vec3d(0.0, 0.0, 0.0);
	public static final Vec3d X = new Vec3d(1.0, 0.0, 0.0);
	public static final Vec3d Y = new Vec3d(0.0, 1.0, 0.0);
	public static final Vec3d Z = new Vec3d(0.0, 0.0, 1.0);

	public final double x;
	public final double y;
	public final double z;

	public static Vec3d of(Vec3i vec) {
		return new Vec3d(vec.getX(), vec.getY(), vec.getZ());
	}

	public static Vec3d add(Vec3i vec, double deltaX, double deltaY, double deltaZ) {
		return new Vec3d(vec.getX() + deltaX, vec.getY() + deltaY, vec.getZ() + deltaZ);
	}

	public static Vec3d ofCenter(Vec3i vec) {
		return add(vec, 0.5, 0.5, 0.5);
	}

	public static Vec3d ofBottomCenter(Vec3i vec) {
		return add(vec, 0.5, 0.0, 0.5);
	}

	public static Vec3d ofCenter(Vec3i vec, double deltaY) {
		return add(vec, 0.5, deltaY, 0.5);
	}

	public Vec3d(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vec3d(Vector3fc vec) {
		this(vec.x(), vec.y(), vec.z());
	}

	public Vec3d(Vec3i vec) {
		this(vec.getX(), vec.getY(), vec.getZ());
	}

	public Vec3d relativize(Vec3d vec) {
		return new Vec3d(vec.x - x, vec.y - y, vec.z - z);
	}

	public Vec3d normalize() {
		double len = Math.sqrt(x * x + y * y + z * z);
		return len < 1.0E-5F ? ZERO : new Vec3d(x / len, y / len, z / len);
	}

	public double dotProduct(Vec3d vec) {
		return x * vec.x + y * vec.y + z * vec.z;
	}

	public Vec3d crossProduct(Vec3d vec) {
		return new Vec3d(
			y * vec.z - z * vec.y,
			z * vec.x - x * vec.z,
			x * vec.y - y * vec.x
		);
	}

	public Vec3d subtract(Vec3d vec) {
		return subtract(vec.x, vec.y, vec.z);
	}

	public Vec3d subtract(double value) {
		return subtract(value, value, value);
	}

	public Vec3d subtract(double x, double y, double z) {
		return add(-x, -y, -z);
	}

	public Vec3d add(double value) {
		return add(value, value, value);
	}

	public Vec3d add(Vec3d vec) {
		return add(vec.x, vec.y, vec.z);
	}

	public Vec3d add(double x, double y, double z) {
		return new Vec3d(this.x + x, this.y + y, this.z + z);
	}

	public boolean isInRange(Position pos, double radius) {
		return squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < radius * radius;
	}

	public double distanceTo(Vec3d vec) {
		double dx = vec.x - x;
		double dy = vec.y - y;
		double dz = vec.z - z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public double squaredDistanceTo(Vec3d vec) {
		double dx = vec.x - x;
		double dy = vec.y - y;
		double dz = vec.z - z;
		return dx * dx + dy * dy + dz * dz;
	}

	public double squaredDistanceTo(double x, double y, double z) {
		double dx = x - this.x;
		double dy = y - this.y;
		double dz = z - this.z;
		return dx * dx + dy * dy + dz * dz;
	}

	public boolean isWithinRangeOf(Vec3d vec, double horizontalRange, double verticalRange) {
		double dx = vec.getX() - x;
		double dy = vec.getY() - y;
		double dz = vec.getZ() - z;
		return MathHelper.squaredHypot(dx, dz) < MathHelper.square(horizontalRange) && Math.abs(dy) < verticalRange;
	}

	public Vec3d multiply(double value) {
		return multiply(value, value, value);
	}

	public Vec3d negate() {
		return multiply(-1.0);
	}

	public Vec3d multiply(Vec3d vec) {
		return multiply(vec.x, vec.y, vec.z);
	}

	public Vec3d multiply(double x, double y, double z) {
		return new Vec3d(this.x * x, this.y * y, this.z * z);
	}

	public Vec3d getHorizontal() {
		return new Vec3d(x, 0.0, z);
	}

	public Vec3d addRandom(Random random, float multiplier) {
		return add(
			(random.nextFloat() - 0.5F) * multiplier,
			(random.nextFloat() - 0.5F) * multiplier,
			(random.nextFloat() - 0.5F) * multiplier
		);
	}

	public Vec3d addHorizontalRandom(Random random, float multiplier) {
		return add((random.nextFloat() - 0.5F) * multiplier, 0.0, (random.nextFloat() - 0.5F) * multiplier);
	}

	public double length() {
		return Math.sqrt(x * x + y * y + z * z);
	}

	public double lengthSquared() {
		return x * x + y * y + z * z;
	}

	public double horizontalLength() {
		return Math.sqrt(x * x + z * z);
	}

	public double horizontalLengthSquared() {
		return x * x + z * z;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof Vec3d vec3d)) {
			return false;
		}

		if (Double.compare(vec3d.x, x) != 0) {
			return false;
		}

		if (Double.compare(vec3d.y, y) != 0) {
			return false;
		}

		return Double.compare(vec3d.z, z) == 0;
	}

	@Override
	public int hashCode() {
		long bits = Double.doubleToLongBits(x);
		int hash = (int) (bits ^ bits >>> 32);
		bits = Double.doubleToLongBits(y);
		hash = 31 * hash + (int) (bits ^ bits >>> 32);
		bits = Double.doubleToLongBits(z);
		return 31 * hash + (int) (bits ^ bits >>> 32);
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}

	public Vec3d lerp(Vec3d to, double delta) {
		return new Vec3d(
			MathHelper.lerp(delta, x, to.x),
			MathHelper.lerp(delta, y, to.y),
			MathHelper.lerp(delta, z, to.z)
		);
	}

	public Vec3d rotateX(float angle) {
		float cosA = MathHelper.cos(angle);
		float sinA = MathHelper.sin(angle);
		double rotY = y * cosA + z * sinA;
		double rotZ = z * cosA - y * sinA;
		return new Vec3d(x, rotY, rotZ);
	}

	public Vec3d rotateY(float angle) {
		float cosA = MathHelper.cos(angle);
		float sinA = MathHelper.sin(angle);
		double rotX = x * cosA + z * sinA;
		double rotZ = z * cosA - x * sinA;
		return new Vec3d(rotX, y, rotZ);
	}

	public Vec3d rotateZ(float angle) {
		float cosA = MathHelper.cos(angle);
		float sinA = MathHelper.sin(angle);
		double rotX = x * cosA + y * sinA;
		double rotY = y * cosA - x * sinA;
		return new Vec3d(rotX, rotY, z);
	}

	public Vec3d rotateYClockwise() {
		return new Vec3d(-z, y, x);
	}

	public static Vec3d fromPolar(Vec2f polar) {
		return fromPolar(polar.x, polar.y);
	}

	/**
	 * Преобразует сферические координаты (pitch, yaw) в единичный вектор направления.
	 * Использует соглашение Minecraft: yaw=0 → юг (+Z), pitch=0 → горизонталь.
	 *
	 * @param pitch угол наклона в градусах (−90..+90)
	 * @param yaw   угол поворота в градусах
	 * @return единичный вектор направления взгляда
	 */
	public static Vec3d fromPolar(float pitch, float yaw) {
		float cosYaw = MathHelper.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
		float sinYaw = MathHelper.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
		float cosPitch = -MathHelper.cos(-pitch * (float) (Math.PI / 180.0));
		float sinPitch = MathHelper.sin(-pitch * (float) (Math.PI / 180.0));
		return new Vec3d(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
	}

	public Vec2f getYawAndPitch() {
		float yaw = (float) Math.atan2(-x, z) * (180.0F / (float) Math.PI);
		float pitch = (float) Math.asin(-y / Math.sqrt(x * x + y * y + z * z)) * (180.0F / (float) Math.PI);
		return new Vec2f(pitch, yaw);
	}

	public Vec3d floorAlongAxes(EnumSet<Direction.Axis> axes) {
		double flooredX = axes.contains(Direction.Axis.X) ? MathHelper.floor(x) : x;
		double flooredY = axes.contains(Direction.Axis.Y) ? MathHelper.floor(y) : y;
		double flooredZ = axes.contains(Direction.Axis.Z) ? MathHelper.floor(z) : z;
		return new Vec3d(flooredX, flooredY, flooredZ);
	}

	public double getComponentAlongAxis(Direction.Axis axis) {
		return axis.choose(x, y, z);
	}

	public Vec3d withAxis(Direction.Axis axis, double value) {
		double newX = axis == Direction.Axis.X ? value : x;
		double newY = axis == Direction.Axis.Y ? value : y;
		double newZ = axis == Direction.Axis.Z ? value : z;
		return new Vec3d(newX, newY, newZ);
	}

	public Vec3d offset(Direction direction, double value) {
		Vec3i vec3i = direction.getVector();
		return new Vec3d(x + value * vec3i.getX(), y + value * vec3i.getY(), z + value * vec3i.getZ());
	}

	@Override
	public final double getX() {
		return x;
	}

	@Override
	public final double getY() {
		return y;
	}

	@Override
	public final double getZ() {
		return z;
	}

	public Vector3f toVector3f() {
		return new Vector3f((float) x, (float) y, (float) z);
	}

	public Vec3d projectOnto(Vec3d vec) {
		return vec.lengthSquared() == 0.0
			? vec
			: vec.multiply(dotProduct(vec)).multiply(1.0 / vec.lengthSquared());
	}

	/**
	 * Трансформирует локальный вектор {@code vec} в мировое пространство,
	 * используя ориентацию, заданную углами поворота {@code rotation} (pitch, yaw).
	 *
	 * @param rotation углы поворота (x=pitch, y=yaw) в градусах
	 * @param vec      вектор в локальном пространстве
	 * @return вектор в мировом пространстве
	 */
	public static Vec3d transformLocalPos(Vec2f rotation, Vec3d vec) {
		float cosYaw = MathHelper.cos((rotation.y + 90.0F) * (float) (Math.PI / 180.0));
		float sinYaw = MathHelper.sin((rotation.y + 90.0F) * (float) (Math.PI / 180.0));
		float cosPitch = MathHelper.cos(-rotation.x * (float) (Math.PI / 180.0));
		float sinPitch = MathHelper.sin(-rotation.x * (float) (Math.PI / 180.0));
		float cosPitchUp = MathHelper.cos((-rotation.x + 90.0F) * (float) (Math.PI / 180.0));
		float sinPitchUp = MathHelper.sin((-rotation.x + 90.0F) * (float) (Math.PI / 180.0));
		Vec3d forward = new Vec3d(cosYaw * cosPitch, sinPitch, sinYaw * cosPitch);
		Vec3d up = new Vec3d(cosYaw * cosPitchUp, sinPitchUp, sinYaw * cosPitchUp);
		Vec3d right = forward.crossProduct(up).multiply(-1.0);
		double worldX = forward.x * vec.z + up.x * vec.y + right.x * vec.x;
		double worldY = forward.y * vec.z + up.y * vec.y + right.y * vec.x;
		double worldZ = forward.z * vec.z + up.z * vec.y + right.z * vec.x;
		return new Vec3d(worldX, worldY, worldZ);
	}

	public Vec3d transformLocalPos(Vec3d vec) {
		return transformLocalPos(getYawAndPitch(), vec);
	}

	public boolean isFinite() {
		return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
	}
}
