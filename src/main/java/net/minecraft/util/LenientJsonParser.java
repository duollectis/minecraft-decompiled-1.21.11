package net.minecraft.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.Reader;

/**
 * {@code LenientJsonParser}.
 */
public class LenientJsonParser {

	/**
	 * Parse.
	 *
	 * @param reader reader
	 *
	 * @return JsonElement — результат операции
	 */
	public static JsonElement parse(Reader reader) throws JsonIOException, JsonSyntaxException {
		return JsonParser.parseReader(reader);
	}

	/**
	 * Parse.
	 *
	 * @param json json
	 *
	 * @return JsonElement — результат операции
	 */
	public static JsonElement parse(String json) throws JsonSyntaxException {
		return JsonParser.parseString(json);
	}
}
