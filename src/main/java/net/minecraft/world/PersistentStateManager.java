package net.minecraft.world;

import com.google.common.collect.Iterables;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.FixedBufferInputStream;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

/**
 * Менеджер персистентных состояний мира.
 * <p>
 * Отвечает за загрузку, кэширование и асинхронное сохранение объектов
 * {@link PersistentState} на диск в формате сжатого NBT. Каждое состояние
 * идентифицируется своим {@link PersistentStateType} и хранится в файле
 * {@code <id>.dat} в директории мира.
 */
public class PersistentStateManager implements AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Магическое число заголовка gzip-потока (little-endian). */
	private static final int GZIP_MAGIC = 0x8B1F;

	/** Количество байт заголовка для определения формата сжатия. */
	private static final int COMPRESSION_HEADER_SIZE = 2;

	/** Версия данных по умолчанию для файлов без явной версии. */
	private static final int DEFAULT_DATA_VERSION = 1343;

	private final Map<PersistentStateType<?>, Optional<PersistentState>> loadedStates = new HashMap<>();
	private final DataFixer dataFixer;
	private final RegistryWrapper.WrapperLookup registries;
	private final Path directory;
	private CompletableFuture<?> savingFuture = CompletableFuture.completedFuture(null);

	public PersistentStateManager(Path directory, DataFixer dataFixer, RegistryWrapper.WrapperLookup registries) {
		this.dataFixer = dataFixer;
		this.directory = directory;
		this.registries = registries;
	}

	private Path getFile(String id) {
		return directory.resolve(id + ".dat");
	}

	/**
	 * Возвращает существующее состояние или создаёт новое через конструктор типа.
	 *
	 * @param type тип персистентного состояния
	 * @return существующее или только что созданное состояние
	 */
	public <T extends PersistentState> T getOrCreate(PersistentStateType<T> type) {
		T existing = get(type);
		if (existing != null) {
			return existing;
		}

		T created = type.constructor().get();
		set(type, created);
		return created;
	}

	/**
	 * Возвращает загруженное состояние или читает его с диска.
	 * Результат кэшируется: повторный вызов не обращается к файловой системе.
	 *
	 * @param type тип персистентного состояния
	 * @return состояние или {@code null} если файл не существует
	 */
	public <T extends PersistentState> @Nullable T get(PersistentStateType<T> type) {
		Optional<PersistentState> cached = loadedStates.get(type);
		if (cached == null) {
			cached = Optional.ofNullable(readFromFile(type));
			loadedStates.put(type, cached);
		}

		@SuppressWarnings("unchecked")
		T result = (T) cached.orElse(null);
		return result;
	}

	private <T extends PersistentState> @Nullable T readFromFile(PersistentStateType<T> type) {
		try {
			Path path = getFile(type.id());
			if (!Files.exists(path)) {
				return null;
			}

			NbtCompound nbtCompound = readNbt(
				type.id(),
				type.dataFixType(),
				SharedConstants.getGameVersion().dataVersion().id()
			);
			RegistryOps<NbtElement> registryOps = registries.getOps(NbtOps.INSTANCE);

			@SuppressWarnings("unchecked")
			T result = (T) type.codec()
				.parse(registryOps, nbtCompound.get("data"))
				.resultOrPartial(error -> LOGGER.error("Failed to parse saved data for '{}': {}", type, error))
				.orElse(null);
			return result;
		} catch (Exception exception) {
			LOGGER.error("Error loading saved data: {}", type, exception);
		}

		return null;
	}

	public <T extends PersistentState> void set(PersistentStateType<T> type, T state) {
		loadedStates.put(type, Optional.of(state));
		state.markDirty();
	}

	/**
	 * Читает NBT-файл с диска, автоматически определяя формат (gzip или plain)
	 * и применяя DataFixer для миграции данных до текущей версии.
	 *
	 * @param id                 идентификатор состояния (имя файла без расширения)
	 * @param dataFixTypes       тип DataFixer для миграции
	 * @param currentSaveVersion текущая версия формата сохранения
	 * @return обновлённый NBT-compound
	 * @throws IOException при ошибке чтения файла
	 */
	public NbtCompound readNbt(String id, DataFixTypes dataFixTypes, int currentSaveVersion) throws IOException {
		try (
			InputStream inputStream = Files.newInputStream(getFile(id));
			PushbackInputStream pushbackStream = new PushbackInputStream(
				new FixedBufferInputStream(inputStream),
				COMPRESSION_HEADER_SIZE
			)
		) {
			NbtCompound nbtCompound = isCompressed(pushbackStream)
				? NbtIo.readCompressed(pushbackStream, NbtSizeTracker.ofUnlimitedBytes())
				: readUncompressed(pushbackStream);

			int savedVersion = NbtHelper.getDataVersion(nbtCompound, DEFAULT_DATA_VERSION);
			return dataFixTypes.update(dataFixer, nbtCompound, savedVersion, currentSaveVersion);
		}
	}

	private static NbtCompound readUncompressed(PushbackInputStream stream) throws IOException {
		try (DataInputStream dataInputStream = new DataInputStream(stream)) {
			return NbtIo.readCompound(dataInputStream);
		}
	}

	/**
	 * Определяет, является ли поток gzip-сжатым, проверяя первые два байта
	 * на соответствие магическому числу {@code 0x1F8B} (little-endian: {@code 0x8B1F}).
	 *
	 * @param stream поток с поддержкой возврата байт
	 * @return {@code true} если поток сжат gzip
	 * @throws IOException при ошибке чтения
	 */
	private boolean isCompressed(PushbackInputStream stream) throws IOException {
		byte[] header = new byte[COMPRESSION_HEADER_SIZE];
		int bytesRead = stream.read(header, 0, COMPRESSION_HEADER_SIZE);
		boolean compressed = false;

		if (bytesRead == COMPRESSION_HEADER_SIZE) {
			int magic = (header[1] & 0xFF) << 8 | header[0] & 0xFF;
			compressed = magic == GZIP_MAGIC;
		}

		if (bytesRead != 0) {
			stream.unread(header, 0, bytesRead);
		}

		return compressed;
	}

	/**
	 * Запускает асинхронное сохранение всех «грязных» состояний.
	 * <p>
	 * Если количество состояний превышает число доступных фоновых потоков,
	 * они разбиваются на батчи и сохраняются параллельно. Иначе каждое
	 * состояние сохраняется в отдельном потоке.
	 *
	 * @return {@link CompletableFuture}, завершающийся после записи всех файлов
	 */
	public CompletableFuture<?> startSaving() {
		Map<PersistentStateType<?>, NbtCompound> statesToSave = collectStatesToSave();
		if (statesToSave.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}

		int threadCount = Util.getAvailableBackgroundThreads();
		int stateCount = statesToSave.size();

		if (stateCount > threadCount) {
			savingFuture = savingFuture.thenCompose(ignored -> {
				List<CompletableFuture<?>> futures = new ArrayList<>(threadCount);
				int batchSize = MathHelper.ceilDiv(stateCount, threadCount);

				for (List<Entry<PersistentStateType<?>, NbtCompound>> batch : Iterables.partition(statesToSave.entrySet(), batchSize)) {
					futures.add(CompletableFuture.runAsync(
						() -> {
							for (Entry<PersistentStateType<?>, NbtCompound> entry : batch) {
								save(entry.getKey(), entry.getValue());
							}
						},
						Util.getIoWorkerExecutor()
					));
				}

				return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
			});
		} else {
			savingFuture = savingFuture.thenCompose(ignored ->
				CompletableFuture.allOf(
					statesToSave.entrySet()
						.stream()
						.map(entry -> CompletableFuture.runAsync(
							() -> save(entry.getKey(), entry.getValue()),
							Util.getIoWorkerExecutor()
						))
						.toArray(CompletableFuture[]::new)
				)
			);
		}

		return savingFuture;
	}

	private Map<PersistentStateType<?>, NbtCompound> collectStatesToSave() {
		Map<PersistentStateType<?>, NbtCompound> result = new Object2ObjectArrayMap<>();
		RegistryOps<NbtElement> registryOps = registries.getOps(NbtOps.INSTANCE);

		loadedStates.forEach((type, optionalState) ->
			optionalState
				.filter(PersistentState::isDirty)
				.ifPresent(state -> {
					result.put(type, encode(type, state, registryOps));
					state.setDirty(false);
				})
		);

		return result;
	}

	@SuppressWarnings("unchecked")
	private <T extends PersistentState> NbtCompound encode(
		PersistentStateType<T> type,
		PersistentState state,
		RegistryOps<NbtElement> ops
	) {
		Codec<T> codec = type.codec();
		NbtCompound nbtCompound = new NbtCompound();
		nbtCompound.put("data", (NbtElement) codec.encodeStart(ops, (T) state).getOrThrow());
		NbtHelper.putDataVersion(nbtCompound);
		return nbtCompound;
	}

	private void save(PersistentStateType<?> type, NbtCompound nbt) {
		Path path = getFile(type.id());
		try {
			NbtIo.writeCompressed(nbt, path);
		} catch (IOException exception) {
			LOGGER.error("Could not save data to {}", path.getFileName(), exception);
		}
	}

	/** Блокирующее сохранение всех «грязных» состояний. */
	public void save() {
		startSaving().join();
	}

	@Override
	public void close() {
		save();
	}
}
