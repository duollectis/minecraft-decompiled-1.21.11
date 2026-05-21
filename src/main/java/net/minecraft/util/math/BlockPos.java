package net.minecraft.util.math;

import com.google.common.collect.AbstractIterator;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Unmodifiable
/**
 * {@code BlockPos}.
 */
public class BlockPos extends Vec3i {

	public static final Codec<BlockPos> CODEC = Codec.INT_STREAM
			.comapFlatMap(
					stream -> Util
							.decodeFixedLengthArray(stream, 3)
							.map(values -> new BlockPos(values[0], values[1], values[2])),
					pos -> IntStream.of(pos.getX(), pos.getY(), pos.getZ())
			)
			.stable();
	public static final PacketCodec<ByteBuf, BlockPos> PACKET_CODEC = new PacketCodec<ByteBuf, BlockPos>() {
		/**
		 * Decode.
		 *
		 * @param byteBuf byte buf
		 *
		 * @return BlockPos — результат операции
		 */
		public BlockPos decode(ByteBuf byteBuf) {
			return PacketByteBuf.readBlockPos(byteBuf);
		}

		/**
		 * Encode.
		 *
		 * @param byteBuf byte buf
		 * @param blockPos block pos
		 */
		public void encode(ByteBuf byteBuf, BlockPos blockPos) {
			PacketByteBuf.writeBlockPos(byteBuf, blockPos);
		}
	};
	public static final BlockPos ORIGIN = new BlockPos(0, 0, 0);
	public static final int
			SIZE_BITS_XZ =
			1 + MathHelper.floorLog2(MathHelper.smallestEncompassingPowerOfTwo(30000000));
	public static final int SIZE_BITS_Y = 64 - 2 * SIZE_BITS_XZ;
	private static final long BITS_X = (1L << SIZE_BITS_XZ) - 1L;
	private static final long BITS_Y = (1L << SIZE_BITS_Y) - 1L;
	private static final long BITS_Z = (1L << SIZE_BITS_XZ) - 1L;
	private static final int PACKED_X_OFFSET = 0;
	private static final int BIT_SHIFT_Z = SIZE_BITS_Y;
	private static final int BIT_SHIFT_X = SIZE_BITS_Y + SIZE_BITS_XZ;
	public static final int MAX_XZ = (1 << SIZE_BITS_XZ) / 2 - 1;

	public BlockPos(int i, int j, int k) {
		super(i, j, k);
	}

	public BlockPos(Vec3i pos) {
		this(pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * Offset.
	 *
	 * @param value value
	 * @param direction direction
	 *
	 * @return long — результат операции
	 */
	public static long offset(long value, Direction direction) {
		return add(value, direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
	}

	/**
	 * Add.
	 *
	 * @param value value
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return long — результат операции
	 */
	public static long add(long value, int x, int y, int z) {
		return asLong(unpackLongX(value) + x, unpackLongY(value) + y, unpackLongZ(value) + z);
	}

	/**
	 * Unpack long x.
	 *
	 * @param packedPos packed pos
	 *
	 * @return int — результат операции
	 */
	public static int unpackLongX(long packedPos) {
		return (int) (packedPos << 64 - BIT_SHIFT_X - SIZE_BITS_XZ >> 64 - SIZE_BITS_XZ);
	}

	/**
	 * Unpack long y.
	 *
	 * @param packedPos packed pos
	 *
	 * @return int — результат операции
	 */
	public static int unpackLongY(long packedPos) {
		return (int) (packedPos << 64 - SIZE_BITS_Y >> 64 - SIZE_BITS_Y);
	}

	/**
	 * Unpack long z.
	 *
	 * @param packedPos packed pos
	 *
	 * @return int — результат операции
	 */
	public static int unpackLongZ(long packedPos) {
		return (int) (packedPos << 64 - BIT_SHIFT_Z - SIZE_BITS_XZ >> 64 - SIZE_BITS_XZ);
	}

	/**
	 * From long.
	 *
	 * @param packedPos packed pos
	 *
	 * @return BlockPos — результат операции
	 */
	public static BlockPos fromLong(long packedPos) {
		return new BlockPos(unpackLongX(packedPos), unpackLongY(packedPos), unpackLongZ(packedPos));
	}

	/**
	 * Of floored.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return BlockPos — результат операции
	 */
	public static BlockPos ofFloored(double x, double y, double z) {
		return new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
	}

	/**
	 * Of floored.
	 *
	 * @param pos pos
	 *
	 * @return BlockPos — результат операции
	 */
	public static BlockPos ofFloored(Position pos) {
		return ofFloored(pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * Min.
	 *
	 * @param a a
	 * @param b b
	 *
	 * @return BlockPos — результат операции
	 */
	public static BlockPos min(BlockPos a, BlockPos b) {
		return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
	}

	/**
	 * Max.
	 *
	 * @param a a
	 * @param b b
	 *
	 * @return BlockPos — результат операции
	 */
	public static BlockPos max(BlockPos a, BlockPos b) {
		return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
	}

	/**
	 * As long.
	 *
	 * @return long — результат операции
	 */
	public long asLong() {
		return asLong(this.getX(), this.getY(), this.getZ());
	}

	/**
	 * As long.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return long — результат операции
	 */
	public static long asLong(int x, int y, int z) {
		long l = 0L;
		l |= (x & BITS_X) << BIT_SHIFT_X;
		l |= (y & BITS_Y) << 0;
		return l | (z & BITS_Z) << BIT_SHIFT_Z;
	}

	/**
	 * Удаляет chunk section local y.
	 *
	 * @param y y
	 *
	 * @return long — результат операции
	 */
	public static long removeChunkSectionLocalY(long y) {
		return y & -16L;
	}

	/**
	 * Add.
	 *
	 * @param i i
	 * @param j j
	 * @param k k
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos add(int i, int j, int k) {
		return i == 0 && j == 0 && k == 0 ? this : new BlockPos(this.getX() + i, this.getY() + j, this.getZ() + k);
	}

	/**
	 * To center pos.
	 *
	 * @return Vec3d — результат операции
	 */
	public Vec3d toCenterPos() {
		return Vec3d.ofCenter(this);
	}

	/**
	 * To bottom center pos.
	 *
	 * @return Vec3d — результат операции
	 */
	public Vec3d toBottomCenterPos() {
		return Vec3d.ofBottomCenter(this);
	}

	@Contract(pure = true)
	/**
	 * Add.
	 *
	 * @param vec3i vec3i
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos add(Vec3i vec3i) {
		return this.add(vec3i.getX(), vec3i.getY(), vec3i.getZ());
	}

	/**
	 * Subtract.
	 *
	 * @param vec3i vec3i
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos subtract(Vec3i vec3i) {
		return this.add(-vec3i.getX(), -vec3i.getY(), -vec3i.getZ());
	}

	/**
	 * Multiply.
	 *
	 * @param i i
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos multiply(int i) {
		if (i == 1) {
			return this;
		}
		else {
			return i == 0 ? ORIGIN : new BlockPos(this.getX() * i, this.getY() * i, this.getZ() * i);
		}
	}

	/**
	 * Up.
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos up() {
		return this.offset(Direction.UP);
	}

	/**
	 * Up.
	 *
	 * @param distance distance
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos up(int distance) {
		return this.offset(Direction.UP, distance);
	}

	/**
	 * Down.
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos down() {
		return this.offset(Direction.DOWN);
	}

	/**
	 * Down.
	 *
	 * @param i i
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos down(int i) {
		return this.offset(Direction.DOWN, i);
	}

	/**
	 * North.
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos north() {
		return this.offset(Direction.NORTH);
	}

	/**
	 * North.
	 *
	 * @param distance distance
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos north(int distance) {
		return this.offset(Direction.NORTH, distance);
	}

	/**
	 * South.
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos south() {
		return this.offset(Direction.SOUTH);
	}

	/**
	 * South.
	 *
	 * @param distance distance
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos south(int distance) {
		return this.offset(Direction.SOUTH, distance);
	}

	/**
	 * West.
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos west() {
		return this.offset(Direction.WEST);
	}

	/**
	 * West.
	 *
	 * @param distance distance
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos west(int distance) {
		return this.offset(Direction.WEST, distance);
	}

	/**
	 * East.
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos east() {
		return this.offset(Direction.EAST);
	}

	/**
	 * East.
	 *
	 * @param distance distance
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos east(int distance) {
		return this.offset(Direction.EAST, distance);
	}

	/**
	 * Offset.
	 *
	 * @param direction direction
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos offset(Direction direction) {
		return new BlockPos(
				this.getX() + direction.getOffsetX(),
				this.getY() + direction.getOffsetY(),
				this.getZ() + direction.getOffsetZ()
		);
	}

	/**
	 * Offset.
	 *
	 * @param direction direction
	 * @param i i
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos offset(Direction direction, int i) {
		return i == 0
		       ? this
		       : new BlockPos(
				       this.getX() + direction.getOffsetX() * i,
				       this.getY() + direction.getOffsetY() * i,
				       this.getZ() + direction.getOffsetZ() * i
		       );
	}

	/**
	 * Offset.
	 *
	 * @param axis axis
	 * @param i i
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos offset(Direction.Axis axis, int i) {
		if (i == 0) {
			return this;
		}
		else {
			int j = axis == Direction.Axis.X ? i : 0;
			int k = axis == Direction.Axis.Y ? i : 0;
			int l = axis == Direction.Axis.Z ? i : 0;
			return new BlockPos(this.getX() + j, this.getY() + k, this.getZ() + l);
		}
	}

	/**
	 * Rotate.
	 *
	 * @param rotation rotation
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos rotate(BlockRotation rotation) {
		return switch (rotation) {
			case CLOCKWISE_90 -> new BlockPos(-this.getZ(), this.getY(), this.getX());
			case CLOCKWISE_180 -> new BlockPos(-this.getX(), this.getY(), -this.getZ());
			case COUNTERCLOCKWISE_90 -> new BlockPos(this.getZ(), this.getY(), -this.getX());
			case NONE -> this;
		};
	}

	/**
	 * Cross product.
	 *
	 * @param pos pos
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos crossProduct(Vec3i pos) {
		return new BlockPos(
				this.getY() * pos.getZ() - this.getZ() * pos.getY(),
				this.getZ() * pos.getX() - this.getX() * pos.getZ(),
				this.getX() * pos.getY() - this.getY() * pos.getX()
		);
	}

	/**
	 * With y.
	 *
	 * @param y y
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos withY(int y) {
		return new BlockPos(this.getX(), y, this.getZ());
	}

	/**
	 * To immutable.
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos toImmutable() {
		return this;
	}

	public BlockPos.Mutable mutableCopy() {
		return new BlockPos.Mutable(this.getX(), this.getY(), this.getZ());
	}

	/**
	 * Clamp to within.
	 *
	 * @param pos pos
	 *
	 * @return Vec3d — результат операции
	 */
	public Vec3d clampToWithin(Vec3d pos) {
		return new Vec3d(
				MathHelper.clamp(pos.x, (double) (this.getX() + 1.0E-5F), this.getX() + 1.0 - 1.0E-5F),
				MathHelper.clamp(pos.y, (double) (this.getY() + 1.0E-5F), this.getY() + 1.0 - 1.0E-5F),
				MathHelper.clamp(pos.z, (double) (this.getZ() + 1.0E-5F), this.getZ() + 1.0 - 1.0E-5F)
		);
	}

	/**
	 * Iterate randomly.
	 *
	 * @param random random
	 * @param count count
	 * @param around around
	 * @param range range
	 *
	 * @return Iterable — результат операции
	 */
	public static Iterable<BlockPos> iterateRandomly(Random random, int count, BlockPos around, int range) {
		return iterateRandomly(
				random,
				count,
				around.getX() - range,
				around.getY() - range,
				around.getZ() - range,
				around.getX() + range,
				around.getY() + range,
				around.getZ() + range
		);
	}

	@Deprecated
	/**
	 * Stream south east square.
	 *
	 * @param pos pos
	 *
	 * @return Stream — результат операции
	 */
	public static Stream<BlockPos> streamSouthEastSquare(BlockPos pos) {
		return Stream.of(pos, pos.south(), pos.east(), pos.south().east());
	}

	public static Iterable<BlockPos> iterateRandomly(
			Random random,
			int count,
			int minX,
			int minY,
			int minZ,
			int maxX,
			int maxY,
			int maxZ
	) {
		int i = maxX - minX + 1;
		int j = maxY - minY + 1;
		int k = maxZ - minZ + 1;
		return () -> new AbstractIterator<BlockPos>() {
			final BlockPos.Mutable pos = new BlockPos.Mutable();
			int remaining = count;

			/**
			 * Вычисляет next.
			 *
			 * @return BlockPos — результат операции
			 */
			protected BlockPos computeNext() {
				if (this.remaining <= 0) {
					return (BlockPos) this.endOfData();
				}
				else {
					BlockPos
							blockPos =
							this.pos.set(minX + random.nextInt(i), minY + random.nextInt(j), minZ + random.nextInt(k));
					this.remaining--;
					return blockPos;
				}
			}
		};
	}

	/**
	 * Iterate outwards.
	 *
	 * @param center center
	 * @param rangeX range x
	 * @param rangeY range y
	 * @param rangeZ range z
	 *
	 * @return Iterable — результат операции
	 */
	public static Iterable<BlockPos> iterateOutwards(BlockPos center, int rangeX, int rangeY, int rangeZ) {
		int i = rangeX + rangeY + rangeZ;
		int j = center.getX();
		int k = center.getY();
		int l = center.getZ();
		return () -> new AbstractIterator<BlockPos>() {
			private final BlockPos.Mutable pos = new BlockPos.Mutable();
			private int manhattanDistance;
			private int limitX;
			private int limitY;
			private int dx;
			private int dy;
			private boolean swapZ;

			/**
			 * Вычисляет next.
			 *
			 * @return BlockPos — результат операции
			 */
			protected BlockPos computeNext() {
				if (this.swapZ) {
					this.swapZ = false;
					this.pos.setZ(l - (this.pos.getZ() - l));
					return this.pos;
				}
				else {
					BlockPos blockPos;
					for (blockPos = null; blockPos == null; this.dy++) {
						if (this.dy > this.limitY) {
							this.dx++;
							if (this.dx > this.limitX) {
								this.manhattanDistance++;
								if (this.manhattanDistance > i) {
									return (BlockPos) this.endOfData();
								}

								this.limitX = Math.min(rangeX, this.manhattanDistance);
								this.dx = -this.limitX;
							}

							this.limitY = Math.min(rangeY, this.manhattanDistance - Math.abs(this.dx));
							this.dy = -this.limitY;
						}

						int ix = this.dx;
						int jx = this.dy;
						int kx = this.manhattanDistance - Math.abs(ix) - Math.abs(jx);
						if (kx <= rangeZ) {
							this.swapZ = kx != 0;
							blockPos = this.pos.set(j + ix, k + jx, l + kx);
						}
					}

					return blockPos;
				}
			}
		};
	}

	public static Optional<BlockPos> findClosest(
			BlockPos pos,
			int horizontalRange,
			int verticalRange,
			Predicate<BlockPos> condition
	) {
		for (BlockPos blockPos : iterateOutwards(pos, horizontalRange, verticalRange, horizontalRange)) {
			if (condition.test(blockPos)) {
				return Optional.of(blockPos);
			}
		}

		return Optional.empty();
	}

	/**
	 * Stream outwards.
	 *
	 * @param center center
	 * @param maxX max x
	 * @param maxY max y
	 * @param maxZ max z
	 *
	 * @return Stream — результат операции
	 */
	public static Stream<BlockPos> streamOutwards(BlockPos center, int maxX, int maxY, int maxZ) {
		return StreamSupport.stream(iterateOutwards(center, maxX, maxY, maxZ).spliterator(), false);
	}

	/**
	 * Iterate.
	 *
	 * @param box box
	 *
	 * @return Iterable — результат операции
	 */
	public static Iterable<BlockPos> iterate(Box box) {
		BlockPos blockPos = ofFloored(box.minX, box.minY, box.minZ);
		BlockPos blockPos2 = ofFloored(box.maxX, box.maxY, box.maxZ);
		return iterate(blockPos, blockPos2);
	}

	/**
	 * Iterate.
	 *
	 * @param start start
	 * @param end end
	 *
	 * @return Iterable — результат операции
	 */
	public static Iterable<BlockPos> iterate(BlockPos start, BlockPos end) {
		return iterate(
				Math.min(start.getX(), end.getX()),
				Math.min(start.getY(), end.getY()),
				Math.min(start.getZ(), end.getZ()),
				Math.max(start.getX(), end.getX()),
				Math.max(start.getY(), end.getY()),
				Math.max(start.getZ(), end.getZ())
		);
	}

	/**
	 * Stream.
	 *
	 * @param start start
	 * @param end end
	 *
	 * @return Stream — результат операции
	 */
	public static Stream<BlockPos> stream(BlockPos start, BlockPos end) {
		return StreamSupport.stream(iterate(start, end).spliterator(), false);
	}

	/**
	 * Stream.
	 *
	 * @param box box
	 *
	 * @return Stream — результат операции
	 */
	public static Stream<BlockPos> stream(BlockBox box) {
		return stream(
				Math.min(box.getMinX(), box.getMaxX()),
				Math.min(box.getMinY(), box.getMaxY()),
				Math.min(box.getMinZ(), box.getMaxZ()),
				Math.max(box.getMinX(), box.getMaxX()),
				Math.max(box.getMinY(), box.getMaxY()),
				Math.max(box.getMinZ(), box.getMaxZ())
		);
	}

	/**
	 * Stream.
	 *
	 * @param box box
	 *
	 * @return Stream — результат операции
	 */
	public static Stream<BlockPos> stream(Box box) {
		return stream(
				MathHelper.floor(box.minX),
				MathHelper.floor(box.minY),
				MathHelper.floor(box.minZ),
				MathHelper.floor(box.maxX),
				MathHelper.floor(box.maxY),
				MathHelper.floor(box.maxZ)
		);
	}

	/**
	 * Stream.
	 *
	 * @param startX start x
	 * @param startY start y
	 * @param startZ start z
	 * @param endX end x
	 * @param endY end y
	 * @param endZ end z
	 *
	 * @return Stream — результат операции
	 */
	public static Stream<BlockPos> stream(int startX, int startY, int startZ, int endX, int endY, int endZ) {
		return StreamSupport.stream(iterate(startX, startY, startZ, endX, endY, endZ).spliterator(), false);
	}

	/**
	 * Iterate.
	 *
	 * @param startX start x
	 * @param startY start y
	 * @param startZ start z
	 * @param endX end x
	 * @param endY end y
	 * @param endZ end z
	 *
	 * @return Iterable — результат операции
	 */
	public static Iterable<BlockPos> iterate(int startX, int startY, int startZ, int endX, int endY, int endZ) {
		int i = endX - startX + 1;
		int j = endY - startY + 1;
		int k = endZ - startZ + 1;
		int l = i * j * k;
		return () -> new AbstractIterator<BlockPos>() {
			private final BlockPos.Mutable pos = new BlockPos.Mutable();
			private int index;

			/**
			 * Вычисляет next.
			 *
			 * @return BlockPos — результат операции
			 */
			protected BlockPos computeNext() {
				if (this.index == l) {
					return (BlockPos) this.endOfData();
				}
				else {
					int ix = this.index % i;
					int jx = this.index / i;
					int kx = jx % j;
					int lx = jx / j;
					this.index++;
					return this.pos.set(startX + ix, startY + kx, startZ + lx);
				}
			}
		};
	}

	public static Iterable<BlockPos.Mutable> iterateInSquare(
			BlockPos center,
			int radius,
			Direction firstDirection,
			Direction secondDirection
	) {
		Validate.validState(
				firstDirection.getAxis() != secondDirection.getAxis(),
				"The two directions cannot be on the same axis",
				new Object[0]
		);
		return () -> new AbstractIterator<BlockPos.Mutable>() {
			private final Direction[]
					directions =
					new Direction[]{
							firstDirection,
							secondDirection,
							firstDirection.getOpposite(),
							secondDirection.getOpposite()
					};
			private final BlockPos.Mutable pos = center.mutableCopy().move(secondDirection);
			private final int maxDirectionChanges = 4 * radius;
			private int directionChangeCount = -1;
			private int maxSteps;
			private int steps;
			private int currentX = this.pos.getX();
			private int currentY = this.pos.getY();
			private int currentZ = this.pos.getZ();

			protected BlockPos.Mutable computeNext() {
				this.pos
						.set(this.currentX, this.currentY, this.currentZ)
						.move(this.directions[(this.directionChangeCount + 4) % 4]);
				this.currentX = this.pos.getX();
				this.currentY = this.pos.getY();
				this.currentZ = this.pos.getZ();
				if (this.steps >= this.maxSteps) {
					if (this.directionChangeCount >= this.maxDirectionChanges) {
						return (BlockPos.Mutable) this.endOfData();
					}

					this.directionChangeCount++;
					this.steps = 0;
					this.maxSteps = this.directionChangeCount / 2 + 1;
				}

				this.steps++;
				return this.pos;
			}
		};
	}

	public static int iterateRecursively(
			BlockPos pos,
			int maxDepth,
			int maxIterations,
			BiConsumer<BlockPos, Consumer<BlockPos>> nextQueuer,
			Function<BlockPos, BlockPos.IterationState> callback
	) {
		Queue<Pair<BlockPos, Integer>> queue = new ArrayDeque<>();
		LongSet longSet = new LongOpenHashSet();
		queue.add(Pair.of(pos, 0));
		int i = 0;

		while (!queue.isEmpty()) {
			Pair<BlockPos, Integer> pair = queue.poll();
			BlockPos blockPos = (BlockPos) pair.getLeft();
			int j = (Integer) pair.getRight();
			long l = blockPos.asLong();
			if (longSet.add(l)) {
				BlockPos.IterationState iterationState = callback.apply(blockPos);
				if (iterationState != BlockPos.IterationState.SKIP) {
					if (iterationState == BlockPos.IterationState.STOP) {
						break;
					}

					if (++i >= maxIterations) {
						return i;
					}

					if (j < maxDepth) {
						nextQueuer.accept(blockPos, queuedPos -> queue.add(Pair.of(queuedPos, j + 1)));
					}
				}
			}
		}

		return i;
	}

	/**
	 * Iterate collision order.
	 *
	 * @param bounds bounds
	 * @param velocity velocity
	 *
	 * @return Iterable — результат операции
	 */
	public static Iterable<BlockPos> iterateCollisionOrder(Box bounds, Vec3d velocity) {
		Vec3d vec3d = bounds.getMinPos();
		int i = MathHelper.floor(vec3d.getX());
		int j = MathHelper.floor(vec3d.getY());
		int k = MathHelper.floor(vec3d.getZ());
		Vec3d vec3d2 = bounds.getMaxPos();
		int l = MathHelper.floor(vec3d2.getX());
		int m = MathHelper.floor(vec3d2.getY());
		int n = MathHelper.floor(vec3d2.getZ());
		return iterateCollisionOrder(i, j, k, l, m, n, velocity);
	}

	/**
	 * Iterate collision order.
	 *
	 * @param start start
	 * @param end end
	 * @param velocity velocity
	 *
	 * @return Iterable — результат операции
	 */
	public static Iterable<BlockPos> iterateCollisionOrder(BlockPos start, BlockPos end, Vec3d velocity) {
		return iterateCollisionOrder(
				start.getX(),
				start.getY(),
				start.getZ(),
				end.getX(),
				end.getY(),
				end.getZ(),
				velocity
		);
	}

	public static Iterable<BlockPos> iterateCollisionOrder(
			int x1,
			int y1,
			int z1,
			int x2,
			int y2,
			int z2,
			Vec3d velocity
	) {
		int i = Math.min(x1, x2);
		int j = Math.min(y1, y2);
		int k = Math.min(z1, z2);
		int l = Math.max(x1, x2);
		int m = Math.max(y1, y2);
		int n = Math.max(z1, z2);
		int o = l - i;
		int p = m - j;
		int q = n - k;
		int r = velocity.x >= 0.0 ? i : l;
		int s = velocity.y >= 0.0 ? j : m;
		int t = velocity.z >= 0.0 ? k : n;
		List<Direction.Axis> list = Direction.getCollisionOrder(velocity);
		Direction.Axis axis = list.get(0);
		Direction.Axis axis2 = list.get(1);
		Direction.Axis axis3 = list.get(2);
		Direction
				direction =
				velocity.getComponentAlongAxis(axis) >= 0.0 ? axis.getPositiveDirection() : axis.getNegativeDirection();
		Direction
				direction2 =
				velocity.getComponentAlongAxis(axis2) >= 0.0 ? axis2.getPositiveDirection()
				                                             : axis2.getNegativeDirection();
		Direction
				direction3 =
				velocity.getComponentAlongAxis(axis3) >= 0.0 ? axis3.getPositiveDirection()
				                                             : axis3.getNegativeDirection();
		int u = axis.choose(o, p, q);
		int v = axis2.choose(o, p, q);
		int w = axis3.choose(o, p, q);
		return () -> new AbstractIterator<BlockPos>() {
			private final BlockPos.Mutable pos = new BlockPos.Mutable();
			private int deltaAxis1;
			private int deltaAxis2;
			private int deltaAxis3;
			private boolean done;
			private final int axis1x = direction.getOffsetX();
			private final int axis1y = direction.getOffsetY();
			private final int axis1z = direction.getOffsetZ();
			private final int axis2x = direction2.getOffsetX();
			private final int axis2y = direction2.getOffsetY();
			private final int axis2z = direction2.getOffsetZ();
			private final int axis3x = direction3.getOffsetX();
			private final int axis3y = direction3.getOffsetY();
			private final int axis3z = direction3.getOffsetZ();

			/**
			 * Вычисляет next.
			 *
			 * @return BlockPos — результат операции
			 */
			protected BlockPos computeNext() {
				if (this.done) {
					return (BlockPos) this.endOfData();
				}
				else {
					this.pos
							.set(
									r + this.axis1x * this.deltaAxis1 + this.axis2x * this.deltaAxis2
											+ this.axis3x * this.deltaAxis3,
									s + this.axis1y * this.deltaAxis1 + this.axis2y * this.deltaAxis2
											+ this.axis3y * this.deltaAxis3,
									t + this.axis1z * this.deltaAxis1 + this.axis2z * this.deltaAxis2
											+ this.axis3z * this.deltaAxis3
							);
					if (this.deltaAxis3 < w) {
						this.deltaAxis3++;
					}
					else if (this.deltaAxis2 < v) {
						this.deltaAxis2++;
						this.deltaAxis3 = 0;
					}
					else if (this.deltaAxis1 < u) {
						this.deltaAxis1++;
						this.deltaAxis3 = 0;
						this.deltaAxis2 = 0;
					}
					else {
						this.done = true;
					}

					return this.pos;
				}
			}
		};
	}

	/**
	 * {@code IterationState}.
	 */
	public static enum IterationState {
		ACCEPT,
		SKIP,
		STOP;
	}

	/**
	 * {@code Mutable}.
	 */
	public static class Mutable extends BlockPos {

		public Mutable() {
			this(0, 0, 0);
		}

		public Mutable(int i, int j, int k) {
			super(i, j, k);
		}

		public Mutable(double x, double y, double z) {
			this(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
		}

		@Override
		public BlockPos add(int i, int j, int k) {
			return super.add(i, j, k).toImmutable();
		}

		@Override
		public BlockPos multiply(int i) {
			return super.multiply(i).toImmutable();
		}

		@Override
		public BlockPos offset(Direction direction, int i) {
			return super.offset(direction, i).toImmutable();
		}

		@Override
		public BlockPos offset(Direction.Axis axis, int i) {
			return super.offset(axis, i).toImmutable();
		}

		@Override
		public BlockPos rotate(BlockRotation rotation) {
			return super.rotate(rotation).toImmutable();
		}

		public BlockPos.Mutable set(int x, int y, int z) {
			this.setX(x);
			this.setY(y);
			this.setZ(z);
			return this;
		}

		public BlockPos.Mutable set(double x, double y, double z) {
			return this.set(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
		}

		public BlockPos.Mutable set(Vec3i pos) {
			return this.set(pos.getX(), pos.getY(), pos.getZ());
		}

		public BlockPos.Mutable set(long pos) {
			return this.set(unpackLongX(pos), unpackLongY(pos), unpackLongZ(pos));
		}

		public BlockPos.Mutable set(AxisCycleDirection axis, int x, int y, int z) {
			return this.set(
					axis.choose(x, y, z, Direction.Axis.X),
					axis.choose(x, y, z, Direction.Axis.Y),
					axis.choose(x, y, z, Direction.Axis.Z)
			);
		}

		public BlockPos.Mutable set(Vec3i pos, Direction direction) {
			return this.set(
					pos.getX() + direction.getOffsetX(),
					pos.getY() + direction.getOffsetY(),
					pos.getZ() + direction.getOffsetZ()
			);
		}

		public BlockPos.Mutable set(Vec3i pos, int x, int y, int z) {
			return this.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
		}

		public BlockPos.Mutable set(Vec3i vec1, Vec3i vec2) {
			return this.set(vec1.getX() + vec2.getX(), vec1.getY() + vec2.getY(), vec1.getZ() + vec2.getZ());
		}

		public BlockPos.Mutable move(Direction direction) {
			return this.move(direction, 1);
		}

		public BlockPos.Mutable move(Direction direction, int distance) {
			return this.set(
					this.getX() + direction.getOffsetX() * distance,
					this.getY() + direction.getOffsetY() * distance,
					this.getZ() + direction.getOffsetZ() * distance
			);
		}

		public BlockPos.Mutable move(int dx, int dy, int dz) {
			return this.set(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
		}

		public BlockPos.Mutable move(Vec3i vec) {
			return this.set(this.getX() + vec.getX(), this.getY() + vec.getY(), this.getZ() + vec.getZ());
		}

		public BlockPos.Mutable clamp(Direction.Axis axis, int min, int max) {
			return switch (axis) {
				case X -> this.set(MathHelper.clamp(this.getX(), min, max), this.getY(), this.getZ());
				case Y -> this.set(this.getX(), MathHelper.clamp(this.getY(), min, max), this.getZ());
				case Z -> this.set(this.getX(), this.getY(), MathHelper.clamp(this.getZ(), min, max));
			};
		}

		public BlockPos.Mutable setX(int i) {
			super.setX(i);
			return this;
		}

		public BlockPos.Mutable setY(int i) {
			super.setY(i);
			return this;
		}

		public BlockPos.Mutable setZ(int i) {
			super.setZ(i);
			return this;
		}

		@Override
		public BlockPos toImmutable() {
			return new BlockPos(this);
		}
	}
}
