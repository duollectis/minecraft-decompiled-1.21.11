package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.TextVisitFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/**
 * Абстрактный провайдер локализации, управляющий переводами строк интерфейса.
 * По умолчанию загружает английский язык ({@code en_us}) из ресурсов.
 * Поддерживает замену устаревших ключей через {@link DeprecatedLanguageData}.
 */
public abstract class Language {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new Gson();

	/**
	 * Паттерн для замены числовых форматных спецификаторов (%d, %f) на строковые (%s).
	 * Это необходимо, так как Java-форматирование строк использует %s для всех типов в переводах.
	 */
	private static final Pattern TOKEN_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");

	public static final String DEFAULT_LANGUAGE = "en_us";

	private static volatile Language instance = create();

	private static Language create() {
		DeprecatedLanguageData deprecatedLanguageData = DeprecatedLanguageData.create();
		Map<String, String> translations = new HashMap<>();

		load(translations::put, "/assets/minecraft/lang/en_us.json");
		deprecatedLanguageData.apply(translations);

		final Map<String, String> immutableTranslations = Map.copyOf(translations);

		return new Language() {
			@Override
			public String get(String key, String fallback) {
				return immutableTranslations.getOrDefault(key, fallback);
			}

			@Override
			public boolean hasTranslation(String key) {
				return immutableTranslations.containsKey(key);
			}

			@Override
			public boolean isRightToLeft() {
				return false;
			}

			@Override
			public OrderedText reorder(StringVisitable text) {
				return visitor -> text.visit(
					(style, string) -> TextVisitFactory.visitFormatted(string, style, visitor)
						? Optional.empty()
						: StringVisitable.TERMINATE_VISIT,
					Style.EMPTY
				).isPresent();
			}
		};
	}

	private static void load(BiConsumer<String, String> entryConsumer, String path) {
		try (InputStream inputStream = Language.class.getResourceAsStream(path)) {
			load(inputStream, entryConsumer);
		} catch (JsonParseException | IOException exception) {
			LOGGER.error("Couldn't read strings from {}", path, exception);
		}
	}

	/**
	 * Загружает переводы из JSON-потока, заменяя числовые форматные спецификаторы на строковые.
	 * Формат файла: плоский JSON-объект с ключами перевода и строковыми значениями.
	 *
	 * @param inputStream поток с JSON-данными переводов
	 * @param entryConsumer получатель пар ключ-значение переводов
	 */
	public static void load(InputStream inputStream, BiConsumer<String, String> entryConsumer) {
		JsonObject jsonObject = GSON.fromJson(
			new InputStreamReader(inputStream, StandardCharsets.UTF_8),
			JsonObject.class
		);

		for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String normalized = TOKEN_PATTERN
				.matcher(JsonHelper.asString(entry.getValue(), entry.getKey()))
				.replaceAll("%$1s");

			entryConsumer.accept(entry.getKey(), normalized);
		}
	}

	public static Language getInstance() {
		return instance;
	}

	public static void setInstance(Language language) {
		instance = language;
	}

	/**
	 * Возвращает перевод по ключу, используя сам ключ как запасное значение.
	 *
	 * @param key ключ перевода
	 * @return переведённая строка или ключ, если перевод не найден
	 */
	public String get(String key) {
		return get(key, key);
	}

	/**
	 * Возвращает перевод по ключу с явным запасным значением.
	 *
	 * @param key ключ перевода
	 * @param fallback значение, возвращаемое если перевод не найден
	 * @return переведённая строка или {@code fallback}
	 */
	public abstract String get(String key, String fallback);

	public abstract boolean hasTranslation(String key);

	public abstract boolean isRightToLeft();

	/**
	 * Преобразует {@link StringVisitable} в {@link OrderedText} с учётом направления текста.
	 *
	 * @param text текст для преобразования
	 * @return упорядоченный текст для рендеринга
	 */
	public abstract OrderedText reorder(StringVisitable text);

	public List<OrderedText> reorder(List<StringVisitable> texts) {
		return texts.stream()
			.map(this::reorder)
			.collect(ImmutableList.toImmutableList());
	}
}
