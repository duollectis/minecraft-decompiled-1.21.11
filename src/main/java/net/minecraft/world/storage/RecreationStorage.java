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
 * Хранилище для пересоздания (recreation) чанков: читает из исходного хранилища,
 * а записывает в отдельный выходной воркер. После закрытия удаляет выходную директорию.
 *
 * <p>Используется при обновлении мира — старые данные читаются, обновляются DataFixer'ом
 * и записываются в новое место, после чего временная директория очищается.
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

	/**
	 * Перенаправляет запись в выходной воркер вместо исходного хранилища.
	 */
	@Override
	public CompletableFuture<Void> set(ChunkPos chunkPos, Supplier<NbtCompound> chunkTagFactory) {
		markChunkDone(chunkPos);
		return recreationWorker.setResult(chunkPos, chunkTagFactory);
	}

	@Override
	public void close() throws IOException {
		super.close();
		recreationWorker.close();

		if (outputDirectory.toFile().exists()) {
			FileUtils.deleteDirectory(outputDirectory.toFile());
		}
	}
}
