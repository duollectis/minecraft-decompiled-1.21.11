package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.server.dedicated.management.listener.ManagementListener;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Базовый класс для JSON-конфигурационных списков сервера (бан-лист, операторы, белый список).
 * Хранит записи в памяти в виде {@link Map} и синхронизирует их с файлом на диске при каждом изменении.
 *
 * @param <K> тип ключа записи
 * @param <V> тип записи, расширяющей {@link ServerConfigEntry}
 */
public abstract class ServerConfigList<K, V extends ServerConfigEntry<K>> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final File file;
	private final Map<String, V> map = Maps.newHashMap();
	protected final ManagementListener managementListener;

	public ServerConfigList(File file, ManagementListener managementListener) {
		this.file = file;
		this.managementListener = managementListener;
	}

	public File getFile() {
		return file;
	}

	public boolean add(V entry) {
		String key = toString(entry.getKey());
		V existing = map.get(key);
		if (entry.equals(existing)) {
			return false;
		}

		map.put(key, entry);

		try {
			save();
		}
		catch (IOException exception) {
			LOGGER.warn("Could not save the list after adding a user.", exception);
		}

		return true;
	}

	public @Nullable V get(K key) {
		removeInvalidEntries();
		return map.get(toString(key));
	}

	public boolean remove(K key) {
		V removed = map.remove(toString(key));
		if (removed == null) {
			return false;
		}

		try {
			save();
		}
		catch (IOException exception) {
			LOGGER.warn("Could not save the list after removing a user.", exception);
		}

		return true;
	}

	public boolean remove(ServerConfigEntry<K> entry) {
		return remove(Objects.requireNonNull(entry.getKey()));
	}

	public void clear() {
		map.clear();

		try {
			save();
		}
		catch (IOException exception) {
			LOGGER.warn("Could not save the list after removing a user.", exception);
		}
	}

	public String[] getNames() {
		return map.keySet().toArray(new String[0]);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	protected String toString(K profile) {
		return profile.toString();
	}

	protected boolean contains(K object) {
		return map.containsKey(toString(object));
	}

	private void removeInvalidEntries() {
		List<K> invalid = Lists.newArrayList();

		for (V entry : map.values()) {
			if (entry.isInvalid()) {
				invalid.add(entry.getKey());
			}
		}

		for (K key : invalid) {
			map.remove(toString(key));
		}
	}

	protected abstract ServerConfigEntry<K> fromJson(JsonObject json);

	public Collection<V> values() {
		return map.values();
	}

	/**
	 * Сохраняет все записи в JSON-файл на диске.
	 * Вызывается автоматически при каждом изменении списка.
	 */
	public void save() throws IOException {
		JsonArray jsonArray = new JsonArray();
		map.values().stream().map(entry -> Util.make(new JsonObject(), entry::write)).forEach(jsonArray::add);

		try (BufferedWriter writer = Files.newWriter(file, StandardCharsets.UTF_8)) {
			GSON.toJson(jsonArray, GSON.newJsonWriter(writer));
		}
	}

	/**
	 * Загружает записи из JSON-файла с диска.
	 * Если файл не существует — ничего не делает.
	 */
	public void load() throws IOException {
		if (!file.exists()) {
			return;
		}

		try (BufferedReader reader = Files.newReader(file, StandardCharsets.UTF_8)) {
			map.clear();
			JsonArray jsonArray = (JsonArray) GSON.fromJson(reader, JsonArray.class);
			if (jsonArray == null) {
				return;
			}

			for (JsonElement element : jsonArray) {
				JsonObject jsonObject = JsonHelper.asObject(element, "entry");
				ServerConfigEntry<K> entry = fromJson(jsonObject);
				if (entry.getKey() != null) {
					map.put(toString(entry.getKey()), (V) entry);
				}
			}
		}
	}
}
