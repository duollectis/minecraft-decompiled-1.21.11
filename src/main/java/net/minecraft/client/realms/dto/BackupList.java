package net.minecraft.client.realms.dto;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO списка резервных копий мира Realms.
 * Парсит JSON-ответ сервера и фильтрует некорректные записи.
 */
@Environment(EnvType.CLIENT)
public record BackupList(List<Backup> backups) {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Парсит список резервных копий из JSON-строки ответа сервера Realms.
	 * Некорректные записи пропускаются без прерывания парсинга.
	 *
	 * @param json JSON-строка с полем {@code backups}
	 * @return список резервных копий (может быть пустым при ошибке)
	 */
	public static BackupList parse(String json) {
		List<Backup> result = new ArrayList<>();

		try {
			JsonElement backupsElement = LenientJsonParser.parse(json).getAsJsonObject().get("backups");

			if (backupsElement.isJsonArray()) {
				for (JsonElement element : backupsElement.getAsJsonArray()) {
					Backup backup = Backup.parse(element);

					if (backup != null) {
						result.add(backup);
					}
				}
			}
		} catch (Exception ex) {
			LOGGER.error("Could not parse BackupList", ex);
		}

		return new BackupList(result);
	}
}
