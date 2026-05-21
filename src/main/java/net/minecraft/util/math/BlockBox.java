package net.minecraft.util.math;

import com.google.common.base.MoreObjects;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Util;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * {@code BlockBox}.
 */
public class BlockBox {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final Codec<BlockBox> CODEC = Codec.INT_STREAM
			.comapFlatMap(
					values -> Util
							.decodeFixedLengthArray(values, 6)
							.map(array -> new BlockBox(array[0], array[1], array[2], array[3], array[4], array[5])),
					box -> IntStream.of(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)
			)
			.stable();
	public static final PacketCodec<ByteBuf, BlockBox> PACKET_CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC,
			box -> new BlockPos(box.minX, box.minY, box.minZ),
			BlockPos.PACKET_CODEC,
			box -> new BlockPos(box.maxX, box.maxY, box.maxZ),
			(min, max) -> new BlockBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ())
	);
	private int minX;
	private int minY;
	private int minZ;
	private int maxX;
	private int maxY;
	private int maxZ;

	public BlockBox(BlockPos pos) {
		this(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
	}

	public BlockBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		if (maxX < minX || maxY < minY || maxZ < minZ) {
			Util.logErrorOrPause("Invalid bounding box data, inverted bounds for: " + this);
			this.minX = Math.min(minX, maxX);
			this.minY = Math.min(minY, maxY);
			this.minZ = Math.min(minZ, maxZ);
			this.maxX = Math.max(minX, maxX);
			this.maxY = Math.max(minY, maxY);
			this.maxZ = Math.max(minZ, maxZ);
		}
	}

	/**
	 * Create.
	 *
	 * @param first first
	 * @param second second
	 *
	 * @return BlockBox — результат операции
	 */
	public static BlockBox create(Vec3i first, Vec3i second) {
		return new BlockBox(
				Math.min(first.getX(), second.getX()),
				Math.min(first.getY(), second.getY()),
				Math.min(first.getZ(), second.getZ()),
				Math.max(first.getX(), second.getX()),
				Math.max(first.getY(), second.getY()),
				Math.max(first.getZ(), second.getZ())
		);
	}

	/**
	 * Infinite.
	 *
	 * @return BlockBox — результат операции
	 */
	public static BlockBox infinite() {
		return new BlockBox(
				Integer.MIN_VALUE,
				Integer.MIN_VALUE,
				Integer.MIN_VALUE,
				Integer.MAX_VALUE,
				Integer.MAX_VALUE,
				Integer.MAX_VALUE
		);
	}

	public static BlockBox rotated(
			int x,
			int y,
			int z,
			int offsetX,
			int offsetY,
			int offsetZ,
			int sizeX,
			int sizeY,
			int sizeZ,
			Direction facing
	) {
		switch (facing) {
			case SOUTH:
			default:
				return new BlockBox(
						x + offsetX,
						y + offsetY,
						z + offsetZ,
						x + sizeX - 1 + offsetX,
						y + sizeY - 1 + offsetY,
						z + sizeZ - 1 + offsetZ
				);
			case NORTH:
				return new BlockBox(
						x + offsetX,
						y + offsetY,
						z - sizeZ + 1 + offsetZ,
						x + sizeX - 1 + offsetX,
						y + sizeY - 1 + offsetY,
						z + offsetZ
				);
			case WEST:
				return new BlockBox(
						x - sizeZ + 1 + offsetZ,
						y + offsetY,
						z + offsetX,
						x + offsetZ,
						y + sizeY - 1 + offsetY,
						z + sizeX - 1 + offsetX
				);
			case EAST:
				return new BlockBox(
						x + offsetZ,
						y + offsetY,
						z + offsetX,
						x + sizeZ - 1 + offsetZ,
						y + sizeY - 1 + offsetY,
						z + sizeX - 1 + offsetX
				);
		}
	}

	/**
	 * Stream chunk pos.
	 *
	 * @return Stream — результат операции
	 */
	public Stream<ChunkPos> streamChunkPos() {
		int i = ChunkSectionPos.getSectionCoord(this.getMinX());
		int j = ChunkSectionPos.getSectionCoord(this.getMinZ());
		int k = ChunkSectionPos.getSectionCoord(this.getMaxX());
		int l = ChunkSectionPos.getSectionCoord(this.getMaxZ());
		return ChunkPos.stream(new ChunkPos(i, j), new ChunkPos(k, l));
	}

	/**
	 * Intersects.
	 *
	 * @param other other
	 *
	 * @return boolean — результат операции
	 */
	public boolean intersects(BlockBox other) {
		return this.maxX >= other.minX
				&& this.minX <= other.maxX
				&& this.maxZ >= other.minZ
				&& this.minZ <= other.maxZ
				&& this.maxY >= other.minY
				&& this.minY <= other.maxY;
	}

	/**
	 * Intersects x z.
	 *
	 * @param minX min x
	 * @param minZ min z
	 * @param maxX max x
	 * @param maxZ max z
	 *
	 * @return boolean — результат операции
	 */
	public boolean intersectsXZ(int minX, int minZ, int maxX, int maxZ) {
		return this.maxX >= minX && this.minX <= maxX && this.maxZ >= minZ && this.minZ <= maxZ;
	}

	/**
	 * Encompass positions.
	 *
	 * @param positions positions
	 *
	 * @return Optional — результат операции
	 */
	public static Optional<BlockBox> encompassPositions(Iterable<BlockPos> positions) {
		Iterator<BlockPos> iterator = positions.iterator();
		if (!iterator.hasNext()) {
			return Optional.empty();
		}
		else {
			BlockBox blockBox = new BlockBox(iterator.next());
			iterator.forEachRemaining(blockBox::encompass);
			return Optional.of(blockBox);
		}
	}

	/**
	 * Encompass.
	 *
	 * @param boxes boxes
	 *
	 * @return Optional — результат операции
	 */
	public static Optional<BlockBox> encompass(Iterable<BlockBox> boxes) {
		Iterator<BlockBox> iterator = boxes.iterator();
		if (!iterator.hasNext()) {
			return Optional.empty();
		}
		else {
			BlockBox blockBox = iterator.next();
			BlockBox
					blockBox2 =
					new BlockBox(
							blockBox.minX,
							blockBox.minY,
							blockBox.minZ,
							blockBox.maxX,
							blockBox.maxY,
							blockBox.maxZ
					);
			iterator.forEachRemaining(blockBox2::encompass);
			return Optional.of(blockBox2);
		}
	}

	@Deprecated
	/**
	 * Encompass.
	 *
	 * @param box box
	 *
	 * @return BlockBox — результат операции
	 */
	public BlockBox encompass(BlockBox box) {
		this.minX = Math.min(this.minX, box.minX);
		this.minY = Math.min(this.minY, box.minY);
		this.minZ = Math.min(this.minZ, box.minZ);
		this.maxX = Math.max(this.maxX, box.maxX);
		this.maxY = Math.max(this.maxY, box.maxY);
		this.maxZ = Math.max(this.maxZ, box.maxZ);
		return this;
	}

	/**
	 * Создаёт encompassing.
	 *
	 * @param box1 box1
	 * @param box2 box2
	 *
	 * @return BlockBox — результат операции
	 */
	public static BlockBox createEncompassing(BlockBox box1, BlockBox box2) {
		return new BlockBox(
				Math.min(box1.minX, box2.minX),
				Math.min(box1.minY, box2.minY),
				Math.min(box1.minZ, box2.minZ),
				Math.max(box1.maxX, box2.maxX),
				Math.max(box1.maxY, box2.maxY),
				Math.max(box1.maxZ, box2.maxZ)
		);
	}

	@Deprecated
	/**
	 * Encompass.
	 *
	 * @param pos pos
	 *
	 * @return BlockBox — результат операции
	 */
	public BlockBox encompass(BlockPos pos) {
		this.minX = Math.min(this.minX, pos.getX());
		this.minY = Math.min(this.minY, pos.getY());
		this.minZ = Math.min(this.minZ, pos.getZ());
		this.maxX = Math.max(this.maxX, pos.getX());
		this.maxY = Math.max(this.maxY, pos.getY());
		this.maxZ = Math.max(this.maxZ, pos.getZ());
		return this;
	}

	@Deprecated
	/**
	 * Move.
	 *
	 * @param dx dx
	 * @param dy dy
	 * @param dz dz
	 *
	 * @return BlockBox — результат операции
	 */
	public BlockBox move(int dx, int dy, int dz) {
		this.minX += dx;
		this.minY += dy;
		this.minZ += dz;
		this.maxX += dx;
		this.maxY += dy;
		this.maxZ += dz;
		return this;
	}

	@Deprecated
	/**
	 * Move.
	 *
	 * @param vec vec
	 *
	 * @return BlockBox — результат операции
	 */
	public BlockBox move(Vec3i vec) {
		return this.move(vec.getX(), vec.getY(), vec.getZ());
	}

	/**
	 * Offset.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return BlockBox — результат операции
	 */
	public BlockBox offset(int x, int y, int z) {
		return new BlockBox(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
	}

	/**
	 * Expand.
	 *
	 * @param offset offset
	 *
	 * @return BlockBox — результат операции
	 */
	public BlockBox expand(int offset) {
		return this.expand(offset, offset, offset);
	}

	/**
	 * Expand.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return BlockBox — результат операции
	 */
	public BlockBox expand(int x, int y, int z) {
		return new BlockBox(
				this.getMinX() - x,
				this.getMinY() - y,
				this.getMinZ() - z,
				this.getMaxX() + x,
				this.getMaxY() + y,
				this.getMaxZ() + z
		);
	}

	/**
	 * Contains.
	 *
	 * @param pos pos
	 *
	 * @return boolean — результат операции
	 */
	public boolean contains(Vec3i pos) {
		return this.contains(pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * Contains.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return boolean — результат операции
	 */
	public boolean contains(int x, int y, int z) {
		return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ && y >= this.minY && y <= this.maxY;
	}

	public Vec3i getDimensions() {
		return new Vec3i(this.maxX - this.minX, this.maxY - this.minY, this.maxZ - this.minZ);
	}

	public int getBlockCountX() {
		return this.maxX - this.minX + 1;
	}

	public int getBlockCountY() {
		return this.maxY - this.minY + 1;
	}

	public int getBlockCountZ() {
		return this.maxZ - this.minZ + 1;
	}

	public BlockPos getCenter() {
		return new BlockPos(
				this.minX + (this.maxX - this.minX + 1) / 2,
				this.minY + (this.maxY - this.minY + 1) / 2,
				this.minZ + (this.maxZ - this.minZ + 1) / 2
		);
	}

	/**
	 * For each vertex.
	 *
	 * @param consumer consumer
	 */
	public void forEachVertex(Consumer<BlockPos> consumer) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		consumer.accept(mutable.set(this.maxX, this.maxY, this.maxZ));
		consumer.accept(mutable.set(this.minX, this.maxY, this.maxZ));
		consumer.accept(mutable.set(this.maxX, this.minY, this.maxZ));
		consumer.accept(mutable.set(this.minX, this.minY, this.maxZ));
		consumer.accept(mutable.set(this.maxX, this.maxY, this.minZ));
		consumer.accept(mutable.set(this.minX, this.maxY, this.minZ));
		consumer.accept(mutable.set(this.maxX, this.minY, this.minZ));
		consumer.accept(mutable.set(this.minX, this.minY, this.minZ));
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
		                  .add("minX", this.minX)
		                  .add("minY", this.minY)
		                  .add("minZ", this.minZ)
		                  .add("maxX", this.maxX)
		                  .add("maxY", this.maxY)
		                  .add("maxZ", this.maxZ)
		                  .toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		else {
			return !(o instanceof BlockBox blockBox)
			       ? false
			       : this.minX == blockBox.minX
			         && this.minY == blockBox.minY
			         && this.minZ == blockBox.minZ
			         && this.maxX == blockBox.maxX
			         && this.maxY == blockBox.maxY
			         && this.maxZ == blockBox.maxZ;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
	}

	public int getMinX() {
		return this.minX;
	}

	public int getMinY() {
		return this.minY;
	}

	public int getMinZ() {
		return this.minZ;
	}

	public int getMaxX() {
		return this.maxX;
	}

	public int getMaxY() {
		return this.maxY;
	}

	public int getMaxZ() {
		return this.maxZ;
	}
}
