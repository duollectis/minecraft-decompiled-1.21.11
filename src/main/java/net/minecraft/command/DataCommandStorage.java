package net.minecraft.command;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Хранилище NBT-данных для команды {@code /data storage}.
 * Данные персистентны — сохраняются на диск через {@link PersistentStateManager}.
 */
public class DataCommandStorage {

	private static final String COMMAND_STORAGE_PREFIX = "command_storage_";

	private final Map<String, PersistentState> storages = new HashMap<>();
	private final PersistentStateManager stateManager;

	public DataCommandStorage(PersistentStateManager stateManager) {
		this.stateManager = stateManager;
	}

	public NbtCompound get(Identifier id) {
		PersistentState state = getStorage(id.getNamespace());
		return state != null ? state.get(id.getPath()) : new NbtCompound();
	}

	private @Nullable PersistentState getStorage(String namespace) {
		PersistentState cached = storages.get(namespace);
		if (cached != null) {
			return cached;
		}

		PersistentState loaded = stateManager.get(PersistentState.createStateType(namespace));
		if (loaded != null) {
			storages.put(namespace, loaded);
		}

		return loaded;
	}

	private PersistentState getOrCreateStorage(String namespace) {
		PersistentState cached = storages.get(namespace);
		if (cached != null) {
			return cached;
		}

		PersistentState created = stateManager.getOrCreate(PersistentState.createStateType(namespace));
		storages.put(namespace, created);
		return created;
	}

	public void set(Identifier id, NbtCompound nbt) {
		getOrCreateStorage(id.getNamespace()).set(id.getPath(), nbt);
	}

	public Stream<Identifier> getIds() {
		return storages.entrySet().stream().flatMap(entry -> entry.getValue().getIds(entry.getKey()));
	}

	static String getSaveKey(String namespace) {
		return COMMAND_STORAGE_PREFIX + namespace;
	}

	static class PersistentState extends net.minecraft.world.PersistentState {

		public static final Codec<PersistentState> CODEC = RecordCodecBuilder.create(
				instance -> instance
						.group(Codec
								.unboundedMap(Codecs.IDENTIFIER_PATH, NbtCompound.CODEC)
								.fieldOf("contents")
								.forGetter(state -> state.map))
						.apply(instance, PersistentState::new)
		);

		private final Map<String, NbtCompound> map;

		private PersistentState(Map<String, NbtCompound> map) {
			this.map = new HashMap<>(map);
		}

		private PersistentState() {
			this(new HashMap<>());
		}

		public static PersistentStateType<PersistentState> createStateType(String id) {
			return new PersistentStateType<>(
					DataCommandStorage.getSaveKey(id),
					PersistentState::new,
					CODEC,
					DataFixTypes.SAVED_DATA_COMMAND_STORAGE
			);
		}

		public NbtCompound get(String name) {
			NbtCompound nbt = map.get(name);
			return nbt != null ? nbt : new NbtCompound();
		}

		public void set(String name, NbtCompound nbt) {
			if (nbt.isEmpty()) {
				map.remove(name);
			} else {
				map.put(name, nbt);
			}

			markDirty();
		}

		public Stream<Identifier> getIds(String namespace) {
			return map.keySet().stream().map(key -> Identifier.of(namespace, key));
		}
	}
}
