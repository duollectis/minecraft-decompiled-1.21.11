package net.minecraft.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Строгий парсер JSON, требующий полного соответствия спецификации RFC 8259.
 * В отличие от {@link LenientJsonParser}, выбрасывает исключение при любых отклонениях
 * от стандарта, включая наличие данных после корневого элемента.
 */
public class StrictJsonParser {

	/**
	 * Парсит JSON из потока чтения в строгом режиме.
	 * Выбрасывает исключение если документ содержит данные после корневого элемента.
	 *
	 * @param reader источник JSON-данных
	 * @return разобранный JSON-элемент
	 * @throws JsonIOException при ошибке чтения
	 * @throws JsonSyntaxException при синтаксической ошибке или лишних данных
	 */
	public static JsonElement parse(Reader reader) throws JsonIOException, JsonSyntaxException {
		try {
			JsonReader jsonReader = new JsonReader(reader);
			jsonReader.setStrictness(Strictness.STRICT);
			JsonElement jsonElement = JsonParser.parseReader(jsonReader);

			if (!jsonElement.isJsonNull() && jsonReader.peek() != JsonToken.END_DOCUMENT) {
				throw new JsonSyntaxException("Did not consume the entire document.");
			}

			return jsonElement;
		} catch (NumberFormatException | MalformedJsonException exception) {
			throw new JsonSyntaxException(exception);
		} catch (IOException exception) {
			throw new JsonIOException(exception);
		}
	}

	/**
	 * Парсит JSON из строки в строгом режиме.
	 *
	 * @param json строка с JSON-данными
	 * @return разобранный JSON-элемент
	 * @throws JsonSyntaxException при синтаксической ошибке
	 */
	public static JsonElement parse(String json) throws JsonSyntaxException {
		return parse(new StringReader(json));
	}
}
