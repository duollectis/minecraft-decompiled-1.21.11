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
 * Ограничивающий прямоугольник (AABB) в блочных координатах.
 * Хранит минимальные и максимальные координаты по каждой оси.
 * Используется в структурных генераторах, командах fill/clone и системе чанков.
 */
public class BlockBox {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final Codec<BlockBox> CODEC = Codec.INT_STREAM
			.comapFlatMap(
					values -> Util
							.decodeFixedLengthIntArray(values, 6)
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

	/**
	 * Создаёт ограничивающий прямоугольник. Если переданные координаты инвертированы
	 * (max < min), автоматически исправляет их и логирует предупреждение.
	 */
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
		return switch (facing) {
			case NORTH -> new BlockBox(
					x + offsetX,
					y + offsetY,
					z - sizeZ + 1 + offsetZ,
					x + sizeX - 1 + offsetX,
					y + sizeY - 1 + offsetY,
					z + offsetZ
			);
			case WEST -> new BlockBox(
					x - sizeZ + 1 + offsetZ,
					y + offsetY,
					z + offsetX,
					x + offsetZ,
					y + sizeY - 1 + offsetY,
					z + sizeX - 1 + offsetX
			);
			case EAST -> new BlockBox(
					x + offsetZ,
					y + offsetY,
					z + offsetX,
					x + sizeZ - 1 + offsetZ,
					y + sizeY - 1 + offsetY,
					z + sizeX - 1 + offsetX
			);
			default -> new BlockBox(
					x + offsetX,
					y + offsetY,
					z + offsetZ,
					x + sizeX - 1 + offsetX,
					y + sizeY - 1 + offsetY,
					z + sizeZ - 1 + offsetZ
			);
		};
	}

	public Stream<ChunkPos> streamChunkPos() {
		int minChunkX = ChunkSectionPos.getSectionCoord(minX);
		int minChunkZ = ChunkSectionPos.getSectionCoord(minZ);
		int maxChunkX = ChunkSectionPos.getSectionCoord(maxX);
		int maxChunkZ = ChunkSectionPos.getSectionCoord(maxZ);
		return ChunkPos.stream(new ChunkPos(minChunkX, minChunkZ), new ChunkPos(maxChunkX, maxChunkZ));
	}

	public boolean intersects(BlockBox other) {
		return maxX >= other.minX
				&& minX <= other.maxX
				&& maxZ >= other.minZ
				&& minZ <= other.maxZ
				&& maxY >= other.minY
				&& minY <= other.maxY;
	}

	public boolean intersectsXZ(int minX, int minZ, int maxX, int maxZ) {
		return this.maxX >= minX && this.minX <= maxX && this.maxZ >= minZ && this.minZ <= maxZ;
	}

	public static Optional<BlockBox> encompassPositions(Iterable<BlockPos> positions) {
		Iterator<BlockPos> iterator = positions.iterator();

		if (iterator.hasNext() == false) {
			return Optional.empty();
		}

		BlockBox box = new BlockBox(iterator.next());
		iterator.forEachRemaining(box::encompass);
		return Optional.of(box);
	}

	public static Optional<BlockBox> encompass(Iterable<BlockBox> boxes) {
		Iterator<BlockBox> iterator = boxes.iterator();

		if (iterator.hasNext() == false) {
			return Optional.empty();
		}

		BlockBox first = iterator.next();
		BlockBox result = new BlockBox(first.minX, first.minY, first.minZ, first.maxX, first.maxY, first.maxZ);
		iterator.forEachRemaining(result::encompass);
		return Optional.of(result);
	}

	/**
	 * @deprecated Мутирующий метод. Используй {@link #createEncompassing(BlockBox, BlockBox)} для иммутабельного варианта.
	 */
	@Deprecated
	public BlockBox encompass(BlockBox box) {
		minX = Math.min(minX, box.minX);
		minY = Math.min(minY, box.minY);
		minZ = Math.min(minZ, box.minZ);
		maxX = Math.max(maxX, box.maxX);
		maxY = Math.max(maxY, box.maxY);
		maxZ = Math.max(maxZ, box.maxZ);
		return this;
	}

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

	/**
	 * @deprecated Мутирующий метод. Предпочтительно использовать иммутабельные операции.
	 */
	@Deprecated
	public BlockBox encompass(BlockPos pos) {
		minX = Math.min(minX, pos.getX());
		minY = Math.min(minY, pos.getY());
		minZ = Math.min(minZ, pos.getZ());
		maxX = Math.max(maxX, pos.getX());
		maxY = Math.max(maxY, pos.getY());
		maxZ = Math.max(maxZ, pos.getZ());
		return this;
	}

	/**
	 * @deprecated Мутирующий метод. Используй {@link #offset(int, int, int)} для иммутабельного варианта.
	 */
	@Deprecated
	public BlockBox move(int dx, int dy, int dz) {
		minX += dx;
		minY += dy;
		minZ += dz;
		maxX += dx;
		maxY += dy;
		maxZ += dz;
		return this;
	}

	/**
	 * @deprecated Мутирующий метод. Используй {@link #offset(int, int, int)} для иммутабельного варианта.
	 */
	@Deprecated
	public BlockBox move(Vec3i vec) {
		return move(vec.getX(), vec.getY(), vec.getZ());
	}

	public BlockBox offset(int x, int y, int z) {
		return new BlockBox(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z);
	}

	public BlockBox expand(int offset) {
		return expand(offset, offset, offset);
	}

	public BlockBox expand(int x, int y, int z) {
		return new BlockBox(
				getMinX() - x,
				getMinY() - y,
				getMinZ() - z,
				getMaxX() + x,
				getMaxY() + y,
				getMaxZ() + z
		);
	}

	public boolean contains(Vec3i pos) {
		return contains(pos.getX(), pos.getY(), pos.getZ());
	}

	public boolean contains(int x, int y, int z) {
		return x >= minX && x <= maxX && z >= minZ && z <= maxZ && y >= minY && y <= maxY;
	}

	public Vec3i getDimensions() {
		return new Vec3i(maxX - minX, maxY - minY, maxZ - minZ);
	}

	public int getBlockCountX() {
		return maxX - minX + 1;
	}

	public int getBlockCountY() {
		return maxY - minY + 1;
	}

	public int getBlockCountZ() {
		return maxZ - minZ + 1;
	}

	public BlockPos getCenter() {
		return new BlockPos(
				minX + (maxX - minX + 1) / 2,
				minY + (maxY - minY + 1) / 2,
				minZ + (maxZ - minZ + 1) / 2
		);
	}

	public void forEachVertex(Consumer<BlockPos> consumer) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		consumer.accept(mutable.set(maxX, maxY, maxZ));
		consumer.accept(mutable.set(minX, maxY, maxZ));
		consumer.accept(mutable.set(maxX, minY, maxZ));
		consumer.accept(mutable.set(minX, minY, maxZ));
		consumer.accept(mutable.set(maxX, maxY, minZ));
		consumer.accept(mutable.set(minX, maxY, minZ));
		consumer.accept(mutable.set(maxX, minY, minZ));
		consumer.accept(mutable.set(minX, minY, minZ));
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("minX", minX)
				.add("minY", minY)
				.add("minZ", minZ)
				.add("maxX", maxX)
				.add("maxY", maxY)
				.add("maxZ", maxZ)
				.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		return o instanceof BlockBox other
				&& minX == other.minX
				&& minY == other.minY
				&& minZ == other.minZ
				&& maxX == other.maxX
				&& maxY == other.maxY
				&& maxZ == other.maxZ;
	}

	@Override
	public int hashCode() {
		return Objects.hash(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public int getMinX() {
		return minX;
	}

	public int getMinY() {
		return minY;
	}

	public int getMinZ() {
		return minZ;
	}

	public int getMaxX() {
		return maxX;
	}

	public int getMaxY() {
		return maxY;
	}

	public int getMaxZ() {
		return maxZ;
	}
}
