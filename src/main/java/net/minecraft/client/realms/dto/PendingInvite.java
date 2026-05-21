package net.minecraft.client.realms.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.JsonUtils;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.UUID;

@Environment(EnvType.CLIENT)
/**
 * {@code PendingInvite}.
 */
public record PendingInvite(
		String invitationId,
		String worldName,
		String worldOwnerName,
		UUID worldOwnerUuid,
		Instant date
) {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static @Nullable PendingInvite parse(JsonObject json) {
		try {
			return new PendingInvite(
					JsonUtils.getNullableStringOr("invitationId", json, ""),
					JsonUtils.getNullableStringOr("worldName", json, ""),
					JsonUtils.getNullableStringOr("worldOwnerName", json, ""),
					JsonUtils.getUuidOr("worldOwnerUuid", json, Util.NIL_UUID),
					JsonUtils.getInstantOr("date", json)
			);
		}
		catch (Exception var2) {
			LOGGER.error("Could not parse PendingInvite", var2);
			return null;
		}
	}
}
