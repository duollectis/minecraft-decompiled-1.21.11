package net.minecraft.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.Reader;

/**
 * Нестрогий парсер JSON, допускающий комментарии и другие расширения формата.
 * В отличие от {@link StrictJsonParser}, не выбрасывает исключение при нестандартном синтаксисе.
 */
public class LenientJsonParser {

	/**
	 * Парсит JSON из потока чтения в нестрогом режиме.
	 *
	 * @param reader источник JSON-данных
	 * @return разобранный JSON-элемент
	 * @throws JsonIOException при ошибке чтения
	 * @throws JsonSyntaxException при синтаксической ошибке
	 */
	public static JsonElement parse(Reader reader) throws JsonIOException, JsonSyntaxException {
		return JsonParser.parseReader(reader);
	}

	/**
	 * Парсит JSON из строки в нестрогом режиме.
	 *
	 * @param json строка с JSON-данными
	 * @return разобранный JSON-элемент
	 * @throws JsonSyntaxException при синтаксической ошибке
	 */
	public static JsonElement parse(String json) throws JsonSyntaxException {
		return JsonParser.parseString(json);
	}
}
