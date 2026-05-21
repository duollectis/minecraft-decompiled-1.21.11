package net.minecraft.client.render.model.json;

import com.google.gson.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.AxisRotation;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;

@Environment(EnvType.CLIENT)
/**
 * {@code ModelElementFace}.
 */
public record ModelElementFace(
		@Nullable Direction cullFace,
		int tintIndex,
		String textureId,
		ModelElementFace.@Nullable UV uvs,
		AxisRotation rotation
) {

	public static final int NO_TINT = -1;

	public static float getUValue(ModelElementFace.UV uV, AxisRotation axisRotation, int i) {
		return uV.getUVertices(axisRotation.rotate(i)) / 16.0F;
	}

	public static float getVValue(ModelElementFace.UV uV, AxisRotation axisRotation, int i) {
		return uV.getVVertices(axisRotation.rotate(i)) / 16.0F;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Deserializer}.
	 */
	protected static class Deserializer implements JsonDeserializer<ModelElementFace> {

		private static final int DEFAULT_TINT_INDEX = -1;
		private static final int DEFAULT_ROTATION = 0;

		public ModelElementFace deserialize(
				JsonElement jsonElement,
				Type type,
				JsonDeserializationContext jsonDeserializationContext
		) throws JsonParseException {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			Direction direction = deserializeCullFace(jsonObject);
			int i = deserializeTintIndex(jsonObject);
			String string = deserializeTexture(jsonObject);
			ModelElementFace.UV uV = getUV(jsonObject);
			AxisRotation axisRotation = getRotation(jsonObject);
			return new ModelElementFace(direction, i, string, uV, axisRotation);
		}

		private static int deserializeTintIndex(JsonObject jsonObject) {
			return JsonHelper.getInt(jsonObject, "tintindex", -1);
		}

		private static String deserializeTexture(JsonObject jsonObject) {
			return JsonHelper.getString(jsonObject, "texture");
		}

		private static @Nullable Direction deserializeCullFace(JsonObject jsonObject) {
			String string = JsonHelper.getString(jsonObject, "cullface", "");
			return Direction.byId(string);
		}

		private static AxisRotation getRotation(JsonObject jsonObject) {
			int i = JsonHelper.getInt(jsonObject, "rotation", 0);
			return AxisRotation.fromDegrees(i);
		}

		private static ModelElementFace.@Nullable UV getUV(JsonObject jsonObject) {
			if (!jsonObject.has("uv")) {
				return null;
			}
			else {
				JsonArray jsonArray = JsonHelper.getArray(jsonObject, "uv");
				if (jsonArray.size() != 4) {
					throw new JsonParseException("Expected 4 uv values, found: " + jsonArray.size());
				}
				else {
					float f = JsonHelper.asFloat(jsonArray.get(0), "minU");
					float g = JsonHelper.asFloat(jsonArray.get(1), "minV");
					float h = JsonHelper.asFloat(jsonArray.get(2), "maxU");
					float i = JsonHelper.asFloat(jsonArray.get(3), "maxV");
					return new ModelElementFace.UV(f, g, h, i);
				}
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code UV}.
	 */
	public record UV(float minU, float minV, float maxU, float maxV) {

		public float getUVertices(int i) {
			return i != 0 && i != 1 ? this.maxU : this.minU;
		}

		public float getVVertices(int i) {
			return i != 0 && i != 3 ? this.maxV : this.minV;
		}
	}
}
