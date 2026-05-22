package net.minecraft.client.render.model.json;

import com.google.gson.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.AxisRotation;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * Грань элемента модели блока/предмета с UV-координатами, текстурой и параметрами отсечения.
 * Десериализуется из JSON-описания модели через {@link Deserializer}.
 */
@Environment(EnvType.CLIENT)
public record ModelElementFace(
		@Nullable Direction cullFace,
		int tintIndex,
		String textureId,
		ModelElementFace.@Nullable UV uvs,
		AxisRotation rotation
) {

	/** Значение tintIndex, означающее отсутствие окраски. */
	public static final int NO_TINT = -1;

	public static float getUValue(ModelElementFace.UV uv, AxisRotation axisRotation, int vertexIndex) {
		return uv.getUVertices(axisRotation.rotate(vertexIndex)) / 16.0F;
	}

	public static float getVValue(ModelElementFace.UV uv, AxisRotation axisRotation, int vertexIndex) {
		return uv.getVVertices(axisRotation.rotate(vertexIndex)) / 16.0F;
	}

	/**
	 * Десериализатор грани модели из JSON-объекта.
	 * Разбирает поля: texture, cullface, tintindex, uv, rotation.
	 */
	@Environment(EnvType.CLIENT)
	protected static class Deserializer implements JsonDeserializer<ModelElementFace> {

		private static final int DEFAULT_ROTATION = 0;

		public ModelElementFace deserialize(
				JsonElement jsonElement,
				Type type,
				JsonDeserializationContext jsonDeserializationContext
		) throws JsonParseException {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			Direction direction = deserializeCullFace(jsonObject);
			int tintIndex = deserializeTintIndex(jsonObject);
			String texture = deserializeTexture(jsonObject);
			ModelElementFace.UV uv = getUV(jsonObject);
			AxisRotation axisRotation = getRotation(jsonObject);
			return new ModelElementFace(direction, tintIndex, texture, uv, axisRotation);
		}

		private static int deserializeTintIndex(JsonObject jsonObject) {
			return JsonHelper.getInt(jsonObject, "tintindex", NO_TINT);
		}

		private static String deserializeTexture(JsonObject jsonObject) {
			return JsonHelper.getString(jsonObject, "texture");
		}

		private static @Nullable Direction deserializeCullFace(JsonObject jsonObject) {
			String face = JsonHelper.getString(jsonObject, "cullface", "");
			return Direction.byId(face);
		}

		private static AxisRotation getRotation(JsonObject jsonObject) {
			int degrees = JsonHelper.getInt(jsonObject, "rotation", DEFAULT_ROTATION);
			return AxisRotation.fromDegrees(degrees);
		}

		private static ModelElementFace.@Nullable UV getUV(JsonObject jsonObject) {
			if (!jsonObject.has("uv")) {
				return null;
			}

			JsonArray jsonArray = JsonHelper.getArray(jsonObject, "uv");
			if (jsonArray.size() != 4) {
				throw new JsonParseException("Expected 4 uv values, found: " + jsonArray.size());
			}

			float minU = JsonHelper.asFloat(jsonArray.get(0), "minU");
			float minV = JsonHelper.asFloat(jsonArray.get(1), "minV");
			float maxU = JsonHelper.asFloat(jsonArray.get(2), "maxU");
			float maxV = JsonHelper.asFloat(jsonArray.get(3), "maxV");
			return new ModelElementFace.UV(minU, minV, maxU, maxV);
		}
	}

	/**
	 * UV-координаты грани в пространстве текстуры (0–16).
	 * Метод {@link #getUVertices} и {@link #getVVertices} возвращают координату
	 * для конкретной вершины квада по её индексу (0–3).
	 */
	@Environment(EnvType.CLIENT)
	public record UV(float minU, float minV, float maxU, float maxV) {

		public float getUVertices(int vertexIndex) {
			return vertexIndex != 0 && vertexIndex != 1 ? maxU : minU;
		}

		public float getVVertices(int vertexIndex) {
			return vertexIndex != 0 && vertexIndex != 3 ? maxV : minV;
		}
	}
}
