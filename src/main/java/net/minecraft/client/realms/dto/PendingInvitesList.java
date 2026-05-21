package net.minecraft.client.realms.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code PendingInvitesList}.
 */
public record PendingInvitesList(List<PendingInvite> pendingInvites) {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Parse.
	 *
	 * @param json json
	 *
	 * @return PendingInvitesList — результат операции
	 */
	public static PendingInvitesList parse(String json) {
		List<PendingInvite> list = new ArrayList<>();

		try {
			JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
			if (jsonObject.get("invites").isJsonArray()) {
				for (JsonElement jsonElement : jsonObject.get("invites").getAsJsonArray()) {
					PendingInvite pendingInvite = PendingInvite.parse(jsonElement.getAsJsonObject());
					if (pendingInvite != null) {
						list.add(pendingInvite);
					}
				}
			}
		}
		catch (Exception var6) {
			LOGGER.error("Could not parse PendingInvitesList", var6);
		}

		return new PendingInvitesList(list);
	}
}
