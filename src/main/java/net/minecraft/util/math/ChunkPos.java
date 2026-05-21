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
 * {@code ChunkPos}.
 */
public class ChunkPos {

	public static final Codec<ChunkPos> CODEC = Codec.INT_STREAM
			.comapFlatMap(
					stream -> Util.decodeFixedLengthArray(stream, 2).map(coords -> new ChunkPos(coords[0], coords[1])),
					chunkPos -> IntStream.of(chunkPos.x, chunkPos.z)
			)
			.stable();
	public static final PacketCodec<ByteBuf, ChunkPos> PACKET_CODEC = new PacketCodec<ByteBuf, ChunkPos>() {
		public ChunkPos decode(ByteBuf byteBuf) {
			return PacketByteBuf.readChunkPos(byteBuf);
		}

		public void encode(ByteBuf byteBuf, ChunkPos chunkPos) {
			PacketByteBuf.writeChunkPos(byteBuf, chunkPos);
		}
	};
	private static final int SPIRAL_ITERATOR_SIZE = 1056;
	public static final long MARKER = toLong(1875066, 1875066);
	private static final int
			GENERATION_AREA_MARGIN =
			(32 + ChunkGenerationSteps.GENERATION.get(ChunkStatus.FULL).accumulatedDependencies().size() + 1) * 2;
	public static final int MAX_COORDINATE = ChunkSectionPos.getSectionCoord(BlockPos.MAX_XZ) - GENERATION_AREA_MARGIN;
	public static final ChunkPos ORIGIN = new ChunkPos(0, 0);
	private static final long LONG_PACK_SHIFT = 32L;
	private static final long LONG_PACK_MASK = 4294967295L;
	private static final int REGION_COORD_SHIFT = 5;
	public static final int CHUNKS_PER_REGION = 32;
	private static final int REGION_COORD_MASK = 31;
	public static final int REGION_COORD_MAX = 31;
	public final int x;
	public final int z;
	private static final int LCG_MULTIPLIER = 1664525;
	private static final int LCG_INCREMENT = 1013904223;
	private static final int LCG_HASH_SEED = -559038737;

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
		this.z = (int) (pos >> 32);
	}

	public static ChunkPos fromRegion(int x, int z) {
		return new ChunkPos(x << 5, z << 5);
	}

	public static ChunkPos fromRegionCenter(int x, int z) {
		return new ChunkPos((x << 5) + 31, (z << 5) + 31);
	}

	public boolean isWithinGenerationArea() {
		return isWithinGenerationArea(this.x, this.z);
	}

	public static boolean isWithinGenerationArea(int i, int j) {
		return MathHelper.chebyshevDistance(i, j) <= MAX_COORDINATE;
	}

	public long toLong() {
		return toLong(this.x, this.z);
	}

	public static long toLong(int chunkX, int chunkZ) {
		return chunkX & 4294967295L | (chunkZ & 4294967295L) << 32;
	}

	public static long toLong(BlockPos pos) {
		return toLong(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()));
	}

	public static int getPackedX(long pos) {
		return (int) (pos & 4294967295L);
	}

	public static int getPackedZ(long pos) {
		return (int) (pos >>> 32 & 4294967295L);
	}

	@Override
	public int hashCode() {
		return hashCode(this.x, this.z);
	}

	public static int hashCode(int x, int z) {
		int i = 1664525 * x + 1013904223;
		int j = 1664525 * (z ^ -559038737) + 1013904223;
		return i ^ j;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		else {
			return !(o instanceof ChunkPos chunkPos) ? false : this.x == chunkPos.x && this.z == chunkPos.z;
		}
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
		return this.x >> 5;
	}

	public int getRegionZ() {
		return this.z >> 5;
	}

	public int getRegionRelativeX() {
		return this.x & 31;
	}

	public int getRegionRelativeZ() {
		return this.z & 31;
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
		return pos.getX() >= this.getStartX() && pos.getZ() >= this.getStartZ() && pos.getX() <= this.getEndX()
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
		int i = x - this.x;
		int j = z - this.z;
		return i * i + j * j;
	}

	public static Stream<ChunkPos> stream(ChunkPos center, int radius) {
		return stream(
				new ChunkPos(center.x - radius, center.z - radius),
				new ChunkPos(center.x + radius, center.z + radius)
		);
	}

	public static Stream<ChunkPos> stream(ChunkPos pos1, ChunkPos pos2) {
		int i = Math.abs(pos1.x - pos2.x) + 1;
		int j = Math.abs(pos1.z - pos2.z) + 1;
		final int k = pos1.x < pos2.x ? 1 : -1;
		final int l = pos1.z < pos2.z ? 1 : -1;
		return StreamSupport.stream(
				new AbstractSpliterator<ChunkPos>(i * j, 64) {
					private @Nullable ChunkPos position;

					@Override
					public boolean tryAdvance(Consumer<? super ChunkPos> consumer) {
						if (this.position == null) {
							this.position = pos1;
						}
						else {
							int ix = this.position.x;
							int jx = this.position.z;
							if (ix == pos2.x) {
								if (jx == pos2.z) {
									return false;
								}

								this.position = new ChunkPos(pos1.x, jx + l);
							}
							else {
								this.position = new ChunkPos(ix + k, jx);
							}
						}

						consumer.accept(this.position);
						return true;
					}
				}, false
		);
	}
}
