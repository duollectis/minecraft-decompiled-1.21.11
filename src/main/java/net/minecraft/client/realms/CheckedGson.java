package net.minecraft.client.realms;

import com.google.gson.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.DontSerialize;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code CheckedGson}.
 */
public class CheckedGson {

	ExclusionStrategy EXCLUSION_STRATEGY = new ExclusionStrategy() {
		/**
		 * Определяет, следует ли skip class.
		 *
		 * @param clazz clazz
		 *
		 * @return boolean — результат операции
		 */
		public boolean shouldSkipClass(Class<?> clazz) {
			return false;
		}

		/**
		 * Определяет, следует ли skip field.
		 *
		 * @param fieldAttributes field attributes
		 *
		 * @return boolean — результат операции
		 */
		public boolean shouldSkipField(FieldAttributes fieldAttributes) {
			return fieldAttributes.getAnnotation(DontSerialize.class) != null;
		}
	};
	private final Gson GSON = new GsonBuilder()
			.addSerializationExclusionStrategy(this.EXCLUSION_STRATEGY)
			.addDeserializationExclusionStrategy(this.EXCLUSION_STRATEGY)
			.create();

	/**
	 * To json.
	 *
	 * @param serializable serializable
	 *
	 * @return String — результат операции
	 */
	public String toJson(RealmsSerializable serializable) {
		return this.GSON.toJson(serializable);
	}

	/**
	 * To json.
	 *
	 * @param json json
	 *
	 * @return String — результат операции
	 */
	public String toJson(JsonElement json) {
		return this.GSON.toJson(json);
	}

	/**
	 * From json.
	 *
	 * @param json json
	 * @param type type
	 *
	 * @return @Nullable T — результат операции
	 */
	public <T extends RealmsSerializable> @Nullable T fromJson(String json, Class<T> type) {
		return (T) this.GSON.fromJson(json, type);
	}
}
