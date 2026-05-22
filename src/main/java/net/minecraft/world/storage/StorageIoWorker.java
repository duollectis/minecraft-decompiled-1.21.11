package net.minecraft.world.storage;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.scanner.NbtScanQuery;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.scanner.SelectiveNbtCollector;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.PrioritizedConsecutiveExecutor;
import net.minecraft.util.thread.TaskQueue;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Асинхронный I/O-воркер для чтения и записи данных чанков.
 * Буферизует незаписанные результаты в {@link #results} и сбрасывает их
 * в фоновом приоритете через {@link PrioritizedConsecutiveExecutor}.
 *
 * <p>Приоритеты задач: {@link Priority#FOREGROUND} — пользовательские запросы,
 * {@link Priority#BACKGROUND} — фоновая запись, {@link Priority#SHUTDOWN} — завершение.
 */
public class StorageIoWorker implements NbtScannable, AutoCloseable {

	/** Поставщик, возвращающий {@code null} — используется для удаления данных чанка. */
	public static final Supplier<NbtCompound> NULL_NBT_SUPPLIER = () -> null;

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_BLENDING_CACHE_SIZE = 1024;
	/** DataVersion, до которого чанки требуют blending при загрузке в новый движок. */
	private static final int BLENDING_REQUIRED_BEFORE_VERSION = 4295;

	private final AtomicBoolean closed = new AtomicBoolean();
	private final PrioritizedConsecutiveExecutor executor;
	private final RegionBasedStorage storage;
	private final SequencedMap<ChunkPos, Result> results = new LinkedHashMap<>();
	private final Long2ObjectLinkedOpenHashMap<CompletableFuture<BitSet>> blendingStatusCaches =
		new Long2ObjectLinkedOpenHashMap<>();

	protected StorageIoWorker(StorageKey storageKey, Path directory, boolean dsync) {
		this.storage = new RegionBasedStorage(storageKey, directory, dsync);
		this.executor = new PrioritizedConsecutiveExecutor(
			Priority.values().length,
			Util.getIoWorkerExecutor(),
			"IOWorker-" + storageKey.type()
		);
	}

	/**
	 * Проверяет, нужен ли blending хотя бы для одного чанка в заданном радиусе.
	 * Результаты кэшируются по регионам для повторных запросов.
	 *
	 * @param chunkPos центральный чанк
	 * @param checkRadius радиус проверки в чанках
	 * @return {@code true}, если хотя бы один чанк в радиусе требует blending
	 */
	public boolean needsBlending(ChunkPos chunkPos, int checkRadius) {
		ChunkPos minPos = new ChunkPos(chunkPos.x - checkRadius, chunkPos.z - checkRadius);
		ChunkPos maxPos = new ChunkPos(chunkPos.x + checkRadius, chunkPos.z + checkRadius);

		for (int regionX = minPos.getRegionX(); regionX <= maxPos.getRegionX(); regionX++) {
			for (int regionZ = minPos.getRegionZ(); regionZ <= maxPos.getRegionZ(); regionZ++) {
				BitSet blendingBits = getOrComputeBlendingStatus(regionX, regionZ).join();

				if (blendingBits.isEmpty()) {
					continue;
				}

				ChunkPos regionOrigin = ChunkPos.fromRegion(regionX, regionZ);
				int localMinX = Math.max(minPos.x - regionOrigin.x, 0);
				int localMinZ = Math.max(minPos.z - regionOrigin.z, 0);
				int localMaxX = Math.min(maxPos.x - regionOrigin.x, 31);
				int localMaxZ = Math.min(maxPos.z - regionOrigin.z, 31);

				for (int localX = localMinX; localX <= localMaxX; localX++) {
					for (int localZ = localMinZ; localZ <= localMaxZ; localZ++) {
						int bitIndex = localZ * 32 + localX;

						if (blendingBits.get(bitIndex)) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	private CompletableFuture<BitSet> getOrComputeBlendingStatus(int regionX, int regionZ) {
		long regionKey = ChunkPos.toLong(regionX, regionZ);

		synchronized (blendingStatusCaches) {
			CompletableFuture<BitSet> cached = blendingStatusCaches.getAndMoveToFirst(regionKey);

			if (cached == null) {
				cached = computeBlendingStatus(regionX, regionZ);
				blendingStatusCaches.putAndMoveToFirst(regionKey, cached);

				if (blendingStatusCaches.size() > MAX_BLENDING_CACHE_SIZE) {
					blendingStatusCaches.removeLast();
				}
			}

			return cached;
		}
	}

	private CompletableFuture<BitSet> computeBlendingStatus(int regionX, int regionZ) {
		return CompletableFuture.supplyAsync(
			() -> {
				ChunkPos regionStart = ChunkPos.fromRegion(regionX, regionZ);
				ChunkPos regionCenter = ChunkPos.fromRegionCenter(regionX, regionZ);
				BitSet blendingBits = new BitSet();

				ChunkPos.stream(regionStart, regionCenter).forEach(chunkPos -> {
					SelectiveNbtCollector collector = new SelectiveNbtCollector(
						new NbtScanQuery(NbtInt.TYPE, "DataVersion"),
						new NbtScanQuery(NbtCompound.TYPE, "blending_data")
					);

					try {
						scanChunk(chunkPos, collector).join();
					}
					catch (Exception exception) {
						LOGGER.warn("Failed to scan chunk {}", chunkPos, exception);
						return;
					}

					if (collector.getRoot() instanceof NbtCompound nbtCompound && requiresBlending(nbtCompound)) {
						int bitIndex = chunkPos.getRegionRelativeZ() * ChunkPos.CHUNKS_PER_REGION
							+ chunkPos.getRegionRelativeX();
						blendingBits.set(bitIndex);
					}
				});

				return blendingBits;
			},
			Util.getMainWorkerExecutor()
		);
	}

	private boolean requiresBlending(NbtCompound nbt) {
		return nbt.getInt("DataVersion", 0) < BLENDING_REQUIRED_BEFORE_VERSION
			|| nbt.getCompound("blending_data").isPresent();
	}

	public CompletableFuture<Void> setResult(ChunkPos pos, NbtCompound nbt) {
		return setResult(pos, () -> nbt);
	}

	/**
	 * Буферизует NBT-данные чанка для последующей записи на диск.
	 * Если запись для этой позиции уже ожидает — обновляет данные, сохраняя тот же Future.
	 */
	public CompletableFuture<Void> setResult(ChunkPos pos, Supplier<NbtCompound> nbtSupplier) {
		return this.<CompletableFuture<Void>>run(() -> {
			NbtCompound nbt = nbtSupplier.get();
			Result result = results.computeIfAbsent(pos, ignored -> new Result(nbt));
			result.nbt = nbt;
			return result.future;
		}).thenCompose(Function.identity());
	}

	/**
	 * Асинхронно читает данные чанка.
	 * Если чанк ожидает записи в буфере — возвращает его копию без обращения к диску.
	 */
	public CompletableFuture<Optional<NbtCompound>> readChunkData(ChunkPos pos) {
		return runChecked(() -> {
			Result buffered = results.get(pos);

			if (buffered != null) {
				return Optional.ofNullable(buffered.copyNbt());
			}

			try {
				return Optional.ofNullable(storage.getTagAt(pos));
			}
			catch (Exception exception) {
				LOGGER.warn("Failed to read chunk {}", pos, exception);
				throw exception;
			}
		});
	}

	/**
	 * Ожидает завершения всех ожидающих операций записи.
	 *
	 * @param sync если {@code true} — дополнительно синхронизирует файлы на диск (fsync)
	 */
	public CompletableFuture<Void> completeAll(boolean sync) {
		CompletableFuture<Void> allPending = this.<CompletableFuture<Void>>run(
			() -> CompletableFuture.allOf(
				results.values().stream()
					.map(result -> result.future)
					.toArray(CompletableFuture[]::new)
			)
		).thenCompose(Function.identity());

		if (sync) {
			return allPending.thenCompose(ignored -> runChecked(() -> {
				try {
					storage.sync();
					return null;
				}
				catch (Exception exception) {
					LOGGER.warn("Failed to synchronize chunks", exception);
					throw exception;
				}
			}));
		}

		return allPending.thenCompose(ignored -> run(() -> (Void) null));
	}

	@Override
	public CompletableFuture<Void> scanChunk(ChunkPos pos, NbtScanner scanner) {
		return runChecked(() -> {
			try {
				Result buffered = results.get(pos);

				if (buffered != null) {
					if (buffered.nbt != null) {
						buffered.nbt.accept(scanner);
					}
				}
				else {
					storage.scanChunk(pos, scanner);
				}

				return null;
			}
			catch (Exception exception) {
				LOGGER.warn("Failed to bulk scan chunk {}", pos, exception);
				throw exception;
			}
		});
	}

	private <T> CompletableFuture<T> runChecked(Callable<T> task) {
		return executor.executeAsync(
			Priority.FOREGROUND.ordinal(), future -> {
				if (!closed.get()) {
					try {
						future.complete(task.get());
					}
					catch (Exception exception) {
						future.completeExceptionally(exception);
					}
				}

				writeRemainingResults();
			}
		);
	}

	private <T> CompletableFuture<T> run(Supplier<T> task) {
		return executor.executeAsync(
			Priority.FOREGROUND.ordinal(), future -> {
				if (!closed.get()) {
					future.complete(task.get());
				}

				writeRemainingResults();
			}
		);
	}

	private void writeResult() {
		Entry<ChunkPos, Result> entry = results.pollFirstEntry();

		if (entry == null) {
			return;
		}

		write(entry.getKey(), entry.getValue());
		writeRemainingResults();
	}

	private void writeRemainingResults() {
		executor.send(new TaskQueue.PrioritizedTask(Priority.BACKGROUND.ordinal(), this::writeResult));
	}

	private void write(ChunkPos pos, Result result) {
		try {
			storage.write(pos, result.nbt);
			result.future.complete(null);
		}
		catch (Exception exception) {
			LOGGER.error("Failed to store chunk {}", pos, exception);
			result.future.completeExceptionally(exception);
		}
	}

	@Override
	public void close() throws IOException {
		if (!closed.compareAndSet(false, true)) {
			return;
		}

		runRemainingTasks();
		executor.close();

		try {
			storage.close();
		}
		catch (Exception exception) {
			LOGGER.error("Failed to close storage", exception);
		}
	}

	private void runRemainingTasks() {
		executor
			.executeAsync(Priority.SHUTDOWN.ordinal(), future -> future.complete(Unit.INSTANCE))
			.join();
	}

	public StorageKey getStorageKey() {
		return storage.getStorageKey();
	}

	@FunctionalInterface
	interface Callable<T> {

		@Nullable T get() throws Exception;
	}

	enum Priority {
		FOREGROUND,
		BACKGROUND,
		SHUTDOWN
	}

	static class Result {

		@Nullable NbtCompound nbt;
		final CompletableFuture<Void> future = new CompletableFuture<>();

		Result(@Nullable NbtCompound nbt) {
			this.nbt = nbt;
		}

		@Nullable NbtCompound copyNbt() {
			return nbt == null ? null : nbt.copy();
		}
	}
}
