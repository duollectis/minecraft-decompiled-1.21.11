package net.minecraft.client.realms.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.JsonUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
/**
 * {@code WorldTemplate}.
 */
public record WorldTemplate(
		String id,
		String name,
		String version,
		String author,
		String link,
		@Nullable String image,
		String trailer,
		String recommendedPlayers,
		WorldTemplate.WorldTemplateType type
) {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Parse.
	 *
	 * @param node node
	 *
	 * @return @Nullable WorldTemplate — результат операции
	 */
	public static @Nullable WorldTemplate parse(JsonObject node) {
		try {
			String string = JsonUtils.getNullableStringOr("type", node, null);
			return new WorldTemplate(
					JsonUtils.getNullableStringOr("id", node, ""),
					JsonUtils.getNullableStringOr("name", node, ""),
					JsonUtils.getNullableStringOr("version", node, ""),
					JsonUtils.getNullableStringOr("author", node, ""),
					JsonUtils.getNullableStringOr("link", node, ""),
					JsonUtils.getNullableStringOr("image", node, null),
					JsonUtils.getNullableStringOr("trailer", node, ""),
					JsonUtils.getNullableStringOr("recommendedPlayers", node, ""),
					string == null ? WorldTemplate.WorldTemplateType.WORLD_TEMPLATE
					               : WorldTemplate.WorldTemplateType.valueOf(string)
			);
		}
		catch (Exception var2) {
			LOGGER.error("Could not parse WorldTemplate", var2);
			return null;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code WorldTemplateType}.
	 */
	public static enum WorldTemplateType {
		WORLD_TEMPLATE,
		MINIGAME,
		ADVENTUREMAP,
		EXPERIENCE,
		INSPIRATION;
	}
}
