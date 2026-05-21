package net.minecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resource.PackVersion;
import net.minecraft.util.JsonHelper;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

/**
 * {@code MinecraftVersion}.
 */
public class MinecraftVersion {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final GameVersion
			DEVELOPMENT =
			create(UUID.randomUUID().toString().replaceAll("-", ""), "Development Version");

	public static GameVersion create(String id, String name) {
		return create(id, name, true);
	}

	public static GameVersion create(String id, String name, boolean stable) {
		return new GameVersion.Impl(
				id,
				name,
				new SaveVersion(4671, "main"),
				SharedConstants.getProtocolVersion(),
				PackVersion.of(75, 0),
				PackVersion.of(94, 1),
				new Date(),
				stable
		);
	}

	private static GameVersion fromJson(JsonObject json) {
		JsonObject jsonObject = JsonHelper.getObject(json, "pack_version");
		return new GameVersion.Impl(
				JsonHelper.getString(json, "id"),
				JsonHelper.getString(json, "name"),
				new SaveVersion(
						JsonHelper.getInt(json, "world_version"),
						JsonHelper.getString(json, "series_id", "main")
				),
				JsonHelper.getInt(json, "protocol_version"),
				PackVersion.of(
						JsonHelper.getInt(jsonObject, "resource_major"),
						JsonHelper.getInt(jsonObject, "resource_minor")
				),
				PackVersion.of(
						JsonHelper.getInt(jsonObject, "data_major"),
						JsonHelper.getInt(jsonObject, "data_minor")
				),
				Date.from(ZonedDateTime.parse(JsonHelper.getString(json, "build_time")).toInstant()),
				JsonHelper.getBoolean(json, "stable")
		);
	}

	public static GameVersion create() {
		try {
			GameVersion var2;
			try (InputStream inputStream = MinecraftVersion.class.getResourceAsStream("/version.json")) {
				if (inputStream == null) {
					LOGGER.warn("Missing version information!");
					return DEVELOPMENT;
				}

				try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
					var2 = fromJson(JsonHelper.deserialize(inputStreamReader));
				}
			}

			return var2;
		}
		catch (JsonParseException | IOException var8) {
			throw new IllegalStateException("Game version information is corrupt", var8);
		}
	}
}
