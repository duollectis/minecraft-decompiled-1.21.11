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
 * Фабрика для создания объектов {@link GameVersion}.
 * <p>
 * Основной метод {@link #create()} читает версию из ресурса {@code /version.json}.
 * Если файл отсутствует или повреждён, используется заглушка {@link #DEVELOPMENT}.
 */
public class MinecraftVersion {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Версия-заглушка для разработки с случайным UUID в качестве идентификатора.
	 */
	public static final GameVersion DEVELOPMENT = create(
		UUID.randomUUID().toString().replaceAll("-", ""),
		"Development Version"
	);

	public static GameVersion create(String id, String name) {
		return create(id, name, true);
	}

	/**
	 * Создаёт версию игры с заданными параметрами и текущим временем сборки.
	 *
	 * @param id     уникальный идентификатор версии
	 * @param name   отображаемое имя версии
	 * @param stable {@code true}, если версия является стабильным релизом
	 * @return новый объект версии игры
	 */
	public static GameVersion create(String id, String name, boolean stable) {
		return new GameVersion.Impl(
			id,
			name,
			new SaveVersion(SharedConstants.WORLD_VERSION, SharedConstants.CURRENT_SERIES),
			SharedConstants.getProtocolVersion(),
			PackVersion.of(SharedConstants.RESOURCE_PACK_VERSION, SharedConstants.MIN_RESOURCE_PACK_VERSION),
			PackVersion.of(SharedConstants.DATA_PACK_VERSION, SharedConstants.MIN_DATA_PACK_VERSION),
			new Date(),
			stable
		);
	}

	private static GameVersion fromJson(JsonObject json) {
		JsonObject packVersionJson = JsonHelper.getObject(json, "pack_version");
		return new GameVersion.Impl(
			JsonHelper.getString(json, "id"),
			JsonHelper.getString(json, "name"),
			new SaveVersion(
				JsonHelper.getInt(json, "world_version"),
				JsonHelper.getString(json, "series_id", SaveVersion.MAIN_SERIES)
			),
			JsonHelper.getInt(json, "protocol_version"),
			PackVersion.of(
				JsonHelper.getInt(packVersionJson, "resource_major"),
				JsonHelper.getInt(packVersionJson, "resource_minor")
			),
			PackVersion.of(
				JsonHelper.getInt(packVersionJson, "data_major"),
				JsonHelper.getInt(packVersionJson, "data_minor")
			),
			Date.from(ZonedDateTime.parse(JsonHelper.getString(json, "build_time")).toInstant()),
			JsonHelper.getBoolean(json, "stable")
		);
	}

	/**
	 * Создаёт версию игры, читая данные из ресурса {@code /version.json}.
	 * <p>
	 * Если файл отсутствует — возвращает {@link #DEVELOPMENT}.
	 * Если файл повреждён — бросает {@link IllegalStateException}.
	 *
	 * @return версия игры из файла или заглушка для разработки
	 * @throws IllegalStateException если данные версии повреждены
	 */
	public static GameVersion create() {
		try {
			try (InputStream inputStream = MinecraftVersion.class.getResourceAsStream("/version.json")) {
				if (inputStream == null) {
					LOGGER.warn("Missing version information!");
					return DEVELOPMENT;
				}

				try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
					return fromJson(JsonHelper.deserialize(reader));
				}
			}
		} catch (JsonParseException | IOException cause) {
			throw new IllegalStateException("Game version information is corrupt", cause);
		}
	}
}
