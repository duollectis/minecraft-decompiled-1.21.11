package net.minecraft.client.render.model.json;

import com.google.common.collect.Maps;
import com.google.gson.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

@Environment(EnvType.CLIENT)
/**
 * {@code ModelElement}.
 */
public record ModelElement(
		Vector3fc from,
		Vector3fc to,
		Map<Direction, ModelElementFace> faces,
		@Nullable ModelRotation rotation,
		boolean shade,
		int lightEmission
) {

	private static final boolean DEFAULT_SHADE_VALUE = false;
	private static final float MIN_COORD = -16.0F;
	private static final float MAX_COORD = 32.0F;

	public ModelElement(Vector3fc vector3fc, Vector3fc vector3fc2, Map<Direction, ModelElementFace> faces) {
		this(vector3fc, vector3fc2, faces, null, true, 0);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Deserializer}.
	 */
	protected static class Deserializer implements JsonDeserializer<ModelElement> {

		private static final boolean DEFAULT_SHADE = true;
		private static final int DEFAULT_LIGHT_EMISSION = 0;
		private static final String KEY_SHADE = "shade";
		private static final String KEY_LIGHT_EMISSION = "light_emission";
		private static final String KEY_ROTATION = "rotation";
		private static final String KEY_ORIGIN = "origin";
		private static final String KEY_ANGLE = "angle";
		private static final String KEY_X = "x";
		private static final String KEY_Y = "y";
		private static final String KEY_Z = "z";
		private static final String KEY_AXIS = "axis";
		private static final String KEY_RESCALE = "rescale";
		private static final String KEY_FACES = "faces";
		private static final String KEY_TO = "to";
		private static final String KEY_FROM = "from";

		public ModelElement deserialize(
				JsonElement jsonElement,
				Type type,
				JsonDeserializationContext jsonDeserializationContext
		) throws JsonParseException {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			Vector3f vector3f = deserializeAndValidateVec3f(jsonObject, "from");
			Vector3f vector3f2 = deserializeAndValidateVec3f(jsonObject, "to");
			ModelRotation modelRotation = this.deserializeRotation(jsonObject);
			Map<Direction, ModelElementFace>
					map =
					this.deserializeFacesValidating(jsonDeserializationContext, jsonObject);
			if (jsonObject.has("shade") && !JsonHelper.hasBoolean(jsonObject, "shade")) {
				throw new JsonParseException("Expected 'shade' to be a Boolean");
			}
			else {
				boolean bl = JsonHelper.getBoolean(jsonObject, "shade", true);
				int i = 0;
				if (jsonObject.has("light_emission")) {
					boolean bl2 = JsonHelper.hasNumber(jsonObject, "light_emission");
					if (bl2) {
						i = JsonHelper.getInt(jsonObject, "light_emission");
					}

					if (!bl2 || i < 0 || i > 15) {
						throw new JsonParseException(
								"Expected 'light_emission' to be an Integer between (inclusive) 0 and 15");
					}
				}

				return new ModelElement(vector3f, vector3f2, map, modelRotation, bl, i);
			}
		}

		private @Nullable ModelRotation deserializeRotation(JsonObject object) {
			if (!object.has("rotation")) {
				return null;
			}
			else {
				JsonObject jsonObject = JsonHelper.getObject(object, "rotation");
				Vector3f vector3f = deserializeVec3f(jsonObject, "origin");
				vector3f.mul(0.0625F);
				ModelRotation.RotationValue lv;
				if (!jsonObject.has("axis") && !jsonObject.has("angle")) {
					if (!jsonObject.has("x") && !jsonObject.has("y") && !jsonObject.has("z")) {
						throw new JsonParseException(
								"Missing rotation value, expected either 'axis' and 'angle' or 'x', 'y' and 'z'");
					}

					float g = JsonHelper.getFloat(jsonObject, "x", 0.0F);
					float f = JsonHelper.getFloat(jsonObject, "y", 0.0F);
					float h = JsonHelper.getFloat(jsonObject, "z", 0.0F);
					lv = new ModelRotation.EulerRotation(g, f, h);
				}
				else {
					Direction.Axis axis = this.deserializeAxis(jsonObject);
					float f = JsonHelper.getFloat(jsonObject, "angle");
					lv = new ModelRotation.AxisAngleRotation(axis, f);
				}

				boolean bl = JsonHelper.getBoolean(jsonObject, "rescale", false);
				return new ModelRotation(vector3f, lv, bl);
			}
		}

		private Direction.Axis deserializeAxis(JsonObject object) {
			String string = JsonHelper.getString(object, "axis");
			Direction.Axis axis = Direction.Axis.fromId(string.toLowerCase(Locale.ROOT));
			if (axis == null) {
				throw new JsonParseException("Invalid rotation axis: " + string);
			}
			else {
				return axis;
			}
		}

		private Map<Direction, ModelElementFace> deserializeFacesValidating(
				JsonDeserializationContext context,
				JsonObject object
		) {
			Map<Direction, ModelElementFace> map = this.deserializeFaces(context, object);
			if (map.isEmpty()) {
				throw new JsonParseException("Expected between 1 and 6 unique faces, got 0");
			}
			else {
				return map;
			}
		}

		private Map<Direction, ModelElementFace> deserializeFaces(
				JsonDeserializationContext context,
				JsonObject object
		) {
			Map<Direction, ModelElementFace> map = Maps.newEnumMap(Direction.class);
			JsonObject jsonObject = JsonHelper.getObject(object, "faces");

			for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
				Direction direction = this.getDirection(entry.getKey());
				map.put(direction, (ModelElementFace) context.deserialize(entry.getValue(), ModelElementFace.class));
			}

			return map;
		}

		private Direction getDirection(String name) {
			Direction direction = Direction.byId(name);
			if (direction == null) {
				throw new JsonParseException("Unknown facing: " + name);
			}
			else {
				return direction;
			}
		}

		private static Vector3f deserializeAndValidateVec3f(JsonObject jsonObject, String string) {
			Vector3f vector3f = deserializeVec3f(jsonObject, string);
			if (!(vector3f.x() < -16.0F)
					&& !(vector3f.y() < -16.0F)
					&& !(vector3f.z() < -16.0F)
					&& !(vector3f.x() > 32.0F)
					&& !(vector3f.y() > 32.0F)
					&& !(vector3f.z() > 32.0F)) {
				return vector3f;
			}
			else {
				throw new JsonParseException("'" + string + "' specifier exceeds the allowed boundaries: " + vector3f);
			}
		}

		private static Vector3f deserializeVec3f(JsonObject jsonObject, String string) {
			JsonArray jsonArray = JsonHelper.getArray(jsonObject, string);
			if (jsonArray.size() != 3) {
				throw new JsonParseException("Expected 3 " + string + " values, found: " + jsonArray.size());
			}
			else {
				float[] fs = new float[3];

				for (int i = 0; i < fs.length; i++) {
					fs[i] = JsonHelper.asFloat(jsonArray.get(i), string + "[" + i + "]");
				}

				return new Vector3f(fs[0], fs[1], fs[2]);
			}
		}
	}
}
