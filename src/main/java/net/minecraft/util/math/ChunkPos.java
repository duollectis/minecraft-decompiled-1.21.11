package net.minecraft.util.math;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Util;
import net.minecraft.world.chunk.ChunkGenerationSteps;
import net.minecraft.world.chunk.ChunkStatus;
import org.jspecify.annotations.Nullable;

import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Позиция чанка в координатах чанков (не блоков).
 * Один чанк = 16 блоков по X и Z. Регион = 32 чанка по каждой оси.
 */
public class ChunkPos {

	// --- Упаковка long ---
	private static final long LONG_PACK_SHIFT = 32L;
	private static final long LONG_PACK_MASK = 4294967295L;

	// --- Регионы ---
	private static final int REGION_COORD_SHIFT = 5;
	public static final int CHUNKS_PER_REGION = 32;
	private static final int REGION_COORD_MASK = 31;
	public static final int REGION_COORD_MAX = 31;

	// --- Генерация ---
	private static final int GENERATION_AREA_MARGIN =
		(CHUNKS_PER_REGION + ChunkGenerationSteps.GENERATION.get(ChunkStatus.FULL).accumulatedDependencies().size() + 1) * 2;
	public static final int MAX_COORDINATE = ChunkSectionPos.getSectionCoord(BlockPos.MAX_XZ) - GENERATION_AREA_MARGIN;

	// --- Хэш (LCG) ---
	private static final int LCG_MULTIPLIER = 1664525;
	private static final int LCG_INCREMENT = 1013904223;
	private static final int LCG_HASH_SEED = -559038737;

	// --- Прочие константы ---
	private static final int SPIRAL_ITERATOR_SIZE = 1056;

	public static final long MARKER = toLong(1875066, 1875066);
	public static final ChunkPos ORIGIN = new ChunkPos(0, 0);

	public static final Codec<ChunkPos> CODEC = Codec.INT_STREAM
		.comapFlatMap(
			stream -> Util.decodeFixedLengthIntArray(stream, 2).map(coords -> new ChunkPos(coords[0], coords[1])),
			chunkPos -> IntStream.of(chunkPos.x, chunkPos.z)
		)
		.stable();

	public static final PacketCodec<ByteBuf, ChunkPos> PACKET_CODEC = new PacketCodec<>() {
		@Override
		public ChunkPos decode(ByteBuf byteBuf) {
			return PacketByteBuf.readChunkPos(byteBuf);
		}

		@Override
		public void encode(ByteBuf byteBuf, ChunkPos chunkPos) {
			PacketByteBuf.writeChunkPos(byteBuf, chunkPos);
		}
	};

	public final int x;
	public final int z;

	public ChunkPos(int x, int z) {
		this.x = x;
		this.z = z;
	}

	public ChunkPos(BlockPos pos) {
		this.x = ChunkSectionPos.getSectionCoord(pos.getX());
		this.z = ChunkSectionPos.getSectionCoord(pos.getZ());
	}

	public ChunkPos(long pos) {
		this.x = (int) pos;
		this.z = (int) (pos >> CHUNKS_PER_REGION);
	}

	public static ChunkPos fromRegion(int x, int z) {
		return new ChunkPos(x << REGION_COORD_SHIFT, z << REGION_COORD_SHIFT);
	}

	public static ChunkPos fromRegionCenter(int x, int z) {
		return new ChunkPos((x << REGION_COORD_SHIFT) + REGION_COORD_MASK, (z << REGION_COORD_SHIFT) + REGION_COORD_MASK);
	}

	public boolean isWithinGenerationArea() {
		return isWithinGenerationArea(this.x, this.z);
	}

	public static boolean isWithinGenerationArea(int x, int z) {
		return MathHelper.chebyshevDistance(x, z) <= MAX_COORDINATE;
	}

	public long toLong() {
		return toLong(this.x, this.z);
	}

	/**
	 * Упаковывает координаты чанка в одно long-значение для эффективного хранения и сравнения.
	 * Используется как ключ в хэш-картах и для передачи по сети.
	 */
	public static long toLong(int chunkX, int chunkZ) {
		return chunkX & LONG_PACK_MASK | (chunkZ & LONG_PACK_MASK) << CHUNKS_PER_REGION;
	}

	public static long toLong(BlockPos pos) {
		return toLong(
			ChunkSectionPos.getSectionCoord(pos.getX()),
			ChunkSectionPos.getSectionCoord(pos.getZ())
		);
	}

	public static int getPackedX(long pos) {
		return (int) (pos & LONG_PACK_MASK);
	}

	public static int getPackedZ(long pos) {
		return (int) (pos >>> CHUNKS_PER_REGION & LONG_PACK_MASK);
	}

	@Override
	public int hashCode() {
		return hashCode(this.x, this.z);
	}

	/**
	 * Вычисляет хэш-код для пары координат чанка через LCG-алгоритм.
	 * Используется для равномерного распределения в хэш-таблицах.
	 */
	public static int hashCode(int x, int z) {
		int hashX = LCG_MULTIPLIER * x + LCG_INCREMENT;
		int hashZ = LCG_MULTIPLIER * (z ^ LCG_HASH_SEED) + LCG_INCREMENT;
		return hashX ^ hashZ;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		return other instanceof ChunkPos chunkPos && this.x == chunkPos.x && this.z == chunkPos.z;
	}

	public int getCenterX() {
		return this.getOffsetX(8);
	}

	public int getCenterZ() {
		return this.getOffsetZ(8);
	}

	public int getStartX() {
		return ChunkSectionPos.getBlockCoord(this.x);
	}

	public int getStartZ() {
		return ChunkSectionPos.getBlockCoord(this.z);
	}

	public int getEndX() {
		return this.getOffsetX(15);
	}

	public int getEndZ() {
		return this.getOffsetZ(15);
	}

	public int getRegionX() {
		return this.x >> REGION_COORD_SHIFT;
	}

	public int getRegionZ() {
		return this.z >> REGION_COORD_SHIFT;
	}

	public int getRegionRelativeX() {
		return this.x & REGION_COORD_MASK;
	}

	public int getRegionRelativeZ() {
		return this.z & REGION_COORD_MASK;
	}

	public BlockPos getBlockPos(int offsetX, int y, int offsetZ) {
		return new BlockPos(this.getOffsetX(offsetX), y, this.getOffsetZ(offsetZ));
	}

	public int getOffsetX(int offset) {
		return ChunkSectionPos.getOffsetPos(this.x, offset);
	}

	public int getOffsetZ(int offset) {
		return ChunkSectionPos.getOffsetPos(this.z, offset);
	}

	public BlockPos getCenterAtY(int y) {
		return new BlockPos(this.getCenterX(), y, this.getCenterZ());
	}

	public boolean contains(BlockPos pos) {
		return pos.getX() >= this.getStartX()
			&& pos.getZ() >= this.getStartZ()
			&& pos.getX() <= this.getEndX()
			&& pos.getZ() <= this.getEndZ();
	}

	@Override
	public String toString() {
		return "[" + this.x + ", " + this.z + "]";
	}

	public BlockPos getStartPos() {
		return new BlockPos(this.getStartX(), 0, this.getStartZ());
	}

	public int getChebyshevDistance(ChunkPos pos) {
		return this.getChebyshevDistance(pos.x, pos.z);
	}

	public int getChebyshevDistance(int x, int z) {
		return MathHelper.chebyshevDistanceBetween(this.x, this.z, x, z);
	}

	public int getSquaredDistance(ChunkPos pos) {
		return this.getSquaredDistance(pos.x, pos.z);
	}

	public int getSquaredDistance(long pos) {
		return this.getSquaredDistance(getPackedX(pos), getPackedZ(pos));
	}

	private int getSquaredDistance(int x, int z) {
		int deltaX = x - this.x;
		int deltaZ = z - this.z;
		return deltaX * deltaX + deltaZ * deltaZ;
	}

	/**
	 * Создаёт поток всех позиций чанков в квадрате радиуса {@code radius} вокруг {@code center}.
	 */
	public static Stream<ChunkPos> stream(ChunkPos center, int radius) {
		return stream(
				new ChunkPos(center.x - radius, center.z - radius),
				new ChunkPos(center.x + radius, center.z + radius)
		);
	}

	/**
	 * Создаёт поток всех позиций чанков в прямоугольнике между {@code pos1} и {@code pos2} включительно.
	 * Обход идёт строка за строкой по оси X, затем по Z.
	 */
	public static Stream<ChunkPos> stream(ChunkPos pos1, ChunkPos pos2) {
		int countX = Math.abs(pos1.x - pos2.x) + 1;
		int countZ = Math.abs(pos1.z - pos2.z) + 1;
		final int stepX = pos1.x < pos2.x ? 1 : -1;
		final int stepZ = pos1.z < pos2.z ? 1 : -1;

		return StreamSupport.stream(
				new AbstractSpliterator<ChunkPos>(countX * countZ, 64) {
					private @Nullable ChunkPos position;

					@Override
					public boolean tryAdvance(Consumer<? super ChunkPos> consumer) {
						if (this.position == null) {
							this.position = pos1;
						} else {
							int currentX = this.position.x;
							int currentZ = this.position.z;

							if (currentX == pos2.x) {
								if (currentZ == pos2.z) {
									return false;
								}

								this.position = new ChunkPos(pos1.x, currentZ + stepZ);
							} else {
								this.position = new ChunkPos(currentX + stepX, currentZ);
							}
						}

						consumer.accept(this.position);
						return true;
					}
				},
				false
		);
	}
}
