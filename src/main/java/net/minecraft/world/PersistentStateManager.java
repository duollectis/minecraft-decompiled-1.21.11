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
 * {@code PersistentStateManager}.
 */
public class PersistentStateManager implements AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();
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
		return this.directory.resolve(id + ".dat");
	}

	public <T extends PersistentState> T getOrCreate(PersistentStateType<T> type) {
		T persistentState = this.get(type);
		if (persistentState != null) {
			return persistentState;
		}
		else {
			T persistentState2 = (T) type.constructor().get();
			this.set(type, persistentState2);
			return persistentState2;
		}
	}

	/**
	 * Get.
	 *
	 * @param type type
	 *
	 * @return @Nullable T — 
	 */
	public <T extends PersistentState> @Nullable T get(PersistentStateType<T> type) {
		Optional<PersistentState> optional = this.loadedStates.get(type);
		if (optional == null) {
			optional = Optional.ofNullable(this.readFromFile(type));
			this.loadedStates.put(type, optional);
		}

		return (T) optional.orElse(null);
	}

	private <T extends PersistentState> @Nullable T readFromFile(PersistentStateType<T> type) {
		try {
			Path path = this.getFile(type.id());
			if (Files.exists(path)) {
				NbtCompound
						nbtCompound =
						this.readNbt(
								type.id(),
								type.dataFixType(),
								SharedConstants.getGameVersion().dataVersion().id()
						);
				RegistryOps<NbtElement> registryOps = this.registries.getOps(NbtOps.INSTANCE);
				return (T) type.codec()
				               .parse(registryOps, nbtCompound.get("data"))
				               .resultOrPartial(error -> LOGGER.error(
						               "Failed to parse saved data for '{}': {}",
						               type,
						               error
				               ))
				               .orElse(null);
			}
		}
		catch (Exception var5) {
			LOGGER.error("Error loading saved data: {}", type, var5);
		}

		return null;
	}

	/**
	 * Set.
	 *
	 * @param type type
	 * @param state state
	 *
	 * @return void — результат операции
	 */
	public <T extends PersistentState> void set(PersistentStateType<T> type, T state) {
		this.loadedStates.put(type, Optional.of(state));
		state.markDirty();
	}

	/**
	 * Читает nbt.
	 *
	 * @param id id
	 * @param dataFixTypes data fix types
	 * @param currentSaveVersion current save version
	 *
	 * @return NbtCompound — результат операции
	 */
	public NbtCompound readNbt(String id, DataFixTypes dataFixTypes, int currentSaveVersion) throws IOException {
		NbtCompound var8;
		try (
				InputStream inputStream = Files.newInputStream(this.getFile(id));
				PushbackInputStream pushbackInputStream = new PushbackInputStream(
						new FixedBufferInputStream(inputStream),
						2
				);
		) {
			NbtCompound nbtCompound;
			if (this.isCompressed(pushbackInputStream)) {
				nbtCompound = NbtIo.readCompressed(pushbackInputStream, NbtSizeTracker.ofUnlimitedBytes());
			}
			else {
				try (DataInputStream dataInputStream = new DataInputStream(pushbackInputStream)) {
					nbtCompound = NbtIo.readCompound(dataInputStream);
				}
			}

			int i = NbtHelper.getDataVersion(nbtCompound, 1343);
			var8 = dataFixTypes.update(this.dataFixer, nbtCompound, i, currentSaveVersion);
		}

		return var8;
	}

	private boolean isCompressed(PushbackInputStream stream) throws IOException {
		byte[] bs = new byte[2];
		boolean bl = false;
		int i = stream.read(bs, 0, 2);
		if (i == 2) {
			int j = (bs[1] & 255) << 8 | bs[0] & 255;
			if (j == 35615) {
				bl = true;
			}
		}

		if (i != 0) {
			stream.unread(bs, 0, i);
		}

		return bl;
	}

	/**
	 * Запускает saving.
	 *
	 * @return CompletableFuture — результат операции
	 */
	public CompletableFuture<?> startSaving() {
		Map<PersistentStateType<?>, NbtCompound> map = this.collectStatesToSave();
		if (map.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}
		else {
			int i = Util.getAvailableBackgroundThreads();
			int j = map.size();
			if (j > i) {
				this.savingFuture = this.savingFuture.thenCompose(object -> {
					List<CompletableFuture<?>> list = new ArrayList<>(i);
					int k = MathHelper.ceilDiv(j, i);

					for (List<Entry<PersistentStateType<?>, NbtCompound>> list2 : Iterables.partition(
							map.entrySet(),
							k
					)) {
						list.add(CompletableFuture.runAsync(
								() -> {
									for (Entry<PersistentStateType<?>, NbtCompound> entry : list2) {
										this.save(entry.getKey(), entry.getValue());
									}
								}, Util.getIoWorkerExecutor()
						));
					}

					return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
				});
			}
			else {
				this.savingFuture = this.savingFuture
						.thenCompose(
								object -> CompletableFuture.allOf(
										map.entrySet()
										   .stream()
										   .map(entry -> CompletableFuture.runAsync(
												   () -> this.save(
														   entry.getKey(),
														   entry.getValue()
												   ), Util.getIoWorkerExecutor()
										   ))
										   .toArray(CompletableFuture[]::new)
								)
						);
			}

			return this.savingFuture;
		}
	}

	private Map<PersistentStateType<?>, NbtCompound> collectStatesToSave() {
		Map<PersistentStateType<?>, NbtCompound> map = new Object2ObjectArrayMap();
		RegistryOps<NbtElement> registryOps = this.registries.getOps(NbtOps.INSTANCE);
		this.loadedStates.forEach((type, optionalState) -> optionalState
				.filter(PersistentState::isDirty)
				.ifPresent(state -> {
					map.put(type, this.encode(type, state, registryOps));
					state.setDirty(false);
				}));
		return map;
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
		Path path = this.getFile(type.id());

		try {
			NbtIo.writeCompressed(nbt, path);
		}
		catch (IOException var5) {
			LOGGER.error("Could not save data to {}", path.getFileName(), var5);
		}
	}

	/**
	 * Save.
	 */
	public void save() {
		this.startSaving().join();
	}

	@Override
	public void close() {
		this.save();
	}
}
