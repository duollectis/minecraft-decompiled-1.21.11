package net.minecraft.client.realms.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.JsonUtils;
import net.minecraft.util.LenientJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Instant;

@Environment(EnvType.CLIENT)
/**
 * {@code Subscription}.
 */
public record Subscription(Instant startDate, int daysLeft, Subscription.SubscriptionType type) {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Parse.
	 *
	 * @param json json
	 *
	 * @return Subscription — результат операции
	 */
	public static Subscription parse(String json) {
		try {
			JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
			return new Subscription(
					JsonUtils.getInstantOr("startDate", jsonObject),
					JsonUtils.getIntOr("daysLeft", jsonObject, 0),
					typeFrom(JsonUtils.getNullableStringOr("subscriptionType", jsonObject, null))
			);
		}
		catch (Exception var2) {
			LOGGER.error("Could not parse Subscription", var2);
			return new Subscription(Instant.EPOCH, 0, Subscription.SubscriptionType.NORMAL);
		}
	}

	private static Subscription.SubscriptionType typeFrom(@Nullable String subscriptionType) {
		try {
			if (subscriptionType != null) {
				return Subscription.SubscriptionType.valueOf(subscriptionType);
			}
		}
		catch (Exception var2) {
		}

		return Subscription.SubscriptionType.NORMAL;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SubscriptionType}.
	 */
	public static enum SubscriptionType {
		NORMAL,
		RECURRING;
	}
}
