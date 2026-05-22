package net.minecraft.world.storage;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.updater.ChunkUpdater;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Версионированное хранилище данных чанков с поддержкой DataFixer.
 * Делегирует I/O в {@link StorageIoWorker} и применяет {@link ChunkUpdater}
 * для миграции устаревших форматов данных при чтении.
 */
public class VersionedChunkStorage implements AutoCloseable {

	private static final String CONTEXT_KEY = "__context";

	private final StorageIoWorker worker;
	private final DataFixer dataFixer;
	private final DataFixTypes dataFixTypes;
	private final Supplier<ChunkUpdater> updaterFactory;

	public VersionedChunkStorage(
		StorageKey storageKey,
		Path directory,
		DataFixer dataFixer,
		boolean dsync,
		DataFixTypes dataFixTypes
	) {
		this(storageKey, directory, dataFixer, dsync, dataFixTypes, ChunkUpdater.PASSTHROUGH_FACTORY);
	}

	public VersionedChunkStorage(
		StorageKey storageKey,
		Path directory,
		DataFixer dataFixer,
		boolean dsync,
		DataFixTypes dataFixTypes,
		Supplier<ChunkUpdater> updaterFactory
	) {
		this.dataFixer = dataFixer;
		this.dataFixTypes = dataFixTypes;
		this.worker = new StorageIoWorker(storageKey, directory, dsync);
		// Мемоизируем фабрику: ChunkUpdater создаётся один раз и переиспользуется
		this.updaterFactory = Suppliers.memoize(updaterFactory::get);
	}

	public boolean needsBlending(ChunkPos chunkPos, int checkRadius) {
		return worker.needsBlending(chunkPos, checkRadius);
	}

	public CompletableFuture<Optional<NbtCompound>> getNbt(ChunkPos chunkPos) {
		return worker.readChunkData(chunkPos);
	}

	public CompletableFuture<Void> setNbt(ChunkPos chunkPos, NbtCompound chunkTag) {
		return set(chunkPos, () -> chunkTag);
	}

	public CompletableFuture<Void> set(ChunkPos chunkPos, Supplier<NbtCompound> chunkTagFactory) {
		markChunkDone(chunkPos);
		return worker.setResult(chunkPos, chunkTagFactory);
	}

	/**
	 * Применяет DataFixer к NBT чанка, обновляя его до текущей версии игры.
	 * Если версия уже актуальна — возвращает исходный объект без изменений.
	 *
	 * @param chunkNbt NBT данные чанка
	 * @param fallbackVersion версия для использования, если DataVersion отсутствует в NBT
	 * @param context дополнительный контекст для DataFixer (временно записывается в NBT)
	 * @return обновлённый NBT чанка
	 * @throws CrashException если DataFixer выбросил исключение
	 */
	public NbtCompound updateChunkNbt(NbtCompound chunkNbt, int fallbackVersion, @Nullable NbtCompound context) {
		int dataVersion = NbtHelper.getDataVersion(chunkNbt, fallbackVersion);

		if (dataVersion == SharedConstants.getGameVersion().dataVersion().id()) {
			return chunkNbt;
		}

		try {
			chunkNbt = updaterFactory.get().applyFix(chunkNbt);
			saveContextToNbt(chunkNbt, context);
			chunkNbt = dataFixTypes.update(
				dataFixer,
				chunkNbt,
				Math.max(updaterFactory.get().targetDataVersion(), dataVersion)
			);
			removeContext(chunkNbt);
			NbtHelper.putDataVersion(chunkNbt);
			return chunkNbt;
		}
		catch (Exception exception) {
			CrashReport crashReport = CrashReport.create(exception, "Updated chunk");
			CrashReportSection section = crashReport.addElement("Updated chunk details");
			section.add("Data version", dataVersion);
			throw new CrashException(crashReport);
		}
	}

	public NbtCompound updateChunkNbt(NbtCompound chunkNbt, int fallbackVersion) {
		return updateChunkNbt(chunkNbt, fallbackVersion, null);
	}

	public Dynamic<NbtElement> updateChunkNbt(Dynamic<NbtElement> chunkNbt, int fallbackVersion) {
		return new Dynamic<>(
			chunkNbt.getOps(),
			updateChunkNbt((NbtCompound) chunkNbt.getValue(), fallbackVersion, null)
		);
	}

	public static void saveContextToNbt(NbtCompound nbt, @Nullable NbtCompound context) {
		if (context != null) {
			nbt.put(CONTEXT_KEY, context);
		}
	}

	private static void removeContext(NbtCompound nbt) {
		nbt.remove(CONTEXT_KEY);
	}

	protected void markChunkDone(ChunkPos chunkPos) {
		updaterFactory.get().markChunkDone(chunkPos);
	}

	public CompletableFuture<Void> completeAll(boolean sync) {
		return worker.completeAll(sync);
	}

	@Override
	public void close() throws IOException {
		worker.close();
	}

	public NbtScannable getWorker() {
		return worker;
	}

	public StorageKey getStorageKey() {
		return worker.getStorageKey();
	}
}
