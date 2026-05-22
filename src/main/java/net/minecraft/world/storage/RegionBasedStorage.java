package net.minecraft.world.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.util.ThrowableDeliverer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.path.PathUtil;
import org.jspecify.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Низкоуровневое хранилище чанков на основе файлов региона (.mca).
 * Кэширует до {@value #MAX_CACHE_SIZE} открытых {@link RegionFile} в LRU-порядке.
 * Все операции чтения/записи делегируются соответствующему файлу региона.
 */
public final class RegionBasedStorage implements AutoCloseable {

	public static final String MCA_EXTENSION = ".mca";
	private static final int MAX_CACHE_SIZE = 256;

	private final Long2ObjectLinkedOpenHashMap<RegionFile> cachedRegionFiles = new Long2ObjectLinkedOpenHashMap<>();
	private final StorageKey storageKey;
	private final Path directory;
	private final boolean dsync;

	RegionBasedStorage(StorageKey storageKey, Path directory, boolean dsync) {
		this.storageKey = storageKey;
		this.directory = directory;
		this.dsync = dsync;
	}

	/**
	 * Возвращает {@link RegionFile} для чанка, открывая его при необходимости.
	 * Если кэш переполнен — вытесняет наиболее давно использованный файл.
	 */
	private RegionFile getRegionFile(ChunkPos pos) throws IOException {
		long regionKey = ChunkPos.toLong(pos.getRegionX(), pos.getRegionZ());
		RegionFile cached = cachedRegionFiles.getAndMoveToFirst(regionKey);

		if (cached != null) {
			return cached;
		}

		if (cachedRegionFiles.size() >= MAX_CACHE_SIZE) {
			cachedRegionFiles.removeLast().close();
		}

		PathUtil.createDirectories(directory);
		Path regionPath = directory.resolve("r." + pos.getRegionX() + "." + pos.getRegionZ() + MCA_EXTENSION);
		RegionFile regionFile = new RegionFile(storageKey, regionPath, directory, dsync);
		cachedRegionFiles.putAndMoveToFirst(regionKey, regionFile);

		return regionFile;
	}

	public @Nullable NbtCompound getTagAt(ChunkPos pos) throws IOException {
		RegionFile regionFile = getRegionFile(pos);

		try (DataInputStream dataInputStream = regionFile.getChunkInputStream(pos)) {
			if (dataInputStream == null) {
				return null;
			}

			return NbtIo.readCompound(dataInputStream);
		}
	}

	public void scanChunk(ChunkPos chunkPos, NbtScanner scanner) throws IOException {
		RegionFile regionFile = getRegionFile(chunkPos);

		try (DataInputStream dataInputStream = regionFile.getChunkInputStream(chunkPos)) {
			if (dataInputStream != null) {
				NbtIo.scan(dataInputStream, scanner, NbtSizeTracker.ofUnlimitedBytes());
			}
		}
	}

	protected void write(ChunkPos pos, @Nullable NbtCompound nbt) throws IOException {
		if (SharedConstants.DONT_SAVE_WORLD) {
			return;
		}

		RegionFile regionFile = getRegionFile(pos);

		if (nbt == null) {
			regionFile.delete(pos);
			return;
		}

		try (DataOutputStream dataOutputStream = regionFile.getChunkOutputStream(pos)) {
			NbtIo.writeCompound(nbt, dataOutputStream);
		}
	}

	@Override
	public void close() throws IOException {
		ThrowableDeliverer<IOException> throwableDeliverer = new ThrowableDeliverer<>();

		for (RegionFile regionFile : cachedRegionFiles.values()) {
			try {
				regionFile.close();
			}
			catch (IOException exception) {
				throwableDeliverer.add(exception);
			}
		}

		throwableDeliverer.deliver();
	}

	public void sync() throws IOException {
		for (RegionFile regionFile : cachedRegionFiles.values()) {
			regionFile.sync();
		}
	}

	public StorageKey getStorageKey() {
		return storageKey;
	}
}
