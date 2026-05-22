package net.minecraft.datafixer.fix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.StrictJsonParser;

import java.util.Optional;

/**
 * Утилитарный класс для создания и разбора текстовых компонентов Minecraft в формате JSON.
 * Используется в фиксах миграции данных для конвертации строк в компоненты текста.
 */
public class TextFixes {

	private static final String EMPTY_TEXT = text("");

	public static <T> Dynamic<T> text(DynamicOps<T> ops, String string) {
		return new Dynamic<>(ops, ops.createString(text(string)));
	}

	public static <T> Dynamic<T> empty(DynamicOps<T> ops) {
		return new Dynamic<>(ops, ops.createString(EMPTY_TEXT));
	}

	public static String text(String string) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("text", string);
		return JsonHelper.toSortedString(jsonObject);
	}

	public static String translate(String string) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("translate", string);
		return JsonHelper.toSortedString(jsonObject);
	}

	public static <T> Dynamic<T> translate(DynamicOps<T> ops, String key) {
		return new Dynamic<>(ops, ops.createString(translate(key)));
	}

	/**
	 * Разбирает строку как нестрогий JSON и конвертирует в текстовый компонент.
	 * Если строка является примитивом — оборачивает в {@code {"text":"..."}}.
	 * Если строка не является JSON-структурой — также оборачивает как текст.
	 */
	public static String parseLenientJson(String json) {
		if (json.isEmpty() || json.equals("null")) {
			return EMPTY_TEXT;
		}

		char firstChar = json.charAt(0);
		char lastChar = json.charAt(json.length() - 1);
		boolean looksLikeJson = firstChar == '"' && lastChar == '"'
			|| firstChar == '{' && lastChar == '}'
			|| firstChar == '[' && lastChar == ']';

		if (looksLikeJson) {
			try {
				JsonElement parsed = LenientJsonParser.parse(json);
				return parsed.isJsonPrimitive()
					? text(parsed.getAsString())
					: JsonHelper.toSortedString(parsed);
			} catch (JsonParseException ignored) {
				// Невалидный JSON — обрабатываем как обычную строку ниже
			}
		}

		return text(json);
	}

	/**
	 * Проверяет, является ли значение Dynamic корректным строгим JSON-компонентом текста.
	 */
	public static boolean isValidStrictJson(Dynamic<?> dynamic) {
		return dynamic.asString().result().filter(string -> {
			try {
				StrictJsonParser.parse(string);
				return true;
			} catch (JsonParseException ignored) {
				return false;
			}
		}).isPresent();
	}

	/**
	 * Извлекает значение поля {@code translate} из JSON-строки текстового компонента.
	 *
	 * @param json строка с JSON-компонентом текста
	 * @return ключ перевода, если поле присутствует
	 */
	public static Optional<String> getTranslate(String json) {
		try {
			JsonElement parsed = LenientJsonParser.parse(json);

			if (parsed.isJsonObject()) {
				JsonObject jsonObject = parsed.getAsJsonObject();
				JsonElement translateField = jsonObject.get("translate");

				if (translateField != null && translateField.isJsonPrimitive()) {
					return Optional.of(translateField.getAsString());
				}
			}
		} catch (JsonParseException ignored) {
			// Невалидный JSON — возвращаем пустой Optional
		}

		return Optional.empty();
	}
}
