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
 * {@code Vec3d}.
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
	public static final PacketCodec<ByteBuf, Vec3d> PACKET_CODEC = new PacketCodec<ByteBuf, Vec3d>() {
		public Vec3d decode(ByteBuf byteBuf) {
			return PacketByteBuf.readVec3d(byteBuf);
		}

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
		return new Vec3d(vec.x - this.x, vec.y - this.y, vec.z - this.z);
	}

	public Vec3d normalize() {
		double d = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
		return d < 1.0E-5F ? ZERO : new Vec3d(this.x / d, this.y / d, this.z / d);
	}

	public double dotProduct(Vec3d vec) {
		return this.x * vec.x + this.y * vec.y + this.z * vec.z;
	}

	public Vec3d crossProduct(Vec3d vec) {
		return new Vec3d(
				this.y * vec.z - this.z * vec.y,
				this.z * vec.x - this.x * vec.z,
				this.x * vec.y - this.y * vec.x
		);
	}

	public Vec3d subtract(Vec3d vec) {
		return this.subtract(vec.x, vec.y, vec.z);
	}

	public Vec3d subtract(double value) {
		return this.subtract(value, value, value);
	}

	public Vec3d subtract(double x, double y, double z) {
		return this.add(-x, -y, -z);
	}

	public Vec3d add(double value) {
		return this.add(value, value, value);
	}

	public Vec3d add(Vec3d vec) {
		return this.add(vec.x, vec.y, vec.z);
	}

	public Vec3d add(double x, double y, double z) {
		return new Vec3d(this.x + x, this.y + y, this.z + z);
	}

	public boolean isInRange(Position pos, double radius) {
		return this.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < radius * radius;
	}

	public double distanceTo(Vec3d vec) {
		double d = vec.x - this.x;
		double e = vec.y - this.y;
		double f = vec.z - this.z;
		return Math.sqrt(d * d + e * e + f * f);
	}

	public double squaredDistanceTo(Vec3d vec) {
		double d = vec.x - this.x;
		double e = vec.y - this.y;
		double f = vec.z - this.z;
		return d * d + e * e + f * f;
	}

	public double squaredDistanceTo(double x, double y, double z) {
		double d = x - this.x;
		double e = y - this.y;
		double f = z - this.z;
		return d * d + e * e + f * f;
	}

	public boolean isWithinRangeOf(Vec3d vec, double horizontalRange, double verticalRange) {
		double d = vec.getX() - this.x;
		double e = vec.getY() - this.y;
		double f = vec.getZ() - this.z;
		return MathHelper.squaredHypot(d, f) < MathHelper.square(horizontalRange) && Math.abs(e) < verticalRange;
	}

	public Vec3d multiply(double value) {
		return this.multiply(value, value, value);
	}

	public Vec3d negate() {
		return this.multiply(-1.0);
	}

	public Vec3d multiply(Vec3d vec) {
		return this.multiply(vec.x, vec.y, vec.z);
	}

	public Vec3d multiply(double x, double y, double z) {
		return new Vec3d(this.x * x, this.y * y, this.z * z);
	}

	public Vec3d getHorizontal() {
		return new Vec3d(this.x, 0.0, this.z);
	}

	public Vec3d addRandom(Random random, float multiplier) {
		return this.add(
				(random.nextFloat() - 0.5F) * multiplier,
				(random.nextFloat() - 0.5F) * multiplier,
				(random.nextFloat() - 0.5F) * multiplier
		);
	}

	public Vec3d addHorizontalRandom(Random random, float multiplier) {
		return this.add((random.nextFloat() - 0.5F) * multiplier, 0.0, (random.nextFloat() - 0.5F) * multiplier);
	}

	public double length() {
		return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
	}

	public double lengthSquared() {
		return this.x * this.x + this.y * this.y + this.z * this.z;
	}

	public double horizontalLength() {
		return Math.sqrt(this.x * this.x + this.z * this.z);
	}

	public double horizontalLengthSquared() {
		return this.x * this.x + this.z * this.z;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		else if (!(o instanceof Vec3d vec3d)) {
			return false;
		}
		else if (Double.compare(vec3d.x, this.x) != 0) {
			return false;
		}
		else {
			return Double.compare(vec3d.y, this.y) != 0 ? false : Double.compare(vec3d.z, this.z) == 0;
		}
	}

	@Override
	public int hashCode() {
		long l = Double.doubleToLongBits(this.x);
		int i = (int) (l ^ l >>> 32);
		l = Double.doubleToLongBits(this.y);
		i = 31 * i + (int) (l ^ l >>> 32);
		l = Double.doubleToLongBits(this.z);
		return 31 * i + (int) (l ^ l >>> 32);
	}

	@Override
	public String toString() {
		return "(" + this.x + ", " + this.y + ", " + this.z + ")";
	}

	public Vec3d lerp(Vec3d to, double delta) {
		return new Vec3d(
				MathHelper.lerp(delta, this.x, to.x),
				MathHelper.lerp(delta, this.y, to.y),
				MathHelper.lerp(delta, this.z, to.z)
		);
	}

	public Vec3d rotateX(float angle) {
		float f = MathHelper.cos(angle);
		float g = MathHelper.sin(angle);
		double d = this.x;
		double e = this.y * f + this.z * g;
		double h = this.z * f - this.y * g;
		return new Vec3d(d, e, h);
	}

	public Vec3d rotateY(float angle) {
		float f = MathHelper.cos(angle);
		float g = MathHelper.sin(angle);
		double d = this.x * f + this.z * g;
		double e = this.y;
		double h = this.z * f - this.x * g;
		return new Vec3d(d, e, h);
	}

	public Vec3d rotateZ(float angle) {
		float f = MathHelper.cos(angle);
		float g = MathHelper.sin(angle);
		double d = this.x * f + this.y * g;
		double e = this.y * f - this.x * g;
		double h = this.z;
		return new Vec3d(d, e, h);
	}

	public Vec3d rotateYClockwise() {
		return new Vec3d(-this.z, this.y, this.x);
	}

	public static Vec3d fromPolar(Vec2f polar) {
		return fromPolar(polar.x, polar.y);
	}

	public static Vec3d fromPolar(float pitch, float yaw) {
		float f = MathHelper.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
		float g = MathHelper.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
		float h = -MathHelper.cos(-pitch * (float) (Math.PI / 180.0));
		float i = MathHelper.sin(-pitch * (float) (Math.PI / 180.0));
		return new Vec3d(g * h, i, f * h);
	}

	public Vec2f getYawAndPitch() {
		float f = (float) Math.atan2(-this.x, this.z) * (180.0F / (float) Math.PI);
		float
				g =
				(float) Math.asin(-this.y / Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z)) * (180.0F
						/ (float) Math.PI
				);
		return new Vec2f(g, f);
	}

	public Vec3d floorAlongAxes(EnumSet<Direction.Axis> axes) {
		double d = axes.contains(Direction.Axis.X) ? MathHelper.floor(this.x) : this.x;
		double e = axes.contains(Direction.Axis.Y) ? MathHelper.floor(this.y) : this.y;
		double f = axes.contains(Direction.Axis.Z) ? MathHelper.floor(this.z) : this.z;
		return new Vec3d(d, e, f);
	}

	public double getComponentAlongAxis(Direction.Axis axis) {
		return axis.choose(this.x, this.y, this.z);
	}

	public Vec3d withAxis(Direction.Axis axis, double value) {
		double d = axis == Direction.Axis.X ? value : this.x;
		double e = axis == Direction.Axis.Y ? value : this.y;
		double f = axis == Direction.Axis.Z ? value : this.z;
		return new Vec3d(d, e, f);
	}

	public Vec3d offset(Direction direction, double value) {
		Vec3i vec3i = direction.getVector();
		return new Vec3d(this.x + value * vec3i.getX(), this.y + value * vec3i.getY(), this.z + value * vec3i.getZ());
	}

	@Override
	public final double getX() {
		return this.x;
	}

	@Override
	public final double getY() {
		return this.y;
	}

	@Override
	public final double getZ() {
		return this.z;
	}

	public Vector3f toVector3f() {
		return new Vector3f((float) this.x, (float) this.y, (float) this.z);
	}

	public Vec3d projectOnto(Vec3d vec) {
		return vec.lengthSquared() == 0.0 ? vec
		                                  : vec.multiply(this.dotProduct(vec)).multiply(1.0 / vec.lengthSquared());
	}

	public static Vec3d transformLocalPos(Vec2f rotation, Vec3d vec) {
		float f = MathHelper.cos((rotation.y + 90.0F) * (float) (Math.PI / 180.0));
		float g = MathHelper.sin((rotation.y + 90.0F) * (float) (Math.PI / 180.0));
		float h = MathHelper.cos(-rotation.x * (float) (Math.PI / 180.0));
		float i = MathHelper.sin(-rotation.x * (float) (Math.PI / 180.0));
		float j = MathHelper.cos((-rotation.x + 90.0F) * (float) (Math.PI / 180.0));
		float k = MathHelper.sin((-rotation.x + 90.0F) * (float) (Math.PI / 180.0));
		Vec3d vec3d = new Vec3d(f * h, i, g * h);
		Vec3d vec3d2 = new Vec3d(f * j, k, g * j);
		Vec3d vec3d3 = vec3d.crossProduct(vec3d2).multiply(-1.0);
		double d = vec3d.x * vec.z + vec3d2.x * vec.y + vec3d3.x * vec.x;
		double e = vec3d.y * vec.z + vec3d2.y * vec.y + vec3d3.y * vec.x;
		double l = vec3d.z * vec.z + vec3d2.z * vec.y + vec3d3.z * vec.x;
		return new Vec3d(d, e, l);
	}

	public Vec3d transformLocalPos(Vec3d vec) {
		return transformLocalPos(this.getYawAndPitch(), vec);
	}

	public boolean isFinite() {
		return Double.isFinite(this.x) && Double.isFinite(this.y) && Double.isFinite(this.z);
	}
}
