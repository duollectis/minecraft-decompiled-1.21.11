package net.minecraft.client.realms.dto;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code BackupList}.
 */
public record BackupList(List<Backup> backups) {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Parse.
	 *
	 * @param json json
	 *
	 * @return BackupList — результат операции
	 */
	public static BackupList parse(String json) {
		List<Backup> list = new ArrayList<>();

		try {
			JsonElement jsonElement = LenientJsonParser.parse(json).getAsJsonObject().get("backups");
			if (jsonElement.isJsonArray()) {
				for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
					Backup backup = Backup.parse(jsonElement2);
					if (backup != null) {
						list.add(backup);
					}
				}
			}
		}
		catch (Exception var6) {
			LOGGER.error("Could not parse BackupList", var6);
		}

		return new BackupList(list);
	}
}
