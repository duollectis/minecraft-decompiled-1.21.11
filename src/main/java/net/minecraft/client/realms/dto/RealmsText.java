package net.minecraft.client.realms.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.JsonUtils;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Локализуемый текст Realms, хранящий ключ перевода и опциональные аргументы.
 * Если перевод для ключа отсутствует в текущей локали, {@link #toText()} возвращает {@code null},
 * что позволяет вызывающему коду использовать запасной текст через {@link #toText(Text)}.
 */
@Environment(EnvType.CLIENT)
public class RealmsText {

	private static final String TRANSLATION_KEY_KEY = "translationKey";
	private static final String ARGS_KEY = "args";

	private final String translationKey;
	private final String @Nullable [] args;

	private RealmsText(String translationKey, String @Nullable [] args) {
		this.translationKey = translationKey;
		this.args = args;
	}

	/**
	 * Возвращает переведённый текст или {@code fallback}, если перевод недоступен.
	 *
	 * @param fallback запасной текст при отсутствии перевода
	 * @return переведённый текст или fallback
	 */
	public Text toText(Text fallback) {
		return Objects.requireNonNullElse(toText(), fallback);
	}

	/**
	 * Возвращает переведённый текст или {@code null}, если ключ перевода отсутствует в текущей локали.
	 *
	 * @return переведённый текст или {@code null}
	 */
	public @Nullable Text toText() {
		if (!I18n.hasTranslation(translationKey)) {
			return null;
		}

		return args == null
				? Text.translatable(translationKey)
				: Text.translatable(translationKey, (Object[]) args);
	}

	/**
	 * Парсит объект {@code RealmsText} из JSON с полями {@code translationKey} и опциональным {@code args}.
	 *
	 * @param json JSON-объект с данными текста
	 * @return распарсенный объект
	 */
	public static RealmsText fromJson(JsonObject json) {
		String key = JsonUtils.getString(TRANSLATION_KEY_KEY, json);
		JsonElement argsElement = json.get(ARGS_KEY);
		String[] args = null;

		if (argsElement != null && !argsElement.isJsonNull()) {
			JsonArray argsArray = argsElement.getAsJsonArray();
			args = new String[argsArray.size()];

			for (int index = 0; index < argsArray.size(); index++) {
				args[index] = argsArray.get(index).getAsString();
			}
		}

		return new RealmsText(key, args);
	}

	@Override
	public String toString() {
		return translationKey;
	}
}
