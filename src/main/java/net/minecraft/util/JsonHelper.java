package net.minecraft.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

/**
 * Утилитарный класс для работы с JSON через Gson.
 * Предоставляет типобезопасные методы чтения полей из {@link JsonObject} и {@link JsonElement},
 * а также сериализацию/десериализацию с поддержкой строгого режима парсинга.
 */
public class JsonHelper {

	private static final Gson GSON = new GsonBuilder().create();

	public static boolean hasString(JsonObject object, String element) {
		return hasPrimitive(object, element) && object.getAsJsonPrimitive(element).isString();
	}

	public static boolean isString(JsonElement element) {
		return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
	}

	public static boolean hasNumber(JsonObject object, String element) {
		return hasPrimitive(object, element) && object.getAsJsonPrimitive(element).isNumber();
	}

	public static boolean isNumber(JsonElement element) {
		return element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
	}

	public static boolean hasBoolean(JsonObject object, String element) {
		return hasPrimitive(object, element) && object.getAsJsonPrimitive(element).isBoolean();
	}

	public static boolean isBoolean(JsonElement object) {
		return object.isJsonPrimitive() && object.getAsJsonPrimitive().isBoolean();
	}

	public static boolean hasArray(JsonObject object, String element) {
		return hasElement(object, element) && object.get(element).isJsonArray();
	}

	public static boolean hasJsonObject(JsonObject object, String element) {
		return hasElement(object, element) && object.get(element).isJsonObject();
	}

	public static boolean hasPrimitive(JsonObject object, String element) {
		return hasElement(object, element) && object.get(element).isJsonPrimitive();
	}

	public static boolean hasElement(@Nullable JsonObject object, String element) {
		return object != null && object.get(element) != null;
	}

	/**
	 * Возвращает элемент JSON по имени поля, выбрасывая исключение если поле отсутствует или null.
	 *
	 * @param object JSON-объект
	 * @param name имя поля
	 * @return найденный элемент
	 * @throws JsonSyntaxException если поле отсутствует или имеет значение null
	 */
	public static JsonElement getElement(JsonObject object, String name) {
		JsonElement jsonElement = object.get(name);

		if (jsonElement != null && !jsonElement.isJsonNull()) {
			return jsonElement;
		}

		throw new JsonSyntaxException("Missing field " + name);
	}

	public static String asString(JsonElement element, String name) {
		if (element.isJsonPrimitive()) {
			return element.getAsString();
		}

		throw new JsonSyntaxException("Expected " + name + " to be a string, was " + getType(element));
	}

	public static String getString(JsonObject object, String element) {
		if (object.has(element)) {
			return asString(object.get(element), element);
		}

		throw new JsonSyntaxException("Missing " + element + ", expected to find a string");
	}

	@Contract("_,_,!null->!null;_,_,null->_")
	public static @Nullable String getString(JsonObject object, String element, @Nullable String defaultStr) {
		return object.has(element) ? asString(object.get(element), element) : defaultStr;
	}

	public static RegistryEntry<Item> asItem(JsonElement element, String name) {
		if (element.isJsonPrimitive()) {
			String id = element.getAsString();
			return Registries.ITEM
				.getEntry(Identifier.of(id))
				.orElseThrow(() -> new JsonSyntaxException(
					"Expected " + name + " to be an item, was unknown string '" + id + "'"
				));
		}

		throw new JsonSyntaxException("Expected " + name + " to be an item, was " + getType(element));
	}

	public static RegistryEntry<Item> getItem(JsonObject object, String key) {
		if (object.has(key)) {
			return asItem(object.get(key), key);
		}

		throw new JsonSyntaxException("Missing " + key + ", expected to find an item");
	}

	@Contract("_,_,!null->!null;_,_,null->_")
	public static @Nullable RegistryEntry<Item> getItem(
		JsonObject object,
		String key,
		@Nullable RegistryEntry<Item> defaultValue
	) {
		return object.has(key) ? asItem(object.get(key), key) : defaultValue;
	}

	public static boolean asBoolean(JsonElement element, String name) {
		if (element.isJsonPrimitive()) {
			return element.getAsBoolean();
		}

		throw new JsonSyntaxException("Expected " + name + " to be a Boolean, was " + getType(element));
	}

	public static boolean getBoolean(JsonObject object, String element) {
		if (object.has(element)) {
			return asBoolean(object.get(element), element);
		}

		throw new JsonSyntaxException("Missing " + element + ", expected to find a Boolean");
	}

	public static boolean getBoolean(JsonObject object, String element, boolean defaultBoolean) {
		return object.has(element) ? asBoolean(object.get(element), element) : defaultBoolean;
	}

	public static double asDouble(JsonElement object, String name) {
		if (object.isJsonPrimitive() && object.getAsJsonPrimitive().isNumber()) {
			return object.getAsDouble();
		}

		throw new JsonSyntaxException("Expected " + name + " to be a Double, was " + getType(object));
	}

	public static double getDouble(JsonObject object, String element) {
		if (object.has(element)) {
			return asDouble(object.get(element), element);
		}

		throw new JsonSyntaxException("Missing " + element + ", expected to find a Double");
	}

	public static double getDouble(JsonObject object, String element, double defaultDouble) {
		return object.has(element) ? asDouble(object.get(element), element) : defaultDouble;
	}

	public static float asFloat(JsonElement element, String name) {
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			return element.getAsFloat();
		}

		throw new JsonSyntaxException("Expected " + name + " to be a Float, was " + getType(element));
	}

	public static float getFloat(JsonObject object, String element) {
		if (object.has(element)) {
			return asFloat(object.get(element), element);
		}

		throw new JsonSyntaxException("Missing " + element + ", expected to find a Float");
	}

	public static float getFloat(JsonObject object, String element, float defaultFloat) {
		return object.has(element) ? asFloat(object.get(element), element) : defaultFloat;
	}

	public static long asLong(JsonElement element, String name) {
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			return element.getAsLong();
		}

		throw new JsonSyntaxException("Expected " + name + " to be a Long, was " + getType(element));
	}

	public static long getLong(JsonObject object, String name) {
		if (object.has(name)) {
			return asLong(object.get(name), name);
		}

		throw new JsonSyntaxException("Missing " + name + ", expected to find a Long");
	}

	public static long getLong(JsonObject object, String element, long defaultLong) {
		return object.has(element) ? asLong(object.get(element), element) : defaultLong;
	}

	public static int asInt(JsonElement element, String name) {
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			return element.getAsInt();
		}

		throw new JsonSyntaxException("Expected " + name + " to be a Int, was " + getType(element));
	}

	public static int getInt(JsonObject object, String element) {
		if (object.has(element)) {
			return asInt(object.get(element), element);
		}

		throw new JsonSyntaxException("Missing " + element + ", expected to find a Int");
	}

	public static int getInt(JsonObject object, String element, int defaultInt) {
		return object.has(element) ? asInt(object.get(element), element) : defaultInt;
	}

	public static byte asByte(JsonElement element, String name) {
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			return element.getAsByte();
		}

		throw new JsonSyntaxException("Expected " + name + " to be a Byte, was " + getType(element));
	}

	public static byte getByte(JsonObject object, String element) {
		if (object.has(element)) {
			return asByte(object.get(element), element);
		}

		throw new JsonSyntaxException("Missing " + element + ", expected to find a Byte");
	}

	public static byte getByte(JsonObject object, String element, byte defaultByte) {
		return object.has(element) ? asByte(object.get(element), element) : defaultByte;
	}

	public static char asChar(JsonElement element, String name) {
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			return element.getAsCharacter();
		}

		throw new JsonSyntaxException("Expected " + name + " to be a Character, was " + getType(element));
	}

	public static char getChar(JsonObject object, String element) {
		if (object.has(element)) {
			return asChar(object.get(element), element);
		}

		throw new JsonSyntaxException("Missing " + element + ", expected to find a Character");
	}

	public static char getChar(JsonObject object, String element, char defaultChar) {
		return object.has(element) ? asChar(object.get(element), element) : defaultChar;
	}

	public static BigDecimal asBigDecimal(JsonElement element, String name) {
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			return element.getAsBigDecimal();
		}

		throw new JsonSyntaxException("Expected " + name + " to be a BigDecimal, was " + getType(element));
	}

	public static BigDecimal getBigDecimal(JsonObject object, String element) {
		if (object.has(element)) {
			return asBigDecimal(object.get(element), element);
		}

		throw new JsonSyntaxException("Missing " + element + ", expected to find a BigDecimal");
	}

	public static BigDecimal getBigDecimal(JsonObject object, String element, BigDecimal defaultBigDecimal) {
		return object.has(element) ? asBigDecimal(object.get(element), element) : defaultBigDecimal;
	}

	public static BigInteger asBigInteger(JsonElement element, String name) {
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			return element.getAsBigInteger();
		}

		throw new JsonSyntaxException("Expected " + name + " to be a BigInteger, was " + getType(element));
	}

	public static BigInteger getBigInteger(JsonObject object, String element) {
		if (object.has(element)) {
			return asBigInteger(object.get(element), element);
		}

		throw new JsonSyntaxException("Missing " + element + ", expected to find a BigInteger");
	}

	public static BigInteger getBigInteger(JsonObject object, String element, BigInteger defaultBigInteger) {
		return object.has(element) ? asBigInteger(object.get(element), element) : defaultBigInteger;
	}

	public static short asShort(JsonElement element, String name) {
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			return element.getAsShort();
		}

		throw new JsonSyntaxException("Expected " + name + " to be a Short, was " + getType(element));
	}

	public static short getShort(JsonObject object, String element) {
		if (object.has(element)) {
			return asShort(object.get(element), element);
		}

		throw new JsonSyntaxException("Missing " + element + ", expected to find a Short");
	}

	public static short getShort(JsonObject object, String element, short defaultShort) {
		return object.has(element) ? asShort(object.get(element), element) : defaultShort;
	}

	public static JsonObject asObject(JsonElement element, String name) {
		if (element.isJsonObject()) {
			return element.getAsJsonObject();
		}

		throw new JsonSyntaxException("Expected " + name + " to be a JsonObject, was " + getType(element));
	}

	public static JsonObject getObject(JsonObject object, String element) {
		if (object.has(element)) {
			return asObject(object.get(element), element);
		}

		throw new JsonSyntaxException("Missing " + element + ", expected to find a JsonObject");
	}

	@Contract("_,_,!null->!null;_,_,null->_")
	public static @Nullable JsonObject getObject(
		JsonObject object,
		String element,
		@Nullable JsonObject defaultObject
	) {
		return object.has(element) ? asObject(object.get(element), element) : defaultObject;
	}

	public static JsonArray asArray(JsonElement element, String name) {
		if (element.isJsonArray()) {
			return element.getAsJsonArray();
		}

		throw new JsonSyntaxException("Expected " + name + " to be a JsonArray, was " + getType(element));
	}

	public static JsonArray getArray(JsonObject object, String element) {
		if (object.has(element)) {
			return asArray(object.get(element), element);
		}

		throw new JsonSyntaxException("Missing " + element + ", expected to find a JsonArray");
	}

	@Contract("_,_,!null->!null;_,_,null->_")
	public static @Nullable JsonArray getArray(JsonObject object, String name, @Nullable JsonArray defaultArray) {
		return object.has(name) ? asArray(object.get(name), name) : defaultArray;
	}

	public static <T> T deserialize(
		@Nullable JsonElement element,
		String name,
		JsonDeserializationContext context,
		Class<? extends T> type
	) {
		if (element != null) {
			return context.deserialize(element, type);
		}

		throw new JsonSyntaxException("Missing " + name);
	}

	public static <T> T deserialize(
		JsonObject object,
		String element,
		JsonDeserializationContext context,
		Class<? extends T> type
	) {
		if (object.has(element)) {
			return deserialize(object.get(element), element, context, type);
		}

		throw new JsonSyntaxException("Missing " + element);
	}

	@Contract("_,_,!null,_,_->!null;_,_,null,_,_->_")
	public static <T> @Nullable T deserialize(
		JsonObject object,
		String element,
		@Nullable T defaultValue,
		JsonDeserializationContext context,
		Class<? extends T> type
	) {
		return object.has(element) ? deserialize(object.get(element), element, context, type) : defaultValue;
	}

	/**
	 * Возвращает человекочитаемое описание типа JSON-элемента для сообщений об ошибках.
	 *
	 * @param element JSON-элемент (может быть null)
	 * @return строковое описание типа элемента
	 */
	public static String getType(@Nullable JsonElement element) {
		String abbreviated = StringUtils.abbreviateMiddle(String.valueOf(element), "...", 10);

		if (element == null) {
			return "null (missing)";
		}

		if (element.isJsonNull()) {
			return "null (json)";
		}

		if (element.isJsonArray()) {
			return "an array (" + abbreviated + ")";
		}

		if (element.isJsonObject()) {
			return "an object (" + abbreviated + ")";
		}

		if (element.isJsonPrimitive()) {
			JsonPrimitive primitive = element.getAsJsonPrimitive();

			if (primitive.isNumber()) {
				return "a number (" + abbreviated + ")";
			}

			if (primitive.isBoolean()) {
				return "a boolean (" + abbreviated + ")";
			}
		}

		return abbreviated;
	}

	/**
	 * Десериализует JSON из потока в объект указанного типа.
	 * Использует строгий режим парсинга ({@link Strictness#STRICT}).
	 *
	 * @param gson экземпляр Gson
	 * @param reader источник JSON
	 * @param type целевой класс
	 * @return десериализованный объект
	 * @throws JsonParseException если данные пусты или содержат ошибки
	 */
	public static <T> T deserialize(Gson gson, Reader reader, Class<T> type) {
		try {
			JsonReader jsonReader = new JsonReader(reader);
			jsonReader.setStrictness(Strictness.STRICT);
			T object = gson.getAdapter(type).read(jsonReader);

			if (object == null) {
				throw new JsonParseException("JSON data was null or empty");
			}

			return object;
		} catch (IOException exception) {
			throw new JsonParseException(exception);
		}
	}

	/**
	 * Десериализует JSON из потока в объект по {@link TypeToken}, допуская null-результат.
	 *
	 * @param gson экземпляр Gson
	 * @param reader источник JSON
	 * @param typeToken токен типа для обобщённых типов
	 * @return десериализованный объект или null
	 * @throws JsonParseException если данные содержат ошибки
	 */
	public static <T> @Nullable T deserializeNullable(Gson gson, Reader reader, TypeToken<T> typeToken) {
		try {
			JsonReader jsonReader = new JsonReader(reader);
			jsonReader.setStrictness(Strictness.STRICT);
			return gson.getAdapter(typeToken).read(jsonReader);
		} catch (IOException exception) {
			throw new JsonParseException(exception);
		}
	}

	/**
	 * Десериализует JSON из потока в объект по {@link TypeToken}, выбрасывая исключение при null.
	 *
	 * @param gson экземпляр Gson
	 * @param reader источник JSON
	 * @param typeToken токен типа для обобщённых типов
	 * @return десериализованный объект
	 * @throws JsonParseException если данные пусты или содержат ошибки
	 */
	public static <T> T deserialize(Gson gson, Reader reader, TypeToken<T> typeToken) {
		T object = deserializeNullable(gson, reader, typeToken);

		if (object == null) {
			throw new JsonParseException("JSON data was null or empty");
		}

		return object;
	}

	public static <T> @Nullable T deserialize(Gson gson, String content, TypeToken<T> typeToken) {
		return deserializeNullable(gson, new StringReader(content), typeToken);
	}

	public static <T> T deserialize(Gson gson, String content, Class<T> type) {
		return deserialize(gson, new StringReader(content), type);
	}

	public static JsonObject deserialize(String content) {
		return deserialize(new StringReader(content));
	}

	public static JsonObject deserialize(Reader reader) {
		return deserialize(GSON, reader, JsonObject.class);
	}

	public static JsonArray deserializeArray(String content) {
		return deserializeArray(new StringReader(content));
	}

	public static JsonArray deserializeArray(Reader reader) {
		return deserialize(GSON, reader, JsonArray.class);
	}

	/**
	 * Сериализует JSON-элемент в строку с ключами, отсортированными в натуральном порядке.
	 * Используется для детерминированного сравнения JSON-структур.
	 *
	 * @param json JSON-элемент для сериализации
	 * @return строковое представление с отсортированными ключами
	 */
	public static String toSortedString(JsonElement json) {
		StringWriter stringWriter = new StringWriter();
		JsonWriter jsonWriter = new JsonWriter(stringWriter);

		try {
			writeSorted(jsonWriter, json, Comparator.naturalOrder());
		} catch (IOException exception) {
			throw new AssertionError(exception);
		}

		return stringWriter.toString();
	}

	/**
	 * Рекурсивно записывает JSON-элемент в {@link JsonWriter} с опциональной сортировкой ключей объектов.
	 *
	 * @param writer целевой JSON-writer
	 * @param json элемент для записи (null допустим)
	 * @param comparator компаратор для сортировки ключей объектов, или null для сохранения порядка
	 * @throws IOException при ошибке записи
	 */
	public static void writeSorted(
		JsonWriter writer,
		@Nullable JsonElement json,
		@Nullable Comparator<String> comparator
	) throws IOException {
		if (json == null || json.isJsonNull()) {
			writer.nullValue();
			return;
		}

		if (json.isJsonPrimitive()) {
			JsonPrimitive primitive = json.getAsJsonPrimitive();

			if (primitive.isNumber()) {
				writer.value(primitive.getAsNumber());
			} else if (primitive.isBoolean()) {
				writer.value(primitive.getAsBoolean());
			} else {
				writer.value(primitive.getAsString());
			}

			return;
		}

		if (json.isJsonArray()) {
			writer.beginArray();

			for (JsonElement element : json.getAsJsonArray()) {
				writeSorted(writer, element, comparator);
			}

			writer.endArray();
			return;
		}

		if (!json.isJsonObject()) {
			throw new IllegalArgumentException("Couldn't write " + json.getClass());
		}

		writer.beginObject();

		for (Entry<String, JsonElement> entry : sort(json.getAsJsonObject().entrySet(), comparator)) {
			writer.name(entry.getKey());
			writeSorted(writer, entry.getValue(), comparator);
		}

		writer.endObject();
	}

	private static Collection<Entry<String, JsonElement>> sort(
		Collection<Entry<String, JsonElement>> entries,
		@Nullable Comparator<String> comparator
	) {
		if (comparator == null) {
			return entries;
		}

		List<Entry<String, JsonElement>> sorted = new ArrayList<>(entries);
		sorted.sort(Entry.comparingByKey(comparator));
		return sorted;
	}

	/**
	 * Проверяет, превышает ли сериализованный JSON-элемент заданный лимит символов.
	 * Использует счётчик символов без реальной записи в буфер.
	 *
	 * @param json проверяемый элемент
	 * @param maxLength максимально допустимое количество символов
	 * @return {@code true} если элемент превышает лимит
	 */
	public static boolean isTooLarge(JsonElement json, int maxLength) {
		try {
			Streams.write(
				json,
				new JsonWriter(Streams.writerForAppendable(new CharacterCounter(maxLength)))
			);
			return false;
		} catch (IllegalStateException exception) {
			return true;
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	/**
	 * Счётчик символов, реализующий {@link Appendable}.
	 * Выбрасывает {@link IllegalStateException} при превышении лимита,
	 * что позволяет прервать сериализацию без реальной записи данных.
	 */
	static class CharacterCounter implements Appendable {

		private int length;
		private final int maxLength;

		public CharacterCounter(int maxLength) {
			this.maxLength = maxLength;
		}

		private Appendable addCharacters(int count) {
			length += count;

			if (length > maxLength) {
				throw new IllegalStateException("Character count over limit: " + length + " > " + maxLength);
			}

			return this;
		}

		@Override
		public Appendable append(CharSequence charSequence) {
			return addCharacters(charSequence.length());
		}

		@Override
		public Appendable append(CharSequence charSequence, int from, int to) {
			return addCharacters(to - from);
		}

		@Override
		public Appendable append(char c) {
			return addCharacters(1);
		}
	}
}
