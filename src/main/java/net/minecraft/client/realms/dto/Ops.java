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

@Environment(EnvType.CLIENT)
/**
 * {@code Ops}.
 */
public record Ops(Set<String> ops) {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static Ops parse(String json) {
		Set<String> set = new HashSet<>();

		try {
			JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
			JsonElement jsonElement = jsonObject.get("ops");
			if (jsonElement.isJsonArray()) {
				for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
					set.add(jsonElement2.getAsString());
				}
			}
		}
		catch (Exception var6) {
			LOGGER.error("Could not parse Ops", var6);
		}

		return new Ops(set);
	}
}
