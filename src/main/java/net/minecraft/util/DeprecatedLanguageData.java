package net.minecraft.util;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Данные об устаревших ключах локализации: удалённых и переименованных.
 * <p>
 * Загружается из {@code /assets/minecraft/lang/deprecated.json} и применяется
 * к карте переводов при загрузке языкового файла, чтобы удалить устаревшие ключи
 * и переименовать перемещённые.
 *
 * @param removed  список ключей, которые были полностью удалены
 * @param renamed  карта переименований: старый ключ → новый ключ
 */
public record DeprecatedLanguageData(List<String> removed, Map<String, String> renamed) {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String DEPRECATED_LANG_PATH = "/assets/minecraft/lang/deprecated.json";

	public static final DeprecatedLanguageData NONE = new DeprecatedLanguageData(List.of(), Map.of());
	public static final Codec<DeprecatedLanguageData> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Codec.STRING.listOf().fieldOf("removed").forGetter(DeprecatedLanguageData::removed),
			Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("renamed").forGetter(DeprecatedLanguageData::renamed)
		).apply(instance, DeprecatedLanguageData::new)
	);

	/**
	 * Десериализует данные об устаревших ключах из входного потока JSON.
	 *
	 * @param stream входной поток с JSON-данными
	 * @return десериализованные данные
	 * @throws IllegalStateException если JSON не соответствует ожидаемой схеме
	 */
	public static DeprecatedLanguageData fromInputStream(InputStream stream) {
		JsonElement json = StrictJsonParser.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
		return CODEC.parse(JsonOps.INSTANCE, json)
			.getOrThrow(error -> new IllegalStateException("Failed to parse deprecated language data: " + error));
	}

	/**
	 * Загружает данные об устаревших ключах из ресурса по указанному пути.
	 * При отсутствии файла или ошибке чтения возвращает {@link #NONE}.
	 *
	 * @param path путь к ресурсу в classpath
	 * @return загруженные данные или {@link #NONE} при ошибке
	 */
	public static DeprecatedLanguageData fromPath(String path) {
		try (InputStream inputStream = Language.class.getResourceAsStream(path)) {
			return inputStream != null ? fromInputStream(inputStream) : NONE;
		} catch (Exception exception) {
			LOGGER.error("Failed to read {}", path, exception);
			return NONE;
		}
	}

	/** @return данные об устаревших ключах из стандартного пути в ресурсах Minecraft */
	public static DeprecatedLanguageData create() {
		return fromPath(DEPRECATED_LANG_PATH);
	}

	/**
	 * Применяет данные об устаревших ключах к карте переводов:
	 * удаляет ключи из {@link #removed} и переименовывает ключи из {@link #renamed}.
	 *
	 * @param translations изменяемая карта переводов
	 */
	public void apply(Map<String, String> translations) {
		for (String removedKey : removed) {
			translations.remove(removedKey);
		}

		renamed.forEach((oldKey, newKey) -> {
			String value = translations.remove(oldKey);

			if (value == null) {
				LOGGER.warn("Missing translation key for rename: {}", oldKey);
				translations.remove(newKey);
			} else {
				translations.put(newKey, value);
			}
		});
	}
}
