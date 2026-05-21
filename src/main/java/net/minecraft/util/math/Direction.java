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

import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * {@code Direction}.
 */
public enum Direction implements StringIdentifiable {
	DOWN(0, 1, -1, "down", Direction.AxisDirection.NEGATIVE, Direction.Axis.Y, new Vec3i(0, -1, 0)),
	UP(1, 0, -1, "up", Direction.AxisDirection.POSITIVE, Direction.Axis.Y, new Vec3i(0, 1, 0)),
	NORTH(2, 3, 2, "north", Direction.AxisDirection.NEGATIVE, Direction.Axis.Z, new Vec3i(0, 0, -1)),
	SOUTH(3, 2, 0, "south", Direction.AxisDirection.POSITIVE, Direction.Axis.Z, new Vec3i(0, 0, 1)),
	WEST(4, 5, 1, "west", Direction.AxisDirection.NEGATIVE, Direction.Axis.X, new Vec3i(-1, 0, 0)),
	EAST(5, 4, 3, "east", Direction.AxisDirection.POSITIVE, Direction.Axis.X, new Vec3i(1, 0, 0));

	public static final StringIdentifiable.EnumCodec<Direction>
			CODEC =
			StringIdentifiable.createCodec(Direction::values);
	public static final Codec<Direction> VERTICAL_CODEC = CODEC.validate(Direction::validateVertical);
	public static final IntFunction<Direction> INDEX_TO_VALUE_FUNCTION = ValueLists.createIndexToValueFunction(
			Direction::getIndex, values(), ValueLists.OutOfBoundsHandling.WRAP
	);
	public static final PacketCodec<ByteBuf, Direction>
			PACKET_CODEC =
			PacketCodecs.indexed(INDEX_TO_VALUE_FUNCTION, Direction::getIndex);
	@Deprecated
	public static final Codec<Direction>
			INDEX_CODEC =
			Codec.BYTE.xmap(Direction::byIndex, direction -> (byte) direction.getIndex());
	@Deprecated
	public static final Codec<Direction> HORIZONTAL_QUARTER_TURNS_CODEC = Codec.BYTE
			.xmap(Direction::fromHorizontalQuarterTurns, direction -> (byte) direction.getHorizontalQuarterTurns());
	private static final ImmutableList<Direction.Axis>
			YXZ =
			ImmutableList.of(Direction.Axis.Y, Direction.Axis.X, Direction.Axis.Z);
	private static final ImmutableList<Direction.Axis>
			YZX =
			ImmutableList.of(Direction.Axis.Y, Direction.Axis.Z, Direction.Axis.X);
	private final int index;
	private final int oppositeIndex;
	private final int horizontalQuarterTurns;
	private final String id;
	private final Direction.Axis axis;
	private final Direction.AxisDirection direction;
	private final Vec3i vec3i;
	private final Vec3d doubleVector;
	private final Vector3fc floatVector;
	private static final Direction[] ALL = values();
	private static final Direction[]
			VALUES =
			Arrays.stream(ALL).sorted(Comparator.comparingInt(direction -> direction.index)).toArray(Direction[]::new);
	private static final Direction[] HORIZONTAL = Arrays.stream(ALL)
	                                                    .filter(direction -> direction.getAxis().isHorizontal())
	                                                    .sorted(Comparator.comparingInt(direction -> direction.horizontalQuarterTurns))
	                                                    .toArray(Direction[]::new);

	private Direction(
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
		this.vec3i = vector;
		this.doubleVector = Vec3d.of(vector);
		this.floatVector = new Vector3f(vector.getX(), vector.getY(), vector.getZ());
	}

	public static Direction[] getEntityFacingOrder(Entity entity) {
		float f = entity.getPitch(1.0F) * (float) (Math.PI / 180.0);
		float g = -entity.getYaw(1.0F) * (float) (Math.PI / 180.0);
		float h = MathHelper.sin(f);
		float i = MathHelper.cos(f);
		float j = MathHelper.sin(g);
		float k = MathHelper.cos(g);
		boolean bl = j > 0.0F;
		boolean bl2 = h < 0.0F;
		boolean bl3 = k > 0.0F;
		float l = bl ? j : -j;
		float m = bl2 ? -h : h;
		float n = bl3 ? k : -k;
		float o = l * i;
		float p = n * i;
		Direction direction = bl ? EAST : WEST;
		Direction direction2 = bl2 ? UP : DOWN;
		Direction direction3 = bl3 ? SOUTH : NORTH;
		if (l > n) {
			if (m > o) {
				return listClosest(direction2, direction, direction3);
			}
			else {
				return p > m ? listClosest(direction, direction3, direction2)
				             : listClosest(direction, direction2, direction3);
			}
		}
		else if (m > p) {
			return listClosest(direction2, direction3, direction);
		}
		else {
			return o > m ? listClosest(direction3, direction, direction2)
			             : listClosest(direction3, direction2, direction);
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
		return this.index;
	}

	public int getHorizontalQuarterTurns() {
		return this.horizontalQuarterTurns;
	}

	public Direction.AxisDirection getDirection() {
		return this.direction;
	}

	public static Direction getLookDirectionForAxis(Entity entity, Direction.Axis axis) {
		return switch (axis) {
			case X -> EAST.pointsTo(entity.getYaw(1.0F)) ? EAST : WEST;
			case Y -> entity.getPitch(1.0F) < 0.0F ? UP : DOWN;
			case Z -> SOUTH.pointsTo(entity.getYaw(1.0F)) ? SOUTH : NORTH;
		};
	}

	public Direction getOpposite() {
		return byIndex(this.oppositeIndex);
	}

	public Direction rotateClockwise(Direction.Axis axis) {
		return switch (axis) {
			case X -> this != WEST && this != EAST ? this.rotateXClockwise() : this;
			case Y -> this != UP && this != DOWN ? this.rotateYClockwise() : this;
			case Z -> this != NORTH && this != SOUTH ? this.rotateZClockwise() : this;
		};
	}

	public Direction rotateCounterclockwise(Direction.Axis axis) {
		return switch (axis) {
			case X -> this != WEST && this != EAST ? this.rotateXCounterclockwise() : this;
			case Y -> this != UP && this != DOWN ? this.rotateYCounterclockwise() : this;
			case Z -> this != NORTH && this != SOUTH ? this.rotateZCounterclockwise() : this;
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
			default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
			case WEST -> UP;
			case EAST -> DOWN;
		};
	}

	private Direction rotateZCounterclockwise() {
		return switch (this) {
			case DOWN -> EAST;
			case UP -> WEST;
			default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
			case WEST -> DOWN;
			case EAST -> UP;
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
		return this.vec3i.getX();
	}

	public int getOffsetY() {
		return this.vec3i.getY();
	}

	public int getOffsetZ() {
		return this.vec3i.getZ();
	}

	public Vector3f getUnitVector() {
		return new Vector3f(this.floatVector);
	}

	public String getId() {
		return this.id;
	}

	public Direction.Axis getAxis() {
		return this.axis;
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
		return (this.horizontalQuarterTurns & 3) * 90;
	}

	public static Direction random(Random random) {
		return Util.getRandom(ALL, random);
	}

	public static Direction getFacing(double x, double y, double z) {
		return getFacing((float) x, (float) y, (float) z);
	}

	public static Direction getFacing(float x, float y, float z) {
		Direction direction = NORTH;
		float f = Float.MIN_VALUE;

		for (Direction direction2 : ALL) {
			float g = x * direction2.vec3i.getX() + y * direction2.vec3i.getY() + z * direction2.vec3i.getZ();
			if (g > f) {
				f = g;
				direction = direction2;
			}
		}

		return direction;
	}

	public static Direction getFacing(Vec3d vec) {
		return getFacing(vec.x, vec.y, vec.z);
	}

	@Contract("_,_,_,!null->!null;_,_,_,_->_")
	public static @Nullable Direction fromVector(int x, int y, int z, @Nullable Direction fallback) {
		int i = Math.abs(x);
		int j = Math.abs(y);
		int k = Math.abs(z);
		if (i > k && i > j) {
			return x < 0 ? WEST : EAST;
		}
		else if (k > i && k > j) {
			return z < 0 ? NORTH : SOUTH;
		}
		else if (j > i && j > k) {
			return y < 0 ? DOWN : UP;
		}
		else {
			return fallback;
		}
	}

	@Contract("_,!null->!null;_,_->_")
	public static @Nullable Direction fromVector(Vec3i vec, @Nullable Direction fallback) {
		return fromVector(vec.getX(), vec.getY(), vec.getZ(), fallback);
	}

	@Override
	public String toString() {
		return this.id;
	}

	@Override
	public String asString() {
		return this.id;
	}

	private static DataResult<Direction> validateVertical(Direction direction) {
		return direction.getAxis().isVertical() ? DataResult.success(direction)
		                                        : DataResult.error(() -> "Expected a vertical direction");
	}

	public static Direction get(Direction.AxisDirection direction, Direction.Axis axis) {
		for (Direction direction2 : ALL) {
			if (direction2.getDirection() == direction && direction2.getAxis() == axis) {
				return direction2;
			}
		}

		throw new IllegalArgumentException("No such direction: " + direction + " " + axis);
	}

	public static ImmutableList<Direction.Axis> getCollisionOrder(Vec3d vec3d) {
		return Math.abs(vec3d.x) < Math.abs(vec3d.z) ? YZX : YXZ;
	}

	public Vec3i getVector() {
		return this.vec3i;
	}

	public Vec3d getDoubleVector() {
		return this.doubleVector;
	}

	public Vector3fc getFloatVector() {
		return this.floatVector;
	}

	public boolean pointsTo(float yaw) {
		float f = yaw * (float) (Math.PI / 180.0);
		float g = -MathHelper.sin(f);
		float h = MathHelper.cos(f);
		return this.vec3i.getX() * g + this.vec3i.getZ() * h > 0.0F;
	}

	/**
	 * {@code Axis}.
	 */
	public static enum Axis implements StringIdentifiable, Predicate<Direction> {
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
		public static final StringIdentifiable.EnumCodec<Direction.Axis>
				CODEC =
				StringIdentifiable.createCodec(Direction.Axis::values);
		private final String id;

		Axis(final String id) {
			this.id = id;
		}

		public static Direction.@Nullable Axis fromId(String id) {
			return CODEC.byId(id);
		}

		public String getId() {
			return this.id;
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
			return new Direction[]{this.getPositiveDirection(), this.getNegativeDirection()};
		}

		@Override
		public String toString() {
			return this.id;
		}

		public static Direction.Axis pickRandomAxis(Random random) {
			return Util.getRandom(VALUES, random);
		}

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
			return this.id;
		}

		public abstract int choose(int x, int y, int z);

		public abstract double choose(double x, double y, double z);

		public abstract boolean choose(boolean x, boolean y, boolean z);
	}

	/**
	 * {@code AxisDirection}.
	 */
	public static enum AxisDirection {
		POSITIVE(1, "Towards positive"),
		NEGATIVE(-1, "Towards negative");

		private final int offset;
		private final String description;

		private AxisDirection(final int offset, final String description) {
			this.offset = offset;
			this.description = description;
		}

		public int offset() {
			return this.offset;
		}

		public String getDescription() {
			return this.description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public Direction.AxisDirection getOpposite() {
			return this == POSITIVE ? NEGATIVE : POSITIVE;
		}
	}

	/**
	 * {@code Type}.
	 */
	public static enum Type implements Iterable<Direction>, Predicate<Direction> {
		HORIZONTAL(
				new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST},
				new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}
		),
		VERTICAL(new Direction[]{Direction.UP, Direction.DOWN}, new Direction.Axis[]{Direction.Axis.Y});

		private final Direction[] facingArray;
		private final Direction.Axis[] axisArray;

		private Type(final Direction[] facingArray, final Direction.Axis[] axisArray) {
			this.facingArray = facingArray;
			this.axisArray = axisArray;
		}

		public Direction random(Random random) {
			return Util.getRandom(this.facingArray, random);
		}

		public Direction.Axis randomAxis(Random random) {
			return Util.getRandom(this.axisArray, random);
		}

		public boolean test(@Nullable Direction direction) {
			return direction != null && direction.getAxis().getType() == this;
		}

		@Override
		public Iterator<Direction> iterator() {
			return Iterators.forArray(this.facingArray);
		}

		public Stream<Direction> stream() {
			return Arrays.stream(this.facingArray);
		}

		public List<Direction> getShuffled(Random random) {
			return Util.copyShuffled(this.facingArray, random);
		}

		public int getFacingCount() {
			return this.facingArray.length;
		}
	}
}
