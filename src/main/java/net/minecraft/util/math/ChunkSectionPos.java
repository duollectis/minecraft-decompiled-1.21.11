package net.minecraft.util.math;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongConsumer;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.CuboidBlockIterator;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.entity.EntityLike;

import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * {@code ChunkSectionPos}.
 */
public class ChunkSectionPos extends Vec3i {

	public static final int SECTION_COORD_BITS = 4;
	public static final int SECTION_SIZE = 16;
	public static final int LOCAL_COORD_MASK = 15;
	public static final int SECTION_HALF_SIZE = 8;
	public static final int LOCAL_COORD_MAX = 15;
	private static final int PACKED_X_BITS = 22;
	private static final int PACKED_Y_BITS = 20;
	private static final int PACKED_Z_BITS = 22;
	private static final long PACKED_X_MASK = 4194303L;
	private static final long PACKED_Y_MASK = 1048575L;
	private static final long PACKED_Z_MASK = 4194303L;
	private static final int PACKED_X_SHIFT = 0;
	private static final int PACKED_Y_SHIFT = 20;
	private static final int PACKED_Z_SHIFT = 42;
	private static final int LOCAL_X_SHIFT = 8;
	private static final int LOCAL_Y_SHIFT = 0;
	private static final int LOCAL_Z_SHIFT = 4;
	public static final PacketCodec<ByteBuf, ChunkSectionPos>
			PACKET_CODEC =
			PacketCodecs.LONG.xmap(ChunkSectionPos::from, ChunkSectionPos::asLong);

	ChunkSectionPos(int i, int j, int k) {
		super(i, j, k);
	}

	/**
	 * From.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return ChunkSectionPos — результат операции
	 */
	public static ChunkSectionPos from(int x, int y, int z) {
		return new ChunkSectionPos(x, y, z);
	}

	/**
	 * From.
	 *
	 * @param pos pos
	 *
	 * @return ChunkSectionPos — результат операции
	 */
	public static ChunkSectionPos from(BlockPos pos) {
		return new ChunkSectionPos(
				getSectionCoord(pos.getX()),
				getSectionCoord(pos.getY()),
				getSectionCoord(pos.getZ())
		);
	}

	/**
	 * From.
	 *
	 * @param chunkPos chunk pos
	 * @param y y
	 *
	 * @return ChunkSectionPos — результат операции
	 */
	public static ChunkSectionPos from(ChunkPos chunkPos, int y) {
		return new ChunkSectionPos(chunkPos.x, y, chunkPos.z);
	}

	/**
	 * From.
	 *
	 * @param entity entity
	 *
	 * @return ChunkSectionPos — результат операции
	 */
	public static ChunkSectionPos from(EntityLike entity) {
		return from(entity.getBlockPos());
	}

	/**
	 * From.
	 *
	 * @param pos pos
	 *
	 * @return ChunkSectionPos — результат операции
	 */
	public static ChunkSectionPos from(Position pos) {
		return new ChunkSectionPos(
				getSectionCoordFloored(pos.getX()),
				getSectionCoordFloored(pos.getY()),
				getSectionCoordFloored(pos.getZ())
		);
	}

	/**
	 * From.
	 *
	 * @param packed packed
	 *
	 * @return ChunkSectionPos — результат операции
	 */
	public static ChunkSectionPos from(long packed) {
		return new ChunkSectionPos(unpackX(packed), unpackY(packed), unpackZ(packed));
	}

	/**
	 * From.
	 *
	 * @param chunk chunk
	 *
	 * @return ChunkSectionPos — результат операции
	 */
	public static ChunkSectionPos from(Chunk chunk) {
		return from(chunk.getPos(), chunk.getBottomSectionCoord());
	}

	/**
	 * Offset.
	 *
	 * @param packed packed
	 * @param direction direction
	 *
	 * @return long — результат операции
	 */
	public static long offset(long packed, Direction direction) {
		return offset(packed, direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
	}

	/**
	 * Offset.
	 *
	 * @param packed packed
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return long — результат операции
	 */
	public static long offset(long packed, int x, int y, int z) {
		return asLong(unpackX(packed) + x, unpackY(packed) + y, unpackZ(packed) + z);
	}

	public static int getSectionCoord(double coord) {
		return getSectionCoord(MathHelper.floor(coord));
	}

	public static int getSectionCoord(int coord) {
		return coord >> 4;
	}

	public static int getSectionCoordFloored(double coord) {
		return MathHelper.floor(coord) >> 4;
	}

	public static int getLocalCoord(int coord) {
		return coord & LOCAL_COORD_MASK;
	}

	/**
	 * Pack local.
	 *
	 * @param pos pos
	 *
	 * @return short — результат операции
	 */
	public static short packLocal(BlockPos pos) {
		int i = getLocalCoord(pos.getX());
		int j = getLocalCoord(pos.getY());
		int k = getLocalCoord(pos.getZ());
		return (short) (i << 8 | k << 4 | j << 0);
	}

	/**
	 * Unpack local x.
	 *
	 * @param packedLocalPos packed local pos
	 *
	 * @return int — результат операции
	 */
	public static int unpackLocalX(short packedLocalPos) {
		return packedLocalPos >>> 8 & LOCAL_COORD_MASK;
	}

	/**
	 * Unpack local y.
	 *
	 * @param packedLocalPos packed local pos
	 *
	 * @return int — результат операции
	 */
	public static int unpackLocalY(short packedLocalPos) {
		return packedLocalPos >>> 0 & LOCAL_COORD_MASK;
	}

	/**
	 * Unpack local z.
	 *
	 * @param packedLocalPos packed local pos
	 *
	 * @return int — результат операции
	 */
	public static int unpackLocalZ(short packedLocalPos) {
		return packedLocalPos >>> 4 & LOCAL_COORD_MASK;
	}

	/**
	 * Unpack block x.
	 *
	 * @param packedLocalPos packed local pos
	 *
	 * @return int — результат операции
	 */
	public int unpackBlockX(short packedLocalPos) {
		return this.getMinX() + unpackLocalX(packedLocalPos);
	}

	/**
	 * Unpack block y.
	 *
	 * @param packedLocalPos packed local pos
	 *
	 * @return int — результат операции
	 */
	public int unpackBlockY(short packedLocalPos) {
		return this.getMinY() + unpackLocalY(packedLocalPos);
	}

	/**
	 * Unpack block z.
	 *
	 * @param packedLocalPos packed local pos
	 *
	 * @return int — результат операции
	 */
	public int unpackBlockZ(short packedLocalPos) {
		return this.getMinZ() + unpackLocalZ(packedLocalPos);
	}

	/**
	 * Unpack block pos.
	 *
	 * @param packedLocalPos packed local pos
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos unpackBlockPos(short packedLocalPos) {
		return new BlockPos(
				this.unpackBlockX(packedLocalPos),
				this.unpackBlockY(packedLocalPos),
				this.unpackBlockZ(packedLocalPos)
		);
	}

	public static int getBlockCoord(int sectionCoord) {
		return sectionCoord << 4;
	}

	public static int getOffsetPos(int chunkCoord, int offset) {
		return getBlockCoord(chunkCoord) + offset;
	}

	/**
	 * Unpack x.
	 *
	 * @param packed packed
	 *
	 * @return int — результат операции
	 */
	public static int unpackX(long packed) {
		return (int) (packed << 0 >> PACKED_Z_SHIFT);
	}

	/**
	 * Unpack y.
	 *
	 * @param packed packed
	 *
	 * @return int — результат операции
	 */
	public static int unpackY(long packed) {
		return (int) (packed << 44 >> 44);
	}

	/**
	 * Unpack z.
	 *
	 * @param packed packed
	 *
	 * @return int — результат операции
	 */
	public static int unpackZ(long packed) {
		return (int) (packed << 22 >> PACKED_Z_SHIFT);
	}

	public int getSectionX() {
		return this.getX();
	}

	public int getSectionY() {
		return this.getY();
	}

	public int getSectionZ() {
		return this.getZ();
	}

	public int getMinX() {
		return getBlockCoord(this.getSectionX());
	}

	public int getMinY() {
		return getBlockCoord(this.getSectionY());
	}

	public int getMinZ() {
		return getBlockCoord(this.getSectionZ());
	}

	public int getMaxX() {
		return getOffsetPos(this.getSectionX(), LOCAL_COORD_MASK);
	}

	public int getMaxY() {
		return getOffsetPos(this.getSectionY(), LOCAL_COORD_MASK);
	}

	public int getMaxZ() {
		return getOffsetPos(this.getSectionZ(), LOCAL_COORD_MASK);
	}

	/**
	 * From block pos.
	 *
	 * @param blockPos block pos
	 *
	 * @return long — результат операции
	 */
	public static long fromBlockPos(long blockPos) {
		return asLong(
				getSectionCoord(BlockPos.unpackLongX(blockPos)),
				getSectionCoord(BlockPos.unpackLongY(blockPos)),
				getSectionCoord(BlockPos.unpackLongZ(blockPos))
		);
	}

	/**
	 * With zero y.
	 *
	 * @param x x
	 * @param z z
	 *
	 * @return long — результат операции
	 */
	public static long withZeroY(int x, int z) {
		return withZeroY(asLong(x, 0, z));
	}

	/**
	 * With zero y.
	 *
	 * @param pos pos
	 *
	 * @return long — результат операции
	 */
	public static long withZeroY(long pos) {
		return pos & -1048576L;
	}

	/**
	 * To chunk pos.
	 *
	 * @param sectionPos section pos
	 *
	 * @return long — результат операции
	 */
	public static long toChunkPos(long sectionPos) {
		return ChunkPos.toLong(unpackX(sectionPos), unpackZ(sectionPos));
	}

	public BlockPos getMinPos() {
		return new BlockPos(
				getBlockCoord(this.getSectionX()),
				getBlockCoord(this.getSectionY()),
				getBlockCoord(this.getSectionZ())
		);
	}

	public BlockPos getCenterPos() {
		int i = 8;
		return this.getMinPos().add(8, 8, 8);
	}

	/**
	 * To chunk pos.
	 *
	 * @return ChunkPos — результат операции
	 */
	public ChunkPos toChunkPos() {
		return new ChunkPos(this.getSectionX(), this.getSectionZ());
	}

	/**
	 * To long.
	 *
	 * @param pos pos
	 *
	 * @return long — результат операции
	 */
	public static long toLong(BlockPos pos) {
		return asLong(getSectionCoord(pos.getX()), getSectionCoord(pos.getY()), getSectionCoord(pos.getZ()));
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
		l |= (x & PACKED_X_MASK) << PACKED_Z_SHIFT;
		l |= (y & PACKED_Y_MASK) << 0;
		return l | (z & PACKED_X_MASK) << 20;
	}

	/**
	 * As long.
	 *
	 * @return long — результат операции
	 */
	public long asLong() {
		return asLong(this.getSectionX(), this.getSectionY(), this.getSectionZ());
	}

	/**
	 * Add.
	 *
	 * @param i i
	 * @param j j
	 * @param k k
	 *
	 * @return ChunkSectionPos — результат операции
	 */
	public ChunkSectionPos add(int i, int j, int k) {
		return i == 0 && j == 0 && k == 0 ? this : new ChunkSectionPos(
				this.getSectionX() + i,
				this.getSectionY() + j,
				this.getSectionZ() + k
		);
	}

	/**
	 * Stream blocks.
	 *
	 * @return Stream — результат операции
	 */
	public Stream<BlockPos> streamBlocks() {
		return BlockPos.stream(
				this.getMinX(),
				this.getMinY(),
				this.getMinZ(),
				this.getMaxX(),
				this.getMaxY(),
				this.getMaxZ()
		);
	}

	/**
	 * Stream.
	 *
	 * @param center center
	 * @param radius radius
	 *
	 * @return Stream — результат операции
	 */
	public static Stream<ChunkSectionPos> stream(ChunkSectionPos center, int radius) {
		int i = center.getSectionX();
		int j = center.getSectionY();
		int k = center.getSectionZ();
		return stream(i - radius, j - radius, k - radius, i + radius, j + radius, k + radius);
	}

	/**
	 * Stream.
	 *
	 * @param center center
	 * @param radius radius
	 * @param minY min y
	 * @param maxY max y
	 *
	 * @return Stream — результат операции
	 */
	public static Stream<ChunkSectionPos> stream(ChunkPos center, int radius, int minY, int maxY) {
		int i = center.x;
		int j = center.z;
		return stream(i - radius, minY, j - radius, i + radius, maxY, j + radius);
	}

	/**
	 * Stream.
	 *
	 * @param minX min x
	 * @param minY min y
	 * @param minZ min z
	 * @param maxX max x
	 * @param maxY max y
	 * @param maxZ max z
	 *
	 * @return Stream — результат операции
	 */
	public static Stream<ChunkSectionPos> stream(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return StreamSupport.stream(
				new AbstractSpliterator<ChunkSectionPos>(
						(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1),
						64
				) {
					final CuboidBlockIterator iterator = new CuboidBlockIterator(minX, minY, minZ, maxX, maxY, maxZ);

					@Override
					public boolean tryAdvance(Consumer<? super ChunkSectionPos> consumer) {
						if (this.iterator.step()) {
							consumer.accept(new ChunkSectionPos(
									this.iterator.getX(),
									this.iterator.getY(),
									this.iterator.getZ()
							));
							return true;
						}
						else {
							return false;
						}
					}
				}, false
		);
	}

	/**
	 * For each chunk section around.
	 *
	 * @param pos pos
	 * @param consumer consumer
	 */
	public static void forEachChunkSectionAround(BlockPos pos, LongConsumer consumer) {
		forEachChunkSectionAround(pos.getX(), pos.getY(), pos.getZ(), consumer);
	}

	/**
	 * For each chunk section around.
	 *
	 * @param pos pos
	 * @param consumer consumer
	 */
	public static void forEachChunkSectionAround(long pos, LongConsumer consumer) {
		forEachChunkSectionAround(
				BlockPos.unpackLongX(pos),
				BlockPos.unpackLongY(pos),
				BlockPos.unpackLongZ(pos),
				consumer
		);
	}

	/**
	 * For each chunk section around.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 * @param consumer consumer
	 */
	public static void forEachChunkSectionAround(int x, int y, int z, LongConsumer consumer) {
		int i = getSectionCoord(x - 1);
		int j = getSectionCoord(x + 1);
		int k = getSectionCoord(y - 1);
		int l = getSectionCoord(y + 1);
		int m = getSectionCoord(z - 1);
		int n = getSectionCoord(z + 1);
		if (i == j && k == l && m == n) {
			consumer.accept(asLong(i, k, m));
		}
		else {
			for (int o = i; o <= j; o++) {
				for (int p = k; p <= l; p++) {
					for (int q = m; q <= n; q++) {
						consumer.accept(asLong(o, p, q));
					}
				}
			}
		}
	}
}
