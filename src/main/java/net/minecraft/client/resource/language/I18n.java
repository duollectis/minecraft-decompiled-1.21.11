package net.minecraft.client.resource.language;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Language;

import java.util.IllegalFormatException;
import java.util.Locale;

@Environment(EnvType.CLIENT)
/**
 * {@code I18n}.
 */
public class I18n {

	private static volatile Language language = Language.getInstance();

	private I18n() {
	}

	static void setLanguage(Language language) {
		I18n.language = language;
	}

	/**
	 * Translate.
	 *
	 * @param key key
	 * @param args args
	 *
	 * @return String — результат операции
	 */
	public static String translate(String key, Object... args) {
		String string = language.get(key);

		try {
			return String.format(Locale.ROOT, string, args);
		}
		catch (IllegalFormatException var4) {
			return "Format error: " + string;
		}
	}

	public static boolean hasTranslation(String key) {
		return language.hasTranslation(key);
	}
}
