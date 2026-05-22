package net.minecraft.client.realms.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.JsonUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO резервной копии мира Realms.
 * Содержит идентификатор, дату последнего изменения, размер и произвольные метаданные.
 * Поле {@link #changeList} заполняется отдельно после парсинга.
 */
@Environment(EnvType.CLIENT)
public class Backup extends ValueObject {

	private static final Logger LOGGER = LogUtils.getLogger();

	public final String backupId;
	public final Instant lastModifiedDate;
	public final long size;
	public boolean uploadedVersion;
	public final Map<String, String> metadata;
	public final Map<String, String> changeList = new HashMap<>();

	private Backup(String backupId, Instant lastModifiedDate, long size, Map<String, String> metadata) {
		this.backupId = backupId;
		this.lastModifiedDate = lastModifiedDate;
		this.size = size;
		this.metadata = metadata;
	}

	public ZonedDateTime getLastModifiedTime() {
		return ZonedDateTime.ofInstant(lastModifiedDate, ZoneId.systemDefault());
	}

	/**
	 * Парсит резервную копию из JSON-элемента ответа сервера Realms.
	 * Метаданные с null-значениями пропускаются.
	 *
	 * @param node JSON-элемент с данными резервной копии
	 * @return распарсенный объект или {@code null} при ошибке
	 */
	public static @Nullable Backup parse(JsonElement node) {
		JsonObject json = node.getAsJsonObject();

		try {
			String backupId = JsonUtils.getNullableStringOr("backupId", json, "");
			Instant lastModified = JsonUtils.getInstantOr("lastModifiedDate", json);
			long size = JsonUtils.getLongOr("size", json, 0L);
			Map<String, String> metadata = new HashMap<>();

			if (json.has("metadata")) {
				JsonObject metaJson = json.getAsJsonObject("metadata");

				for (Map.Entry<String, JsonElement> entry : metaJson.entrySet()) {
					if (!entry.getValue().isJsonNull()) {
						metadata.put(entry.getKey(), entry.getValue().getAsString());
					}
				}
			}

			return new Backup(backupId, lastModified, size, metadata);
		} catch (Exception ex) {
			LOGGER.error("Could not parse Backup", ex);
			return null;
		}
	}
}
