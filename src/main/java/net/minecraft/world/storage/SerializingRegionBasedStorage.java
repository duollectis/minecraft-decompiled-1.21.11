package net.minecraft.world.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.world.ChunkErrorHandler;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Базовый класс для хранилищ, сериализующих данные секций чанков в NBT.
 * Управляет жизненным циклом загрузки/выгрузки секций, отложенной записью
 * и версионированием через {@link VersionedChunkStorage}.
 *
 * @param <R> тип рантайм-объекта секции (например, {@code PointOfInterestSet})
 * @param <P> тип сериализованного представления секции
 */
public class SerializingRegionBasedStorage<R, P> implements AutoCloseable {

	static final Logger LOGGER = LogUtils.getLogger();
	private static final String SECTIONS_KEY = "Sections";

	private final VersionedChunkStorage storageAccess;
	private final Long2ObjectMap<Optional<R>> loadedElements = new Long2ObjectOpenHashMap<>();
	private final LongLinkedOpenHashSet unsavedElements = new LongLinkedOpenHashSet();
	private final Codec<P> codec;
	private final Function<R, P> serializer;
	private final BiFunction<P, Runnable, R> deserializer;
	private final Function<Runnable, R> factory;
	private final DynamicRegistryManager registryManager;
	private final ChunkErrorHandler errorHandler;
	protected final HeightLimitView world;
	private final LongSet loadedChunks = new LongOpenHashSet();
	private final Long2ObjectMap<CompletableFuture<Optional<LoadResult<P>>>> pendingLoads = new Long2ObjectOpenHashMap<>();
	private final Object lock = new Object();

	public SerializingRegionBasedStorage(
		VersionedChunkStorage storageAccess,
		Codec<P> codec,
		Function<R, P> serializer,
		BiFunction<P, Runnable, R> deserializer,
		Function<Runnable, R> factory,
		DynamicRegistryManager registryManager,
		ChunkErrorHandler errorHandler,
		HeightLimitView world
	) {
		this.storageAccess = storageAccess;
		this.codec = codec;
		this.serializer = serializer;
		this.deserializer = deserializer;
		this.factory = factory;
		this.registryManager = registryManager;
		this.errorHandler = errorHandler;
		this.world = world;
	}

	/**
	 * Обрабатывает очередь несохранённых секций и завершённые асинхронные загрузки.
	 * Вызывается каждый тик сервера.
	 *
	 * @param shouldKeepTicking условие продолжения обработки (ограничение по времени тика)
	 */
	protected void tick(BooleanSupplier shouldKeepTicking) {
		LongIterator iterator = unsavedElements.iterator();

		while (iterator.hasNext() && shouldKeepTicking.getAsBoolean()) {
			ChunkPos chunkPos = new ChunkPos(iterator.nextLong());
			iterator.remove();
			save(chunkPos);
		}

		tickPendingLoads();
	}

	private void tickPendingLoads() {
		synchronized (lock) {
			Iterator<Entry<CompletableFuture<Optional<LoadResult<P>>>>> iterator =
				Long2ObjectMaps.fastIterator(pendingLoads);

			while (iterator.hasNext()) {
				Entry<CompletableFuture<Optional<LoadResult<P>>>> entry = iterator.next();
				Optional<LoadResult<P>> result = entry.getValue().getNow(null);

				if (result == null) {
					continue;
				}

				long packedPos = entry.getLongKey();
				onLoad(new ChunkPos(packedPos), result.orElse(null));
				iterator.remove();
				loadedChunks.add(packedPos);
			}
		}
	}

	public void save() {
		if (unsavedElements.isEmpty()) {
			return;
		}

		unsavedElements.forEach(packedPos -> save(new ChunkPos(packedPos)));
		unsavedElements.clear();
	}

	public boolean hasUnsavedElements() {
		return !unsavedElements.isEmpty();
	}

	protected @Nullable Optional<R> getIfLoaded(long pos) {
		return loadedElements.get(pos);
	}

	/**
	 * Возвращает секцию по упакованной позиции, при необходимости синхронно загружая чанк.
	 *
	 * @param pos упакованная позиция секции ({@link ChunkSectionPos#asLong})
	 * @return {@link Optional} с данными секции, или пустой если позиция вне высотных границ
	 */
	protected Optional<R> get(long pos) {
		if (isPosInvalid(pos)) {
			return Optional.empty();
		}

		Optional<R> loaded = getIfLoaded(pos);

		if (loaded != null) {
			return loaded;
		}

		loadAndWait(ChunkSectionPos.from(pos).toChunkPos());
		loaded = getIfLoaded(pos);

		if (loaded == null) {
			throw (IllegalStateException) Util.getFatalOrPause(new IllegalStateException());
		}

		return loaded;
	}

	protected boolean isPosInvalid(long pos) {
		int blockY = ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackY(pos));
		return world.isOutOfHeightLimit(blockY);
	}

	protected R getOrCreate(long pos) {
		if (isPosInvalid(pos)) {
			throw (IllegalArgumentException) Util.getFatalOrPause(
				new IllegalArgumentException("sectionPos out of bounds")
			);
		}

		Optional<R> existing = get(pos);

		if (existing.isPresent()) {
			return existing.get();
		}

		R created = factory.apply(() -> onUpdate(pos));
		loadedElements.put(pos, Optional.of(created));
		return created;
	}

	public CompletableFuture<?> load(ChunkPos chunkPos) {
		synchronized (lock) {
			long packedPos = chunkPos.toLong();

			if (loadedChunks.contains(packedPos)) {
				return CompletableFuture.completedFuture(null);
			}

			return pendingLoads.computeIfAbsent(packedPos, ignored -> loadNbt(chunkPos));
		}
	}

	private void loadAndWait(ChunkPos chunkPos) {
		long packedPos = chunkPos.toLong();
		CompletableFuture<Optional<LoadResult<P>>> future;

		synchronized (lock) {
			if (!loadedChunks.add(packedPos)) {
				return;
			}

			future = pendingLoads.computeIfAbsent(packedPos, ignored -> loadNbt(chunkPos));
		}

		onLoad(chunkPos, future.join().orElse(null));

		synchronized (lock) {
			pendingLoads.remove(packedPos);
		}
	}

	private CompletableFuture<Optional<LoadResult<P>>> loadNbt(ChunkPos chunkPos) {
		RegistryOps<NbtElement> registryOps = registryManager.getOps(NbtOps.INSTANCE);

		return storageAccess
			.getNbt(chunkPos)
			.thenApplyAsync(
				chunkNbt -> chunkNbt.map(nbt -> LoadResult.fromNbt(
					codec, registryOps, nbt, storageAccess, world
				)),
				Util.getMainWorkerExecutor().named("parseSection")
			)
			.exceptionally(throwable -> {
				Throwable cause = throwable instanceof CompletionException ? throwable.getCause() : throwable;

				if (cause instanceof IOException ioException) {
					LOGGER.error("Error reading chunk {} data from disk", chunkPos, ioException);
					errorHandler.onChunkLoadFailure(ioException, storageAccess.getStorageKey(), chunkPos);
					return Optional.empty();
				}

				throw new CompletionException(cause);
			});
	}

	@SuppressWarnings("unchecked")
	private void onLoad(ChunkPos chunkPos, @Nullable LoadResult<P> result) {
		if (result == null) {
			for (int sectionY = world.getBottomSectionCoord(); sectionY <= world.getTopSectionCoord(); sectionY++) {
				loadedElements.put(chunkSectionPosAsLong(chunkPos, sectionY), Optional.empty());
			}

			return;
		}

		boolean versionChanged = result.versionChanged();

		for (int sectionY = world.getBottomSectionCoord(); sectionY <= world.getTopSectionCoord(); sectionY++) {
			long sectionPos = chunkSectionPosAsLong(chunkPos, sectionY);
			Optional<R> optional = Optional
				.ofNullable(result.sectionsByY.get(sectionY))
				.map(section -> deserializer.apply((P) section, () -> onUpdate(sectionPos)));
			loadedElements.put(sectionPos, optional);
			optional.ifPresent(element -> {
				onLoad(sectionPos);

				if (versionChanged) {
					onUpdate(sectionPos);
				}
			});
		}
	}

	private void save(ChunkPos pos) {
		RegistryOps<NbtElement> registryOps = registryManager.getOps(NbtOps.INSTANCE);
		Dynamic<NbtElement> dynamic = serialize(pos, registryOps);
		NbtElement nbtElement = dynamic.getValue();

		if (nbtElement instanceof NbtCompound nbtCompound) {
			storageAccess.setNbt(pos, nbtCompound).exceptionally(throwable -> {
				errorHandler.onChunkSaveFailure(throwable, storageAccess.getStorageKey(), pos);
				return null;
			});
		}
		else {
			LOGGER.error("Expected compound tag, got {}", nbtElement);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> Dynamic<T> serialize(ChunkPos chunkPos, DynamicOps<T> ops) {
		Map<T, T> sectionsMap = Maps.newHashMap();

		for (int sectionY = world.getBottomSectionCoord(); sectionY <= world.getTopSectionCoord(); sectionY++) {
			long sectionPos = chunkSectionPosAsLong(chunkPos, sectionY);
			Optional<R> optional = (Optional<R>) loadedElements.get(sectionPos);

			if (optional == null || optional.isEmpty()) {
				continue;
			}

			DataResult<T> dataResult = codec.encodeStart(ops, serializer.apply(optional.get()));
			String sectionKey = Integer.toString(sectionY);
			dataResult
				.resultOrPartial(LOGGER::error)
				.ifPresent(value -> sectionsMap.put((T) ops.createString(sectionKey), (T) value));
		}

		return new Dynamic<>(
			ops,
			ops.createMap(
				ImmutableMap.of(
					ops.createString(SECTIONS_KEY), ops.createMap(sectionsMap),
					ops.createString("DataVersion"), ops.createInt(SharedConstants.getGameVersion().dataVersion().id())
				)
			)
		);
	}

	private static long chunkSectionPosAsLong(ChunkPos chunkPos, int y) {
		return ChunkSectionPos.asLong(chunkPos.x, y, chunkPos.z);
	}

	protected void onLoad(long pos) {
	}

	/**
	 * Помечает секцию как изменённую и добавляет её чанк в очередь сохранения.
	 * Если секция не загружена — логирует предупреждение (возможная ошибка логики).
	 *
	 * @param pos упакованная позиция секции
	 */
	@SuppressWarnings("unchecked")
	protected void onUpdate(long pos) {
		Optional<R> optional = (Optional<R>) loadedElements.get(pos);

		if (optional != null && !optional.isEmpty()) {
			unsavedElements.add(ChunkPos.toLong(ChunkSectionPos.unpackX(pos), ChunkSectionPos.unpackZ(pos)));
			return;
		}

		LOGGER.warn("No data for position: {}", ChunkSectionPos.from(pos));
	}

	public void saveChunk(ChunkPos pos) {
		if (unsavedElements.remove(pos.toLong())) {
			save(pos);
		}
	}

	@Override
	public void close() throws IOException {
		storageAccess.close();
	}

	/**
	 * Результат загрузки чанка из NBT: карта секций по Y-координате
	 * и флаг изменения версии данных.
	 */
	record LoadResult<T>(Int2ObjectMap<T> sectionsByY, boolean versionChanged) {

		/**
		 * Десериализует секции чанка из NBT, применяя DataFixer при необходимости.
		 * Флаг {@code versionChanged} устанавливается, если данные были обновлены.
		 */
		public static <T> LoadResult<T> fromNbt(
			Codec<T> sectionCodec,
			DynamicOps<NbtElement> ops,
			NbtElement nbt,
			VersionedChunkStorage storage,
			HeightLimitView world
		) {
			Dynamic<NbtElement> original = new Dynamic<>(ops, nbt);
			Dynamic<NbtElement> updated = storage.updateChunkNbt(original, 1945);
			boolean versionChanged = original != updated;
			OptionalDynamic<NbtElement> sections = updated.get(SECTIONS_KEY);
			Int2ObjectMap<T> sectionsByY = new Int2ObjectOpenHashMap<>();

			for (int sectionY = world.getBottomSectionCoord(); sectionY <= world.getTopSectionCoord(); sectionY++) {
				Optional<T> section = sections.get(Integer.toString(sectionY))
					.result()
					.flatMap(data -> sectionCodec
						.parse(data)
						.resultOrPartial(SerializingRegionBasedStorage.LOGGER::error)
					);

				if (section.isPresent()) {
					sectionsByY.put(sectionY, section.get());
				}
			}

			return new LoadResult<>(sectionsByY, versionChanged);
		}
	}
}
