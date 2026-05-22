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

/**
 * Утилитарный класс для безопасного извлечения типизированных значений из {@link JsonObject}.
 * <p>
 * Все методы поддерживают дефолтные значения при отсутствии ключа или {@code null}-значении.
 * Методы с суффиксом {@code Or} возвращают переданный дефолт вместо выброса исключения.
 */
@Environment(EnvType.CLIENT)
public class JsonUtils {

	/**
	 * Извлекает обязательный вложенный объект по ключу и десериализует его.
	 * Выбрасывает {@link IllegalStateException}, если ключ отсутствует или значение не является объектом.
	 *
	 * @param key          ключ в JSON-объекте
	 * @param node         родительский JSON-объект
	 * @param deserializer функция десериализации вложенного объекта
	 * @param <T>          тип результата
	 * @return десериализованное значение
	 */
	public static <T> T get(String key, JsonObject node, Function<JsonObject, T> deserializer) {
		JsonElement element = node.get(key);

		if (element == null || element.isJsonNull()) {
			throw new IllegalStateException("Missing required property: " + key);
		}

		if (!element.isJsonObject()) {
			throw new IllegalStateException("Required property " + key + " was not a JsonObject as expected");
		}

		return deserializer.apply(element.getAsJsonObject());
	}

	/**
	 * Извлекает необязательный вложенный объект по ключу и десериализует его.
	 * Возвращает {@code null}, если ключ отсутствует или значение равно {@code null}.
	 *
	 * @param key          ключ в JSON-объекте
	 * @param node         родительский JSON-объект
	 * @param deserializer функция десериализации вложенного объекта
	 * @param <T>          тип результата
	 * @return десериализованное значение или {@code null}
	 */
	public static <T> @Nullable T getNullable(String key, JsonObject node, Function<JsonObject, T> deserializer) {
		JsonElement element = node.get(key);

		if (element == null || element.isJsonNull()) {
			return null;
		}

		if (!element.isJsonObject()) {
			throw new IllegalStateException("Required property " + key + " was not a JsonObject as expected");
		}

		return deserializer.apply(element.getAsJsonObject());
	}

	public static String getString(String key, JsonObject node) {
		String value = getNullableStringOr(key, node, null);

		if (value == null) {
			throw new IllegalStateException("Missing required property: " + key);
		}

		return value;
	}

	@Contract("_,_,!null->!null;_,_,null->_")
	public static @Nullable String getNullableStringOr(String key, JsonObject node, @Nullable String defaultValue) {
		JsonElement element = node.get(key);

		if (element == null) {
			return defaultValue;
		}

		return element.isJsonNull() ? defaultValue : element.getAsString();
	}

	@Contract("_,_,!null->!null;_,_,null->_")
	public static @Nullable UUID getUuidOr(String key, JsonObject node, @Nullable UUID defaultValue) {
		String value = getNullableStringOr(key, node, null);
		return value == null ? defaultValue : UndashedUuid.fromStringLenient(value);
	}

	public static int getIntOr(String key, JsonObject node, int defaultValue) {
		JsonElement element = node.get(key);

		if (element == null) {
			return defaultValue;
		}

		return element.isJsonNull() ? defaultValue : element.getAsInt();
	}

	public static long getLongOr(String key, JsonObject node, long defaultValue) {
		JsonElement element = node.get(key);

		if (element == null) {
			return defaultValue;
		}

		return element.isJsonNull() ? defaultValue : element.getAsLong();
	}

	public static boolean getBooleanOr(String key, JsonObject node, boolean defaultValue) {
		JsonElement element = node.get(key);

		if (element == null) {
			return defaultValue;
		}

		return element.isJsonNull() ? defaultValue : element.getAsBoolean();
	}

	public static Instant getInstantOr(String key, JsonObject node) {
		JsonElement element = node.get(key);
		return element != null
			? Instant.ofEpochMilli(Long.parseLong(element.getAsString()))
			: Instant.EPOCH;
	}
}
