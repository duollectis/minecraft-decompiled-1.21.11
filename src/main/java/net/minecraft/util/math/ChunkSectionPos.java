package net.minecraft.util.math;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongConsumer;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.CuboidBlockIterator;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.entity.EntityLike;

public class ChunkSectionPos extends Vec3i {
   public static final int field_33096 = 4;
   public static final int field_33097 = 16;
   public static final int field_33100 = 15;
   public static final int field_33098 = 8;
   public static final int field_33099 = 15;
   private static final int field_33101 = 22;
   private static final int field_33102 = 20;
   private static final int field_33103 = 22;
   private static final long field_33104 = 4194303L;
   private static final long field_33105 = 1048575L;
   private static final long field_33106 = 4194303L;
   private static final int field_33107 = 0;
   private static final int field_33108 = 20;
   private static final int field_33109 = 42;
   private static final int field_33110 = 8;
   private static final int field_33111 = 0;
   private static final int field_33112 = 4;
   public static final PacketCodec<ByteBuf, ChunkSectionPos> PACKET_CODEC = PacketCodecs.LONG.xmap(ChunkSectionPos::from, ChunkSectionPos::asLong);

   ChunkSectionPos(int i, int j, int k) {
      super(i, j, k);
   }

   public static ChunkSectionPos from(int x, int y, int z) {
      return new ChunkSectionPos(x, y, z);
   }

   public static ChunkSectionPos from(BlockPos pos) {
      return new ChunkSectionPos(getSectionCoord(pos.getX()), getSectionCoord(pos.getY()), getSectionCoord(pos.getZ()));
   }

   public static ChunkSectionPos from(ChunkPos chunkPos, int y) {
      return new ChunkSectionPos(chunkPos.x, y, chunkPos.z);
   }

   public static ChunkSectionPos from(EntityLike entity) {
      return from(entity.getBlockPos());
   }

   public static ChunkSectionPos from(Position pos) {
      return new ChunkSectionPos(getSectionCoordFloored(pos.getX()), getSectionCoordFloored(pos.getY()), getSectionCoordFloored(pos.getZ()));
   }

   public static ChunkSectionPos from(long packed) {
      return new ChunkSectionPos(unpackX(packed), unpackY(packed), unpackZ(packed));
   }

   public static ChunkSectionPos from(Chunk chunk) {
      return from(chunk.getPos(), chunk.getBottomSectionCoord());
   }

   public static long offset(long packed, Direction direction) {
      return offset(packed, direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
   }

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
      return coord & 15;
   }

   public static short packLocal(BlockPos pos) {
      int i = getLocalCoord(pos.getX());
      int j = getLocalCoord(pos.getY());
      int k = getLocalCoord(pos.getZ());
      return (short)(i << 8 | k << 4 | j << 0);
   }

   public static int unpackLocalX(short packedLocalPos) {
      return packedLocalPos >>> 8 & 15;
   }

   public static int unpackLocalY(short packedLocalPos) {
      return packedLocalPos >>> 0 & 15;
   }

   public static int unpackLocalZ(short packedLocalPos) {
      return packedLocalPos >>> 4 & 15;
   }

   public int unpackBlockX(short packedLocalPos) {
      return this.getMinX() + unpackLocalX(packedLocalPos);
   }

   public int unpackBlockY(short packedLocalPos) {
      return this.getMinY() + unpackLocalY(packedLocalPos);
   }

   public int unpackBlockZ(short packedLocalPos) {
      return this.getMinZ() + unpackLocalZ(packedLocalPos);
   }

   public BlockPos unpackBlockPos(short packedLocalPos) {
      return new BlockPos(this.unpackBlockX(packedLocalPos), this.unpackBlockY(packedLocalPos), this.unpackBlockZ(packedLocalPos));
   }

   public static int getBlockCoord(int sectionCoord) {
      return sectionCoord << 4;
   }

   public static int getOffsetPos(int chunkCoord, int offset) {
      return getBlockCoord(chunkCoord) + offset;
   }

   public static int unpackX(long packed) {
      return (int)(packed << 0 >> 42);
   }

   public static int unpackY(long packed) {
      return (int)(packed << 44 >> 44);
   }

   public static int unpackZ(long packed) {
      return (int)(packed << 22 >> 42);
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
      return getOffsetPos(this.getSectionX(), 15);
   }

   public int getMaxY() {
      return getOffsetPos(this.getSectionY(), 15);
   }

   public int getMaxZ() {
      return getOffsetPos(this.getSectionZ(), 15);
   }

   public static long fromBlockPos(long blockPos) {
      return asLong(
         getSectionCoord(BlockPos.unpackLongX(blockPos)), getSectionCoord(BlockPos.unpackLongY(blockPos)), getSectionCoord(BlockPos.unpackLongZ(blockPos))
      );
   }

   public static long withZeroY(int x, int z) {
      return withZeroY(asLong(x, 0, z));
   }

   public static long withZeroY(long pos) {
      return pos & -1048576L;
   }

   public static long toChunkPos(long sectionPos) {
      return ChunkPos.toLong(unpackX(sectionPos), unpackZ(sectionPos));
   }

   public BlockPos getMinPos() {
      return new BlockPos(getBlockCoord(this.getSectionX()), getBlockCoord(this.getSectionY()), getBlockCoord(this.getSectionZ()));
   }

   public BlockPos getCenterPos() {
      int i = 8;
      return this.getMinPos().add(8, 8, 8);
   }

   public ChunkPos toChunkPos() {
      return new ChunkPos(this.getSectionX(), this.getSectionZ());
   }

   public static long toLong(BlockPos pos) {
      return asLong(getSectionCoord(pos.getX()), getSectionCoord(pos.getY()), getSectionCoord(pos.getZ()));
   }

   public static long asLong(int x, int y, int z) {
      long l = 0L;
      l |= (x & 4194303L) << 42;
      l |= (y & 1048575L) << 0;
      return l | (z & 4194303L) << 20;
   }

   public long asLong() {
      return asLong(this.getSectionX(), this.getSectionY(), this.getSectionZ());
   }

   public ChunkSectionPos add(int i, int j, int k) {
      return i == 0 && j == 0 && k == 0 ? this : new ChunkSectionPos(this.getSectionX() + i, this.getSectionY() + j, this.getSectionZ() + k);
   }

   public Stream<BlockPos> streamBlocks() {
      return BlockPos.stream(this.getMinX(), this.getMinY(), this.getMinZ(), this.getMaxX(), this.getMaxY(), this.getMaxZ());
   }

   public static Stream<ChunkSectionPos> stream(ChunkSectionPos center, int radius) {
      int i = center.getSectionX();
      int j = center.getSectionY();
      int k = center.getSectionZ();
      return stream(i - radius, j - radius, k - radius, i + radius, j + radius, k + radius);
   }

   public static Stream<ChunkSectionPos> stream(ChunkPos center, int radius, int minY, int maxY) {
      int i = center.x;
      int j = center.z;
      return stream(i - radius, minY, j - radius, i + radius, maxY, j + radius);
   }

   public static Stream<ChunkSectionPos> stream(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      return StreamSupport.stream(new AbstractSpliterator<ChunkSectionPos>((maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1), 64) {
         final CuboidBlockIterator iterator = new CuboidBlockIterator(minX, minY, minZ, maxX, maxY, maxZ);

         @Override
         public boolean tryAdvance(Consumer<? super ChunkSectionPos> consumer) {
            if (this.iterator.step()) {
               consumer.accept(new ChunkSectionPos(this.iterator.getX(), this.iterator.getY(), this.iterator.getZ()));
               return true;
            } else {
               return false;
            }
         }
      }, false);
   }

   public static void forEachChunkSectionAround(BlockPos pos, LongConsumer consumer) {
      forEachChunkSectionAround(pos.getX(), pos.getY(), pos.getZ(), consumer);
   }

   public static void forEachChunkSectionAround(long pos, LongConsumer consumer) {
      forEachChunkSectionAround(BlockPos.unpackLongX(pos), BlockPos.unpackLongY(pos), BlockPos.unpackLongZ(pos), consumer);
   }

   public static void forEachChunkSectionAround(int x, int y, int z, LongConsumer consumer) {
      int i = getSectionCoord(x - 1);
      int j = getSectionCoord(x + 1);
      int k = getSectionCoord(y - 1);
      int l = getSectionCoord(y + 1);
      int m = getSectionCoord(z - 1);
      int n = getSectionCoord(z + 1);
      if (i == j && k == l && m == n) {
         consumer.accept(asLong(i, k, m));
      } else {
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
