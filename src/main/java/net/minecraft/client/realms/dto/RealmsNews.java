package net.minecraft.client.realms.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.JsonUtils;
import net.minecraft.util.LenientJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * DTO новостной ссылки Realms.
 * Содержит опциональный URL на страницу новостей, отображаемую в клиенте.
 */
@Environment(EnvType.CLIENT)
public record RealmsNews(@Nullable String newsLink) {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Парсит новостную ссылку из JSON-строки ответа сервера Realms.
	 *
	 * @param json JSON-строка с опциональным полем {@code newsLink}
	 * @return объект с ссылкой или {@code null}-ссылкой при ошибке
	 */
	public static RealmsNews parse(String json) {
		String newsLink = null;

		try {
			JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
			newsLink = JsonUtils.getNullableStringOr("newsLink", jsonObject, null);
		} catch (Exception ex) {
			LOGGER.error("Could not parse RealmsNews", ex);
		}

		return new RealmsNews(newsLink);
	}
}
