package net.minecraft.client.realms.dto;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.realms.util.JsonUtils;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsServerPlayerList}.
 */
public record RealmsServerPlayerList(Map<Long, List<ProfileComponent>> serverIdToPlayers) {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Parse.
	 *
	 * @param json json
	 *
	 * @return RealmsServerPlayerList — результат операции
	 */
	public static RealmsServerPlayerList parse(String json) {
		Builder<Long, List<ProfileComponent>> builder = ImmutableMap.builder();

		try {
			JsonObject jsonObject = JsonHelper.deserialize(json);
			if (JsonHelper.hasArray(jsonObject, "lists")) {
				for (JsonElement jsonElement : jsonObject.getAsJsonArray("lists")) {
					JsonObject jsonObject2 = jsonElement.getAsJsonObject();
					String string = JsonUtils.getNullableStringOr("playerList", jsonObject2, null);
					List<ProfileComponent> list;
					if (string != null) {
						JsonElement jsonElement2 = LenientJsonParser.parse(string);
						if (jsonElement2.isJsonArray()) {
							list = parsePlayers(jsonElement2.getAsJsonArray());
						}
						else {
							list = Lists.newArrayList();
						}
					}
					else {
						list = Lists.newArrayList();
					}

					builder.put(JsonUtils.getLongOr("serverId", jsonObject2, -1L), list);
				}
			}
		}
		catch (Exception var10) {
			LOGGER.error("Could not parse RealmsServerPlayerLists", var10);
		}

		return new RealmsServerPlayerList(builder.build());
	}

	private static List<ProfileComponent> parsePlayers(JsonArray jsonArray) {
		List<ProfileComponent> list = new ArrayList<>(jsonArray.size());

		for (JsonElement jsonElement : jsonArray) {
			if (jsonElement.isJsonObject()) {
				UUID uUID = JsonUtils.getUuidOr("playerId", jsonElement.getAsJsonObject(), null);
				if (uUID != null && !MinecraftClient.getInstance().uuidEquals(uUID)) {
					list.add(ProfileComponent.ofDynamic(uUID));
				}
			}
		}

		return list;
	}

	/**
	 * Get.
	 *
	 * @param serverId server id
	 *
	 * @return List — 
	 */
	public List<ProfileComponent> get(long serverId) {
		List<ProfileComponent> list = this.serverIdToPlayers.get(serverId);
		return list != null ? list : List.of();
	}
}
