package net.minecraft.client.realms.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO списка операторов (ops) сервера Realms.
 * Содержит множество имён игроков с правами оператора.
 */
@Environment(EnvType.CLIENT)
public record Ops(Set<String> ops) {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Парсит список операторов из JSON-строки ответа сервера Realms.
	 *
	 * @param json JSON-строка с полем {@code ops}
	 * @return множество имён операторов (может быть пустым при ошибке)
	 */
	public static Ops parse(String json) {
		Set<String> result = new HashSet<>();

		try {
			JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
			JsonElement opsElement = jsonObject.get("ops");

			if (opsElement.isJsonArray()) {
				for (JsonElement element : opsElement.getAsJsonArray()) {
					result.add(element.getAsString());
				}
			}
		} catch (Exception ex) {
			LOGGER.error("Could not parse Ops", ex);
		}

		return new Ops(result);
	}
}
