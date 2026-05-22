package net.minecraft.util.math;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Contract;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Шесть основных направлений в 3D-пространстве (стороны куба).
 * Каждое направление хранит индекс, ось, вектор смещения и вспомогательные данные
 * для быстрого поворота и трансформации.
 */
public enum Direction implements StringIdentifiable {
	DOWN(0, 1, -1, "down", Direction.AxisDirection.NEGATIVE, Direction.Axis.Y, new Vec3i(0, -1, 0)),
	UP(1, 0, -1, "up", Direction.AxisDirection.POSITIVE, Direction.Axis.Y, new Vec3i(0, 1, 0)),
	NORTH(2, 3, 2, "north", Direction.AxisDirection.NEGATIVE, Direction.Axis.Z, new Vec3i(0, 0, -1)),
	SOUTH(3, 2, 0, "south", Direction.AxisDirection.POSITIVE, Direction.Axis.Z, new Vec3i(0, 0, 1)),
	WEST(4, 5, 1, "west", Direction.AxisDirection.NEGATIVE, Direction.Axis.X, new Vec3i(-1, 0, 0)),
	EAST(5, 4, 3, "east", Direction.AxisDirection.POSITIVE, Direction.Axis.X, new Vec3i(1, 0, 0));

	public static final StringIdentifiable.EnumCodec<Direction> CODEC = StringIdentifiable.createCodec(Direction::values);
	public static final Codec<Direction> VERTICAL_CODEC = CODEC.validate(Direction::validateVertical);
	public static final IntFunction<Direction> INDEX_TO_VALUE_FUNCTION = ValueLists.createIndexToValueFunction(
			Direction::getIndex, values(), ValueLists.OutOfBoundsHandling.WRAP
	);
	public static final PacketCodec<ByteBuf, Direction> PACKET_CODEC = PacketCodecs.indexed(
			INDEX_TO_VALUE_FUNCTION, Direction::getIndex
	);

	@Deprecated
	public static final Codec<Direction> INDEX_CODEC = Codec.BYTE.xmap(
			Direction::byIndex, direction -> (byte) direction.getIndex()
	);

	@Deprecated
	public static final Codec<Direction> HORIZONTAL_QUARTER_TURNS_CODEC = Codec.BYTE.xmap(
			Direction::fromHorizontalQuarterTurns,
			direction -> (byte) direction.getHorizontalQuarterTurns()
	);

	private static final ImmutableList<Direction.Axis> YXZ = ImmutableList.of(
			Direction.Axis.Y, Direction.Axis.X, Direction.Axis.Z
	);
	private static final ImmutableList<Direction.Axis> YZX = ImmutableList.of(
			Direction.Axis.Y, Direction.Axis.Z, Direction.Axis.X
	);
	private static final Direction[] ALL = values();
	private static final Direction[] VALUES = Arrays.stream(ALL)
			.sorted(Comparator.comparingInt(direction -> direction.index))
			.toArray(Direction[]::new);
	private static final Direction[] HORIZONTAL = Arrays.stream(ALL)
			.filter(direction -> direction.getAxis().isHorizontal())
			.sorted(Comparator.comparingInt(direction -> direction.horizontalQuarterTurns))
			.toArray(Direction[]::new);

	private final int index;
	private final int oppositeIndex;
	private final int horizontalQuarterTurns;
	private final String id;
	private final Direction.Axis axis;
	private final Direction.AxisDirection direction;
	private final Vec3i vec3i;
	private final Vec3d doubleVector;
	private final Vector3fc floatVector;

	Direction(
			final int index,
			final int oppositeIndex,
			final int horizontalQuarterTurns,
			final String id,
			final Direction.AxisDirection direction,
			final Direction.Axis axis,
			final Vec3i vector
	) {
		this.index = index;
		this.horizontalQuarterTurns = horizontalQuarterTurns;
		this.oppositeIndex = oppositeIndex;
		this.id = id;
		this.axis = axis;
		this.direction = direction;
		vec3i = vector;
		doubleVector = Vec3d.of(vector);
		floatVector = new Vector3f(vector.getX(), vector.getY(), vector.getZ());
	}

	/**
	 * Возвращает массив направлений, упорядоченных от наиболее вероятного взгляда сущности
	 * к наименее вероятному, на основе угла обзора (pitch/yaw).
	 */
	public static Direction[] getEntityFacingOrder(Entity entity) {
		float pitch = entity.getPitch(1.0F) * (float) (Math.PI / 180.0);
		float yaw = -entity.getYaw(1.0F) * (float) (Math.PI / 180.0);
		float sinPitch = MathHelper.sin(pitch);
		float cosPitch = MathHelper.cos(pitch);
		float sinYaw = MathHelper.sin(yaw);
		float cosYaw = MathHelper.cos(yaw);
		boolean facingEast = sinYaw > 0.0F;
		boolean facingUp = sinPitch < 0.0F;
		boolean facingSouth = cosYaw > 0.0F;
		float absYaw = facingEast ? sinYaw : -sinYaw;
		float absPitch = facingUp ? -sinPitch : sinPitch;
		float absYawCos = facingSouth ? cosYaw : -cosYaw;
		float yawComponent = absYaw * cosPitch;
		float cosYawComponent = absYawCos * cosPitch;
		Direction directionX = facingEast ? EAST : WEST;
		Direction directionY = facingUp ? UP : DOWN;
		Direction directionZ = facingSouth ? SOUTH : NORTH;

		if (absYaw > absYawCos) {
			return absPitch > yawComponent
					? listClosest(directionY, directionX, directionZ)
					: absYawCos > absPitch
							? listClosest(directionX, directionZ, directionY)
							: listClosest(directionX, directionY, directionZ);
		} else if (absPitch > cosYawComponent) {
			return listClosest(directionY, directionZ, directionX);
		} else {
			return absYaw > absPitch
					? listClosest(directionZ, directionX, directionY)
					: listClosest(directionZ, directionY, directionX);
		}
	}

	private static Direction[] listClosest(Direction first, Direction second, Direction third) {
		return new Direction[]{first, second, third, third.getOpposite(), second.getOpposite(), first.getOpposite()};
	}

	public static Direction transform(Matrix4fc matrix, Direction direction) {
		Vector3f vector3f = matrix.transformDirection(direction.floatVector, new Vector3f());
		return getFacing(vector3f.x(), vector3f.y(), vector3f.z());
	}

	public static Collection<Direction> shuffle(Random random) {
		return Util.copyShuffled(values(), random);
	}

	public static Stream<Direction> stream() {
		return Stream.of(ALL);
	}

	public static float getHorizontalDegreesOrThrow(Direction direction) {
		return switch (direction) {
			case NORTH -> 180.0F;
			case SOUTH -> 0.0F;
			case WEST -> 90.0F;
			case EAST -> -90.0F;
			default -> throw new IllegalStateException("No y-Rot for vertical axis: " + direction);
		};
	}

	public Quaternionf getRotationQuaternion() {
		return switch (this) {
			case DOWN -> new Quaternionf().rotationX((float) Math.PI);
			case UP -> new Quaternionf();
			case NORTH -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) Math.PI);
			case SOUTH -> new Quaternionf().rotationX((float) (Math.PI / 2));
			case WEST -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) (Math.PI / 2));
			case EAST -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) (-Math.PI / 2));
		};
	}

	public int getIndex() {
		return index;
	}

	public int getHorizontalQuarterTurns() {
		return horizontalQuarterTurns;
	}

	public Direction.AxisDirection getDirection() {
		return direction;
	}

	public static Direction getLookDirectionForAxis(Entity entity, Direction.Axis axis) {
		return switch (axis) {
			case X -> EAST.pointsTo(entity.getYaw(1.0F)) ? EAST : WEST;
			case Y -> entity.getPitch(1.0F) < 0.0F ? UP : DOWN;
			case Z -> SOUTH.pointsTo(entity.getYaw(1.0F)) ? SOUTH : NORTH;
		};
	}

	public Direction getOpposite() {
		return byIndex(oppositeIndex);
	}

	public Direction rotateClockwise(Direction.Axis axis) {
		return switch (axis) {
			case X -> this != WEST && this != EAST ? rotateXClockwise() : this;
			case Y -> this != UP && this != DOWN ? rotateYClockwise() : this;
			case Z -> this != NORTH && this != SOUTH ? rotateZClockwise() : this;
		};
	}

	public Direction rotateCounterclockwise(Direction.Axis axis) {
		return switch (axis) {
			case X -> this != WEST && this != EAST ? rotateXCounterclockwise() : this;
			case Y -> this != UP && this != DOWN ? rotateYCounterclockwise() : this;
			case Z -> this != NORTH && this != SOUTH ? rotateZCounterclockwise() : this;
		};
	}

	public Direction rotateYClockwise() {
		return switch (this) {
			case NORTH -> EAST;
			case SOUTH -> WEST;
			case WEST -> NORTH;
			case EAST -> SOUTH;
			default -> throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
		};
	}

	private Direction rotateXClockwise() {
		return switch (this) {
			case DOWN -> SOUTH;
			case UP -> NORTH;
			case NORTH -> DOWN;
			case SOUTH -> UP;
			default -> throw new IllegalStateException("Unable to get X-rotated facing of " + this);
		};
	}

	private Direction rotateXCounterclockwise() {
		return switch (this) {
			case DOWN -> NORTH;
			case UP -> SOUTH;
			case NORTH -> UP;
			case SOUTH -> DOWN;
			default -> throw new IllegalStateException("Unable to get X-rotated facing of " + this);
		};
	}

	private Direction rotateZClockwise() {
		return switch (this) {
			case DOWN -> WEST;
			case UP -> EAST;
			case WEST -> UP;
			case EAST -> DOWN;
			default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
		};
	}

	private Direction rotateZCounterclockwise() {
		return switch (this) {
			case DOWN -> EAST;
			case UP -> WEST;
			case WEST -> DOWN;
			case EAST -> UP;
			default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
		};
	}

	public Direction rotateYCounterclockwise() {
		return switch (this) {
			case NORTH -> WEST;
			case SOUTH -> EAST;
			case WEST -> SOUTH;
			case EAST -> NORTH;
			default -> throw new IllegalStateException("Unable to get CCW facing of " + this);
		};
	}

	public int getOffsetX() {
		return vec3i.getX();
	}

	public int getOffsetY() {
		return vec3i.getY();
	}

	public int getOffsetZ() {
		return vec3i.getZ();
	}

	public Vector3f getUnitVector() {
		return new Vector3f(floatVector);
	}

	public String getId() {
		return id;
	}

	public Direction.Axis getAxis() {
		return axis;
	}

	public static @Nullable Direction byId(String id) {
		return CODEC.byId(id);
	}

	public static Direction byIndex(int index) {
		return VALUES[MathHelper.abs(index % VALUES.length)];
	}

	public static Direction fromHorizontalQuarterTurns(int quarterTurns) {
		return HORIZONTAL[MathHelper.abs(quarterTurns % HORIZONTAL.length)];
	}

	public static Direction fromHorizontalDegrees(double angle) {
		return fromHorizontalQuarterTurns(MathHelper.floor(angle / 90.0 + 0.5) & 3);
	}

	public static Direction from(Direction.Axis axis, Direction.AxisDirection direction) {
		return switch (axis) {
			case X -> direction == Direction.AxisDirection.POSITIVE ? EAST : WEST;
			case Y -> direction == Direction.AxisDirection.POSITIVE ? UP : DOWN;
			case Z -> direction == Direction.AxisDirection.POSITIVE ? SOUTH : NORTH;
		};
	}

	public float getPositiveHorizontalDegrees() {
		return (horizontalQuarterTurns & 3) * 90;
	}

	public static Direction random(Random random) {
		return Util.getRandom(ALL, random);
	}

	public static Direction getFacing(double x, double y, double z) {
		return getFacing((float) x, (float) y, (float) z);
	}

	public static Direction getFacing(float x, float y, float z) {
		Direction best = NORTH;
		float maxDot = Float.MIN_VALUE;

		for (Direction candidate : ALL) {
			float dot = x * candidate.vec3i.getX() + y * candidate.vec3i.getY() + z * candidate.vec3i.getZ();

			if (dot > maxDot) {
				maxDot = dot;
				best = candidate;
			}
		}

		return best;
	}

	public static Direction getFacing(Vec3d vec) {
		return getFacing(vec.x, vec.y, vec.z);
	}

	@Contract("_,_,_,!null->!null;_,_,_,_->_")
	public static @Nullable Direction fromVector(int x, int y, int z, @Nullable Direction fallback) {
		int absX = Math.abs(x);
		int absY = Math.abs(y);
		int absZ = Math.abs(z);

		if (absX > absZ && absX > absY) {
			return x < 0 ? WEST : EAST;
		}

		if (absZ > absX && absZ > absY) {
			return z < 0 ? NORTH : SOUTH;
		}

		if (absY > absX && absY > absZ) {
			return y < 0 ? DOWN : UP;
		}

		return fallback;
	}

	@Contract("_,!null->!null;_,_->_")
	public static @Nullable Direction fromVector(Vec3i vec, @Nullable Direction fallback) {
		return fromVector(vec.getX(), vec.getY(), vec.getZ(), fallback);
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public String asString() {
		return id;
	}

	private static DataResult<Direction> validateVertical(Direction direction) {
		return direction.getAxis().isVertical()
				? DataResult.success(direction)
				: DataResult.error(() -> "Expected a vertical direction");
	}

	public static Direction get(Direction.AxisDirection direction, Direction.Axis axis) {
		for (Direction candidate : ALL) {
			if (candidate.getDirection() == direction && candidate.getAxis() == axis) {
				return candidate;
			}
		}

		throw new IllegalArgumentException("No such direction: " + direction + " " + axis);
	}

	public static ImmutableList<Direction.Axis> getCollisionOrder(Vec3d vec3d) {
		return Math.abs(vec3d.x) < Math.abs(vec3d.z) ? YZX : YXZ;
	}

	public Vec3i getVector() {
		return vec3i;
	}

	public Vec3d getDoubleVector() {
		return doubleVector;
	}

	public Vector3fc getFloatVector() {
		return floatVector;
	}

	public boolean pointsTo(float yaw) {
		float radians = yaw * (float) (Math.PI / 180.0);
		float sinYaw = -MathHelper.sin(radians);
		float cosYaw = MathHelper.cos(radians);
		return vec3i.getX() * sinYaw + vec3i.getZ() * cosYaw > 0.0F;
	}

	public enum Axis implements StringIdentifiable, Predicate<Direction> {
		X("x") {
			@Override
			public int choose(int x, int y, int z) {
				return x;
			}

			@Override
			public boolean choose(boolean x, boolean y, boolean z) {
				return x;
			}

			@Override
			public double choose(double x, double y, double z) {
				return x;
			}

			@Override
			public Direction getPositiveDirection() {
				return Direction.EAST;
			}

			@Override
			public Direction getNegativeDirection() {
				return Direction.WEST;
			}
		},
		Y("y") {
			@Override
			public int choose(int x, int y, int z) {
				return y;
			}

			@Override
			public double choose(double x, double y, double z) {
				return y;
			}

			@Override
			public boolean choose(boolean x, boolean y, boolean z) {
				return y;
			}

			@Override
			public Direction getPositiveDirection() {
				return Direction.UP;
			}

			@Override
			public Direction getNegativeDirection() {
				return Direction.DOWN;
			}
		},
		Z("z") {
			@Override
			public int choose(int x, int y, int z) {
				return z;
			}

			@Override
			public double choose(double x, double y, double z) {
				return z;
			}

			@Override
			public boolean choose(boolean x, boolean y, boolean z) {
				return z;
			}

			@Override
			public Direction getPositiveDirection() {
				return Direction.SOUTH;
			}

			@Override
			public Direction getNegativeDirection() {
				return Direction.NORTH;
			}
		};

		public static final Direction.Axis[] VALUES = values();
		public static final StringIdentifiable.EnumCodec<Direction.Axis> CODEC = StringIdentifiable.createCodec(
				Direction.Axis::values
		);

		private final String id;

		Axis(final String id) {
			this.id = id;
		}

		public static Direction.@Nullable Axis fromId(String id) {
			return CODEC.byId(id);
		}

		public String getId() {
			return id;
		}

		public boolean isVertical() {
			return this == Y;
		}

		public boolean isHorizontal() {
			return this == X || this == Z;
		}

		public abstract Direction getPositiveDirection();

		public abstract Direction getNegativeDirection();

		public Direction[] getDirections() {
			return new Direction[]{getPositiveDirection(), getNegativeDirection()};
		}

		@Override
		public String toString() {
			return id;
		}

		public static Direction.Axis pickRandomAxis(Random random) {
			return Util.getRandom(VALUES, random);
		}

		@Override
		public boolean test(@Nullable Direction direction) {
			return direction != null && direction.getAxis() == this;
		}

		public Direction.Type getType() {
			return switch (this) {
				case X, Z -> Direction.Type.HORIZONTAL;
				case Y -> Direction.Type.VERTICAL;
			};
		}

		@Override
		public String asString() {
			return id;
		}

		public abstract int choose(int x, int y, int z);

		public abstract double choose(double x, double y, double z);

		public abstract boolean choose(boolean x, boolean y, boolean z);
	}

	public enum AxisDirection {
		POSITIVE(1, "Towards positive"),
		NEGATIVE(-1, "Towards negative");

		private final int offset;
		private final String description;

		AxisDirection(final int offset, final String description) {
			this.offset = offset;
			this.description = description;
		}

		public int offset() {
			return offset;
		}

		public String getDescription() {
			return description;
		}

		@Override
		public String toString() {
			return description;
		}

		public Direction.AxisDirection getOpposite() {
			return this == POSITIVE ? NEGATIVE : POSITIVE;
		}
	}

	public enum Type implements Iterable<Direction>, Predicate<Direction> {
		HORIZONTAL(
				new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST},
				new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}
		),
		VERTICAL(
				new Direction[]{Direction.UP, Direction.DOWN},
				new Direction.Axis[]{Direction.Axis.Y}
		);

		private final Direction[] facingArray;
		private final Direction.Axis[] axisArray;

		Type(final Direction[] facingArray, final Direction.Axis[] axisArray) {
			this.facingArray = facingArray;
			this.axisArray = axisArray;
		}

		public Direction random(Random random) {
			return Util.getRandom(facingArray, random);
		}

		public Direction.Axis randomAxis(Random random) {
			return Util.getRandom(axisArray, random);
		}

		@Override
		public boolean test(@Nullable Direction direction) {
			return direction != null && direction.getAxis().getType() == this;
		}

		@Override
		public Iterator<Direction> iterator() {
			return Iterators.forArray(facingArray);
		}

		public Stream<Direction> stream() {
			return Arrays.stream(facingArray);
		}

		public List<Direction> getShuffled(Random random) {
			return Util.copyShuffled(facingArray, random);
		}

		public int getFacingCount() {
			return facingArray.length;
		}
	}
}
