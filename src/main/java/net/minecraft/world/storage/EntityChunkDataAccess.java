package net.minecraft.world.storage;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.SimpleConsecutiveExecutor;
import net.minecraft.world.chunk.Chunk;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Реализация {@link ChunkDataAccess} для сущностей ({@link Entity}).
 * Читает и записывает данные сущностей в NBT-формате через {@link VersionedChunkStorage}.
 * Десериализация выполняется асинхронно в выделенном {@link SimpleConsecutiveExecutor}.
 */
public class EntityChunkDataAccess implements ChunkDataAccess<Entity> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String ENTITIES_KEY = "Entities";
	private static final String POSITION_KEY = "Position";

	private final ServerWorld world;
	private final VersionedChunkStorage storage;
	private final LongSet emptyChunks = new LongOpenHashSet();
	private final SimpleConsecutiveExecutor taskExecutor;

	public EntityChunkDataAccess(VersionedChunkStorage storage, ServerWorld world, Executor executor) {
		this.storage = storage;
		this.world = world;
		this.taskExecutor = new SimpleConsecutiveExecutor(executor, "entity-deserializer");
	}

	/**
	 * Асинхронно читает список сущностей для чанка.
	 * Если чанк ранее был определён как пустой — возвращает пустой список без обращения к диску.
	 */
	@Override
	public CompletableFuture<ChunkDataList<Entity>> readChunkData(ChunkPos pos) {
		if (emptyChunks.contains(pos.toLong())) {
			return CompletableFuture.completedFuture(emptyDataList(pos));
		}

		CompletableFuture<Optional<NbtCompound>> nbtFuture = storage.getNbt(pos);
		handleLoadFailure(nbtFuture, pos);

		return nbtFuture.thenApplyAsync(
			nbt -> {
				if (nbt.isEmpty()) {
					emptyChunks.add(pos.toLong());
					return emptyDataList(pos);
				}

				validateChunkPosition(nbt.get(), pos);

				NbtCompound updatedNbt = storage.updateChunkNbt(nbt.get(), -1);

				try (ErrorReporter.Logging logging = new ErrorReporter.Logging(
					Chunk.createErrorReporterContext(pos), LOGGER)
				) {
					ReadView readView = NbtReadView.create(logging, world.getRegistryManager(), updatedNbt);
					ReadView.ListReadView listReadView = readView.getListReadView(ENTITIES_KEY);
					List<Entity> entities = EntityType.streamFromData(listReadView, world, SpawnReason.LOAD).toList();
					return new ChunkDataList<>(pos, entities);
				}
			},
			taskExecutor::send
		);
	}

	private void validateChunkPosition(NbtCompound nbt, ChunkPos expectedPos) {
		try {
			ChunkPos storedPos = nbt.<ChunkPos>get(POSITION_KEY, ChunkPos.CODEC).orElseThrow();

			if (!Objects.equals(expectedPos, storedPos)) {
				LOGGER.error(
					"Chunk file at {} is in the wrong location. (Expected {}, got {})",
					new Object[]{expectedPos, expectedPos, storedPos}
				);
				world.getServer().onChunkMisplacement(storedPos, expectedPos, storage.getStorageKey());
			}
		}
		catch (Exception exception) {
			LOGGER.warn("Failed to parse chunk {} position info", expectedPos, exception);
			world.getServer().onChunkLoadFailure(exception, storage.getStorageKey(), expectedPos);
		}
	}

	private static ChunkDataList<Entity> emptyDataList(ChunkPos pos) {
		return new ChunkDataList<>(pos, List.of());
	}

	@Override
	public void writeChunkData(ChunkDataList<Entity> dataList) {
		ChunkPos chunkPos = dataList.getChunkPos();

		if (dataList.isEmpty()) {
			if (emptyChunks.add(chunkPos.toLong())) {
				handleSaveFailure(storage.set(chunkPos, StorageIoWorker.NULL_NBT_SUPPLIER), chunkPos);
			}

			return;
		}

		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(
			Chunk.createErrorReporterContext(chunkPos), LOGGER)
		) {
			NbtList nbtList = new NbtList();
			dataList.stream().forEach(entity -> {
				NbtWriteView nbtWriteView = NbtWriteView.create(
					logging.makeChild(entity.getErrorReporterContext()),
					entity.getRegistryManager()
				);

				if (entity.saveData(nbtWriteView)) {
					nbtList.add(nbtWriteView.getNbt());
				}
			});

			NbtCompound nbtCompound = NbtHelper.putDataVersion(new NbtCompound());
			nbtCompound.put(ENTITIES_KEY, nbtList);
			nbtCompound.put(POSITION_KEY, ChunkPos.CODEC, chunkPos);
			handleSaveFailure(storage.setNbt(chunkPos, nbtCompound), chunkPos);
			emptyChunks.remove(chunkPos.toLong());
		}
	}

	private void handleSaveFailure(CompletableFuture<?> future, ChunkPos pos) {
		future.exceptionally(throwable -> {
			LOGGER.error("Failed to store entity chunk {}", pos, throwable);
			world.getServer().onChunkSaveFailure(throwable, storage.getStorageKey(), pos);
			return null;
		});
	}

	private void handleLoadFailure(CompletableFuture<?> future, ChunkPos pos) {
		future.exceptionally(throwable -> {
			LOGGER.error("Failed to load entity chunk {}", pos, throwable);
			world.getServer().onChunkLoadFailure(throwable, storage.getStorageKey(), pos);
			return null;
		});
	}

	@Override
	public void awaitAll(boolean sync) {
		storage.completeAll(sync).join();
		taskExecutor.runAll();
	}

	@Override
	public void close() throws IOException {
		storage.close();
	}
}
