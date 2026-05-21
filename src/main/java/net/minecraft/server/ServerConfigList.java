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
 * {@code ServerConfigList}.
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
		return this.file;
	}

	public boolean add(V serverConfigEntry) {
		String string = this.toString(serverConfigEntry.getKey());
		V serverConfigEntry2 = this.map.get(string);
		if (serverConfigEntry.equals(serverConfigEntry2)) {
			return false;
		}
		else {
			this.map.put(string, serverConfigEntry);

			try {
				this.save();
			}
			catch (IOException var5) {
				LOGGER.warn("Could not save the list after adding a user.", var5);
			}

			return true;
		}
	}

	public @Nullable V get(K key) {
		this.removeInvalidEntries();
		return this.map.get(this.toString(key));
	}

	public boolean remove(K key) {
		V serverConfigEntry = this.map.remove(this.toString(key));
		if (serverConfigEntry == null) {
			return false;
		}
		else {
			try {
				this.save();
			}
			catch (IOException var4) {
				LOGGER.warn("Could not save the list after removing a user.", var4);
			}

			return true;
		}
	}

	public boolean remove(ServerConfigEntry<K> entry) {
		return this.remove(Objects.requireNonNull(entry.getKey()));
	}

	public void clear() {
		this.map.clear();

		try {
			this.save();
		}
		catch (IOException var2) {
			LOGGER.warn("Could not save the list after removing a user.", var2);
		}
	}

	public String[] getNames() {
		return this.map.keySet().toArray(new String[0]);
	}

	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	protected String toString(K profile) {
		return profile.toString();
	}

	protected boolean contains(K object) {
		return this.map.containsKey(this.toString(object));
	}

	private void removeInvalidEntries() {
		List<K> list = Lists.newArrayList();

		for (V serverConfigEntry : this.map.values()) {
			if (serverConfigEntry.isInvalid()) {
				list.add(serverConfigEntry.getKey());
			}
		}

		for (K object : list) {
			this.map.remove(this.toString(object));
		}
	}

	protected abstract ServerConfigEntry<K> fromJson(JsonObject json);

	public Collection<V> values() {
		return this.map.values();
	}

	public void save() throws IOException {
		JsonArray jsonArray = new JsonArray();
		this.map.values().stream().map(entry -> Util.make(new JsonObject(), entry::write)).forEach(jsonArray::add);

		try (BufferedWriter bufferedWriter = Files.newWriter(this.file, StandardCharsets.UTF_8)) {
			GSON.toJson(jsonArray, GSON.newJsonWriter(bufferedWriter));
		}
	}

	public void load() throws IOException {
		if (this.file.exists()) {
			try (BufferedReader bufferedReader = Files.newReader(this.file, StandardCharsets.UTF_8)) {
				this.map.clear();
				JsonArray jsonArray = (JsonArray) GSON.fromJson(bufferedReader, JsonArray.class);
				if (jsonArray == null) {
					return;
				}

				for (JsonElement jsonElement : jsonArray) {
					JsonObject jsonObject = JsonHelper.asObject(jsonElement, "entry");
					ServerConfigEntry<K> serverConfigEntry = this.fromJson(jsonObject);
					if (serverConfigEntry.getKey() != null) {
						this.map.put(this.toString(serverConfigEntry.getKey()), (V) serverConfigEntry);
					}
				}
			}
		}
	}
}
