package net.minecraft.client.render.model;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
/**
 * {@code ModelTextures}.
 */
public class ModelTextures {

	public static final ModelTextures EMPTY = new ModelTextures(Map.of());
	private static final char TEXTURE_REFERENCE_PREFIX = '#';
	private final Map<String, SpriteIdentifier> textures;

	ModelTextures(Map<String, SpriteIdentifier> textures) {
		this.textures = textures;
	}

	/**
	 * Get.
	 *
	 * @param textureId texture id
	 *
	 * @return @Nullable SpriteIdentifier — 
	 */
	public @Nullable SpriteIdentifier get(String textureId) {
		if (isTextureReference(textureId)) {
			textureId = textureId.substring(1);
		}

		return this.textures.get(textureId);
	}

	private static boolean isTextureReference(String textureId) {
		return textureId.charAt(0) == '#';
	}

	public static ModelTextures.Textures fromJson(JsonObject json) {
		ModelTextures.Textures.Builder builder = new ModelTextures.Textures.Builder();

		for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
			add(entry.getKey(), entry.getValue().getAsString(), builder);
		}

		return builder.build();
	}

	private static void add(String string, String string2, ModelTextures.Textures.Builder builder) {
		if (isTextureReference(string2)) {
			builder.addTextureReference(string, string2.substring(1));
		}
		else {
			Identifier identifier = Identifier.tryParse(string2);
			if (identifier == null) {
				throw new JsonParseException(string2 + " is not valid resource location");
			}

			builder.addSprite(string, new SpriteIdentifier(BakedModelManager.BLOCK_OR_ITEM_ATLAS_ID, identifier));
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private static final Logger LOGGER = LogUtils.getLogger();
		private final List<ModelTextures.Textures> textures = new ArrayList<>();

		public ModelTextures.Builder addLast(ModelTextures.Textures textures) {
			this.textures.addLast(textures);
			return this;
		}

		public ModelTextures.Builder addFirst(ModelTextures.Textures textures) {
			this.textures.addFirst(textures);
			return this;
		}

		/**
		 * Build.
		 *
		 * @param modelNameSupplier model name supplier
		 *
		 * @return ModelTextures — результат операции
		 */
		public ModelTextures build(SimpleModel modelNameSupplier) {
			if (this.textures.isEmpty()) {
				return ModelTextures.EMPTY;
			}
			else {
				Object2ObjectMap<String, SpriteIdentifier> object2ObjectMap = new Object2ObjectArrayMap();
				Object2ObjectMap<String, ModelTextures.TextureReferenceEntry>
						object2ObjectMap2 =
						new Object2ObjectArrayMap();

				for (ModelTextures.Textures textures : Lists.reverse(this.textures)) {
					textures.values.forEach((textureId, entryx) -> {
						switch (entryx) {
							case ModelTextures.SpriteEntry spriteEntry:
								object2ObjectMap2.remove(textureId);
								object2ObjectMap.put(textureId, spriteEntry.material());
								break;
							case ModelTextures.TextureReferenceEntry textureReferenceEntry:
								object2ObjectMap.remove(textureId);
								object2ObjectMap2.put(textureId, textureReferenceEntry);
								break;
							default:
								throw new MatchException(null, null);
						}
					});
				}

				if (object2ObjectMap2.isEmpty()) {
					return new ModelTextures(object2ObjectMap);
				}
				else {
					boolean bl = true;

					while (bl) {
						bl = false;
						ObjectIterator<it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<String, ModelTextures.TextureReferenceEntry>>
								objectIterator =
								Object2ObjectMaps.fastIterator(
										object2ObjectMap2
								);

						while (objectIterator.hasNext()) {
							it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<String, ModelTextures.TextureReferenceEntry>
									entry =
									(it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<String, ModelTextures.TextureReferenceEntry>) objectIterator.next();
							SpriteIdentifier
									spriteIdentifier =
									(SpriteIdentifier) object2ObjectMap.get(((ModelTextures.TextureReferenceEntry) entry.getValue()).target);
							if (spriteIdentifier != null) {
								object2ObjectMap.put((String) entry.getKey(), spriteIdentifier);
								objectIterator.remove();
								bl = true;
							}
						}
					}

					if (!object2ObjectMap2.isEmpty()) {
						LOGGER.warn(
								"Unresolved texture references in {}:\n{}",
								modelNameSupplier.name(),
								object2ObjectMap2.entrySet()
								                 .stream()
								                 .map(entryx -> "\t#" + (String) entryx.getKey() + "-> #"
										                 + ((ModelTextures.TextureReferenceEntry) entryx.getValue()).target
										                 + "\n")
								                 .collect(Collectors.joining())
						);
					}

					return new ModelTextures(object2ObjectMap);
				}
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Entry}.
	 */
	public sealed interface Entry permits ModelTextures.SpriteEntry, ModelTextures.TextureReferenceEntry {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SpriteEntry}.
	 */
	record SpriteEntry(SpriteIdentifier material) implements ModelTextures.Entry {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code TextureReferenceEntry}.
	 */
	record TextureReferenceEntry(String target) implements ModelTextures.Entry {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Textures}.
	 */
	public record Textures(Map<String, ModelTextures.Entry> values) {

		public static final ModelTextures.Textures EMPTY = new ModelTextures.Textures(Map.of());

		@Environment(EnvType.CLIENT)
		/**
		 * {@code Builder}.
		 */
		public static class Builder {

			private final Map<String, ModelTextures.Entry> entries = new HashMap<>();

			public ModelTextures.Textures.Builder addTextureReference(String textureId, String target) {
				this.entries.put(textureId, new ModelTextures.TextureReferenceEntry(target));
				return this;
			}

			public ModelTextures.Textures.Builder addSprite(String textureId, SpriteIdentifier spriteId) {
				this.entries.put(textureId, new ModelTextures.SpriteEntry(spriteId));
				return this;
			}

			public ModelTextures.Textures build() {
				return this.entries.isEmpty() ? ModelTextures.Textures.EMPTY
				                              : new ModelTextures.Textures(Map.copyOf(this.entries));
			}
		}
	}
}
