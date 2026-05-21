package net.minecraft.world.storage;

import com.mojang.datafixers.DataFixer;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.updater.ChunkUpdater;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * {@code RecreationStorage}.
 */
public class RecreationStorage extends VersionedChunkStorage {

	private final StorageIoWorker recreationWorker;
	private final Path outputDirectory;

	public RecreationStorage(
			StorageKey storageKey,
			Path directory,
			StorageKey outputStorageKey,
			Path outputDirectory,
			DataFixer dataFixer,
			boolean dsync,
			DataFixTypes dataFixTypes,
			Supplier<ChunkUpdater> updaterFactory
	) {
		super(storageKey, directory, dataFixer, dsync, dataFixTypes, updaterFactory);
		this.outputDirectory = outputDirectory;
		this.recreationWorker = new StorageIoWorker(outputStorageKey, outputDirectory, dsync);
	}

	@Override
	public CompletableFuture<Void> set(ChunkPos chunkPos, Supplier<NbtCompound> chunkTagFactory) {
		this.markChunkDone(chunkPos);
		return this.recreationWorker.setResult(chunkPos, chunkTagFactory);
	}

	@Override
	public void close() throws IOException {
		super.close();
		this.recreationWorker.close();
		if (this.outputDirectory.toFile().exists()) {
			FileUtils.deleteDirectory(this.outputDirectory.toFile());
		}
	}
}
