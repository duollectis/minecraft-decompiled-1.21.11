package net.minecraft.world.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.util.ThrowableDeliverer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.path.PathUtil;
import org.jspecify.annotations.Nullable;

public final class RegionBasedStorage implements AutoCloseable {
   public static final String MCA_EXTENSION = ".mca";
   private static final int MAX_CACHE_SIZE = 256;
   private final Long2ObjectLinkedOpenHashMap<RegionFile> cachedRegionFiles = new Long2ObjectLinkedOpenHashMap();
   private final StorageKey storageKey;
   private final Path directory;
   private final boolean dsync;

   RegionBasedStorage(StorageKey storageKey, Path directory, boolean dsync) {
      this.directory = directory;
      this.dsync = dsync;
      this.storageKey = storageKey;
   }

   private RegionFile getRegionFile(ChunkPos pos) throws IOException {
      long l = ChunkPos.toLong(pos.getRegionX(), pos.getRegionZ());
      RegionFile regionFile = (RegionFile)this.cachedRegionFiles.getAndMoveToFirst(l);
      if (regionFile != null) {
         return regionFile;
      } else {
         if (this.cachedRegionFiles.size() >= 256) {
            ((RegionFile)this.cachedRegionFiles.removeLast()).close();
         }

         PathUtil.createDirectories(this.directory);
         Path path = this.directory.resolve("r." + pos.getRegionX() + "." + pos.getRegionZ() + ".mca");
         RegionFile regionFile2 = new RegionFile(this.storageKey, path, this.directory, this.dsync);
         this.cachedRegionFiles.putAndMoveToFirst(l, regionFile2);
         return regionFile2;
      }
   }

   public @Nullable NbtCompound getTagAt(ChunkPos pos) throws IOException {
      RegionFile regionFile = this.getRegionFile(pos);

      NbtCompound var4;
      try (DataInputStream dataInputStream = regionFile.getChunkInputStream(pos)) {
         if (dataInputStream == null) {
            return null;
         }

         var4 = NbtIo.readCompound(dataInputStream);
      }

      return var4;
   }

   public void scanChunk(ChunkPos chunkPos, NbtScanner scanner) throws IOException {
      RegionFile regionFile = this.getRegionFile(chunkPos);

      try (DataInputStream dataInputStream = regionFile.getChunkInputStream(chunkPos)) {
         if (dataInputStream != null) {
            NbtIo.scan(dataInputStream, scanner, NbtSizeTracker.ofUnlimitedBytes());
         }
      }
   }

   protected void write(ChunkPos pos, @Nullable NbtCompound nbt) throws IOException {
      if (!SharedConstants.DONT_SAVE_WORLD) {
         RegionFile regionFile = this.getRegionFile(pos);
         if (nbt == null) {
            regionFile.delete(pos);
         } else {
            try (DataOutputStream dataOutputStream = regionFile.getChunkOutputStream(pos)) {
               NbtIo.writeCompound(nbt, dataOutputStream);
            }
         }
      }
   }

   @Override
   public void close() throws IOException {
      ThrowableDeliverer<IOException> throwableDeliverer = new ThrowableDeliverer<>();
      ObjectIterator var2 = this.cachedRegionFiles.values().iterator();

      while (var2.hasNext()) {
         RegionFile regionFile = (RegionFile)var2.next();

         try {
            regionFile.close();
         } catch (IOException var5) {
            throwableDeliverer.add(var5);
         }
      }

      throwableDeliverer.deliver();
   }

   public void sync() throws IOException {
      ObjectIterator var1 = this.cachedRegionFiles.values().iterator();

      while (var1.hasNext()) {
         RegionFile regionFile = (RegionFile)var1.next();
         regionFile.sync();
      }
   }

   public StorageKey getStorageKey() {
      return this.storageKey;
   }
}
