package net.minecraft.client.realms.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.util.UndashedUuid;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
/**
 * {@code JsonUtils}.
 */
public class JsonUtils {

	public static <T> T get(String key, JsonObject node, Function<JsonObject, T> deserializer) {
		JsonElement jsonElement = node.get(key);
		if (jsonElement == null || jsonElement.isJsonNull()) {
			throw new IllegalStateException("Missing required property: " + key);
		}
		else if (!jsonElement.isJsonObject()) {
			throw new IllegalStateException("Required property " + key + " was not a JsonObject as espected");
		}
		else {
			return deserializer.apply(jsonElement.getAsJsonObject());
		}
	}

	public static <T> @Nullable T getNullable(String key, JsonObject node, Function<JsonObject, T> deserializer) {
		JsonElement jsonElement = node.get(key);
		if (jsonElement == null || jsonElement.isJsonNull()) {
			return null;
		}
		else if (!jsonElement.isJsonObject()) {
			throw new IllegalStateException("Required property " + key + " was not a JsonObject as espected");
		}
		else {
			return deserializer.apply(jsonElement.getAsJsonObject());
		}
	}

	public static String getString(String key, JsonObject node) {
		String string = getNullableStringOr(key, node, null);
		if (string == null) {
			throw new IllegalStateException("Missing required property: " + key);
		}
		else {
			return string;
		}
	}

	@Contract("_,_,!null->!null;_,_,null->_")
	public static @Nullable String getNullableStringOr(String key, JsonObject node, @Nullable String defaultValue) {
		JsonElement jsonElement = node.get(key);
		if (jsonElement != null) {
			return jsonElement.isJsonNull() ? defaultValue : jsonElement.getAsString();
		}
		else {
			return defaultValue;
		}
	}

	@Contract("_,_,!null->!null;_,_,null->_")
	public static @Nullable UUID getUuidOr(String key, JsonObject node, @Nullable UUID defaultValue) {
		String string = getNullableStringOr(key, node, null);
		return string == null ? defaultValue : UndashedUuid.fromStringLenient(string);
	}

	public static int getIntOr(String key, JsonObject node, int defaultValue) {
		JsonElement jsonElement = node.get(key);
		if (jsonElement != null) {
			return jsonElement.isJsonNull() ? defaultValue : jsonElement.getAsInt();
		}
		else {
			return defaultValue;
		}
	}

	public static long getLongOr(String key, JsonObject node, long defaultValue) {
		JsonElement jsonElement = node.get(key);
		if (jsonElement != null) {
			return jsonElement.isJsonNull() ? defaultValue : jsonElement.getAsLong();
		}
		else {
			return defaultValue;
		}
	}

	public static boolean getBooleanOr(String key, JsonObject node, boolean defaultValue) {
		JsonElement jsonElement = node.get(key);
		if (jsonElement != null) {
			return jsonElement.isJsonNull() ? defaultValue : jsonElement.getAsBoolean();
		}
		else {
			return defaultValue;
		}
	}

	public static Instant getInstantOr(String key, JsonObject node) {
		JsonElement jsonElement = node.get(key);
		return jsonElement != null ? Instant.ofEpochMilli(Long.parseLong(jsonElement.getAsString())) : Instant.EPOCH;
	}
}
