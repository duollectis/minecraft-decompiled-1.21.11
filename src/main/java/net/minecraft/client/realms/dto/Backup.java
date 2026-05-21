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
import java.util.Map.Entry;

@Environment(EnvType.CLIENT)
/**
 * {@code Backup}.
 */
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
		return ZonedDateTime.ofInstant(this.lastModifiedDate, ZoneId.systemDefault());
	}

	public static @Nullable Backup parse(JsonElement node) {
		JsonObject jsonObject = node.getAsJsonObject();

		try {
			String string = JsonUtils.getNullableStringOr("backupId", jsonObject, "");
			Instant instant = JsonUtils.getInstantOr("lastModifiedDate", jsonObject);
			long l = JsonUtils.getLongOr("size", jsonObject, 0L);
			Map<String, String> map = new HashMap<>();
			if (jsonObject.has("metadata")) {
				JsonObject jsonObject2 = jsonObject.getAsJsonObject("metadata");

				for (Entry<String, JsonElement> entry : jsonObject2.entrySet()) {
					if (!entry.getValue().isJsonNull()) {
						map.put(entry.getKey(), entry.getValue().getAsString());
					}
				}
			}

			return new Backup(string, instant, l, map);
		}
		catch (Exception var11) {
			LOGGER.error("Could not parse Backup", var11);
			return null;
		}
	}
}
