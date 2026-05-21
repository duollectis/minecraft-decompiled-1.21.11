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

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsText}.
 */
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
	 * To text.
	 *
	 * @param fallback fallback
	 *
	 * @return Text — результат операции
	 */
	public Text toText(Text fallback) {
		return Objects.requireNonNullElse(this.toText(), fallback);
	}

	/**
	 * To text.
	 *
	 * @return @Nullable Text — результат операции
	 */
	public @Nullable Text toText() {
		if (!I18n.hasTranslation(this.translationKey)) {
			return null;
		}
		else {
			return this.args == null ? Text.translatable(this.translationKey)
			                         : Text.translatable(this.translationKey, this.args);
		}
	}

	/**
	 * From json.
	 *
	 * @param json json
	 *
	 * @return RealmsText — результат операции
	 */
	public static RealmsText fromJson(JsonObject json) {
		String string = JsonUtils.getString("translationKey", json);
		JsonElement jsonElement = json.get("args");
		String[] strings;
		if (jsonElement != null && !jsonElement.isJsonNull()) {
			JsonArray jsonArray = jsonElement.getAsJsonArray();
			strings = new String[jsonArray.size()];

			for (int i = 0; i < jsonArray.size(); i++) {
				strings[i] = jsonArray.get(i).getAsString();
			}
		}
		else {
			strings = null;
		}

		return new RealmsText(string, strings);
	}

	@Override
	public String toString() {
		return this.translationKey;
	}
}
