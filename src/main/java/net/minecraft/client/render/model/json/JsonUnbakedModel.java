package net.minecraft.client.render.model.json;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.Geometry;
import net.minecraft.client.render.model.ModelTextures;
import net.minecraft.client.render.model.UnbakedGeometry;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jspecify.annotations.Nullable;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code JsonUnbakedModel}.
 */
public record JsonUnbakedModel(
		@Nullable Geometry geometry,
		UnbakedModel.@Nullable GuiLight guiLight,
		@Nullable Boolean ambientOcclusion,
		@Nullable ModelTransformation transformations,
		ModelTextures.Textures textures,
		@Nullable Identifier parent
) implements UnbakedModel {

	@VisibleForTesting
	static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(JsonUnbakedModel.class, new JsonUnbakedModel.Deserializer())
			.registerTypeAdapter(ModelElement.class, new ModelElement.Deserializer())
			.registerTypeAdapter(ModelElementFace.class, new ModelElementFace.Deserializer())
			.registerTypeAdapter(Transformation.class, new Transformation.Deserializer())
			.registerTypeAdapter(ModelTransformation.class, new ModelTransformation.Deserializer())
			.create();

	public static JsonUnbakedModel deserialize(Reader input) {
		return JsonHelper.deserialize(GSON, input, JsonUnbakedModel.class);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Deserializer}.
	 */
	public static class Deserializer implements JsonDeserializer<JsonUnbakedModel> {

		public JsonUnbakedModel deserialize(
				JsonElement jsonElement,
				Type type,
				JsonDeserializationContext jsonDeserializationContext
		) throws JsonParseException {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			Geometry geometry = this.elementsFromJson(jsonDeserializationContext, jsonObject);
			String string = this.parentFromJson(jsonObject);
			ModelTextures.Textures textures = this.texturesFromJson(jsonObject);
			Boolean boolean_ = this.ambientOcclusionFromJson(jsonObject);
			ModelTransformation modelTransformation = null;
			if (jsonObject.has("display")) {
				JsonObject jsonObject2 = JsonHelper.getObject(jsonObject, "display");
				modelTransformation =
						(ModelTransformation) jsonDeserializationContext.deserialize(
								jsonObject2,
								ModelTransformation.class
						);
			}

			UnbakedModel.GuiLight guiLight = null;
			if (jsonObject.has("gui_light")) {
				guiLight = UnbakedModel.GuiLight.byName(JsonHelper.getString(jsonObject, "gui_light"));
			}

			Identifier identifier = string.isEmpty() ? null : Identifier.of(string);
			return new JsonUnbakedModel(geometry, guiLight, boolean_, modelTransformation, textures, identifier);
		}

		private ModelTextures.Textures texturesFromJson(JsonObject object) {
			if (object.has("textures")) {
				JsonObject jsonObject = JsonHelper.getObject(object, "textures");
				return ModelTextures.fromJson(jsonObject);
			}
			else {
				return ModelTextures.Textures.EMPTY;
			}
		}

		private String parentFromJson(JsonObject json) {
			return JsonHelper.getString(json, "parent", "");
		}

		protected @Nullable Boolean ambientOcclusionFromJson(JsonObject json) {
			return json.has("ambientocclusion") ? JsonHelper.getBoolean(json, "ambientocclusion") : null;
		}

		protected @Nullable Geometry elementsFromJson(JsonDeserializationContext context, JsonObject json) {
			if (!json.has("elements")) {
				return null;
			}
			else {
				List<ModelElement> list = new ArrayList<>();

				for (JsonElement jsonElement : JsonHelper.getArray(json, "elements")) {
					list.add((ModelElement) context.deserialize(jsonElement, ModelElement.class));
				}

				return new UnbakedGeometry(list);
			}
		}
	}
}
