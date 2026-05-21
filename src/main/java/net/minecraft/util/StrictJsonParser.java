package net.minecraft.util;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * {@code StrictJsonParser}.
 */
public class StrictJsonParser {

	/**
	 * Parse.
	 *
	 * @param reader reader
	 *
	 * @return JsonElement — результат операции
	 */
	public static JsonElement parse(Reader reader) throws JsonIOException, JsonSyntaxException {
		try {
			JsonReader jsonReader = new JsonReader(reader);
			jsonReader.setStrictness(Strictness.STRICT);
			JsonElement jsonElement = JsonParser.parseReader(jsonReader);
			if (!jsonElement.isJsonNull() && jsonReader.peek() != JsonToken.END_DOCUMENT) {
				throw new JsonSyntaxException("Did not consume the entire document.");
			}
			else {
				return jsonElement;
			}
		}
		catch (NumberFormatException | MalformedJsonException var3) {
			throw new JsonSyntaxException(var3);
		}
		catch (IOException var4) {
			throw new JsonIOException(var4);
		}
	}

	/**
	 * Parse.
	 *
	 * @param json json
	 *
	 * @return JsonElement — результат операции
	 */
	public static JsonElement parse(String json) throws JsonSyntaxException {
		return parse(new StringReader(json));
	}
}
