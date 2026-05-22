package net.minecraft.client.resource.language;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Language;

import java.util.IllegalFormatException;
import java.util.Locale;

/**
 * Утилитарный класс для локализации строк на стороне клиента.
 * Делегирует поиск переводов текущему экземпляру {@link Language},
 * который обновляется при смене языка через {@link #setLanguage}.
 */
@Environment(EnvType.CLIENT)
public class I18n {

	private static volatile Language language = Language.getInstance();

	private I18n() {
	}

	static void setLanguage(Language language) {
		I18n.language = language;
	}

	/**
	 * Переводит ключ локализации с подстановкой аргументов через {@link String#format}.
	 * При ошибке форматирования возвращает строку с префиксом {@code "Format error: "},
	 * чтобы не скрывать проблемы с шаблонами переводов.
	 *
	 * @param key  ключ перевода
	 * @param args аргументы для подстановки в шаблон
	 * @return отформатированная строка перевода
	 */
	public static String translate(String key, Object... args) {
		String template = language.get(key);

		try {
			return String.format(Locale.ROOT, template, args);
		}
		catch (IllegalFormatException exception) {
			return "Format error: " + template;
		}
	}

	public static boolean hasTranslation(String key) {
		return language.hasTranslation(key);
	}
}
