package net.minecraft.client.realms.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.JsonUtils;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
/**
 * {@code WorldDownload}.
 */
public record WorldDownload(String downloadLink, String resourcePackUrl, String resourcePackHash) {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Parse.
	 *
	 * @param json json
	 *
	 * @return WorldDownload — результат операции
	 */
	public static WorldDownload parse(String json) {
		JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();

		try {
			return new WorldDownload(
					JsonUtils.getNullableStringOr("downloadLink", jsonObject, ""),
					JsonUtils.getNullableStringOr("resourcePackUrl", jsonObject, ""),
					JsonUtils.getNullableStringOr("resourcePackHash", jsonObject, "")
			);
		}
		catch (Exception var3) {
			LOGGER.error("Could not parse WorldDownload", var3);
			return new WorldDownload("", "", "");
		}
	}
}
