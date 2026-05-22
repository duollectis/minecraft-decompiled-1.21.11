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

/**
 * Разрешённые текстуры модели: отображение имён текстурных слотов на {@link SpriteIdentifier}.
 * Строится через {@link Builder} путём послойного наложения {@link Textures} из иерархии моделей.
 */
@Environment(EnvType.CLIENT)
public class ModelTextures {

	public static final ModelTextures EMPTY = new ModelTextures(Map.of());
	private static final char TEXTURE_REFERENCE_PREFIX = '#';

	private final Map<String, SpriteIdentifier> textures;

	ModelTextures(Map<String, SpriteIdentifier> textures) {
		this.textures = textures;
	}

	public @Nullable SpriteIdentifier get(String textureId) {
		if (isTextureReference(textureId)) {
			textureId = textureId.substring(1);
		}

		return textures.get(textureId);
	}

	private static boolean isTextureReference(String textureId) {
		return textureId.charAt(0) == TEXTURE_REFERENCE_PREFIX;
	}

	public static ModelTextures.Textures fromJson(JsonObject json) {
		ModelTextures.Textures.Builder builder = new ModelTextures.Textures.Builder();

		for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
			add(entry.getKey(), entry.getValue().getAsString(), builder);
		}

		return builder.build();
	}

	private static void add(String textureId, String value, ModelTextures.Textures.Builder builder) {
		if (isTextureReference(value)) {
			builder.addTextureReference(textureId, value.substring(1));
		}
		else {
			Identifier identifier = Identifier.tryParse(value);

			if (identifier == null) {
				throw new JsonParseException(value + " is not valid resource location");
			}

			builder.addSprite(textureId, new SpriteIdentifier(BakedModelManager.BLOCK_OR_ITEM_ATLAS_ID, identifier));
		}
	}

	/**
	 * Строитель разрешённых текстур: накапливает слои {@link Textures} в порядке приоритета
	 * (последний добавленный имеет наивысший приоритет) и разрешает цепочки ссылок.
	 */
	@Environment(EnvType.CLIENT)
	public static class Builder {

		private static final Logger LOGGER = LogUtils.getLogger();
		private final List<ModelTextures.Textures> layers = new ArrayList<>();

		public ModelTextures.Builder addLast(ModelTextures.Textures textures) {
			layers.addLast(textures);
			return this;
		}

		public ModelTextures.Builder addFirst(ModelTextures.Textures textures) {
			layers.addFirst(textures);
			return this;
		}

		/**
		 * Строит итоговую карту текстур, разрешая все ссылки вида {@code #slot}.
		 * Неразрешённые ссылки логируются как предупреждение.
		 */
		public ModelTextures build(SimpleModel model) {
			if (layers.isEmpty()) {
				return ModelTextures.EMPTY;
			}

			Object2ObjectMap<String, SpriteIdentifier> resolved = new Object2ObjectArrayMap<>();
			Object2ObjectMap<String, ModelTextures.TextureReferenceEntry> unresolved = new Object2ObjectArrayMap<>();

			for (ModelTextures.Textures layer : Lists.reverse(layers)) {
				layer.values.forEach((textureId, entry) -> {
					switch (entry) {
						case ModelTextures.SpriteEntry spriteEntry -> {
							unresolved.remove(textureId);
							resolved.put(textureId, spriteEntry.material());
						}
						case ModelTextures.TextureReferenceEntry refEntry -> {
							resolved.remove(textureId);
							unresolved.put(textureId, refEntry);
						}
						default -> throw new MatchException(null, null);
					}
				});
			}

			if (unresolved.isEmpty()) {
				return new ModelTextures(resolved);
			}

			// Итеративно разрешаем цепочки ссылок пока есть прогресс
			boolean progress = true;

			while (progress) {
				progress = false;
				ObjectIterator<Object2ObjectMap.Entry<String, ModelTextures.TextureReferenceEntry>> iterator =
						Object2ObjectMaps.fastIterator(unresolved);

				while (iterator.hasNext()) {
					Object2ObjectMap.Entry<String, ModelTextures.TextureReferenceEntry> entry = iterator.next();
					SpriteIdentifier sprite = resolved.get(entry.getValue().target());

					if (sprite != null) {
						resolved.put(entry.getKey(), sprite);
						iterator.remove();
						progress = true;
					}
				}
			}

			if (!unresolved.isEmpty()) {
				LOGGER.warn(
						"Unresolved texture references in {}:\n{}",
						model.name(),
						unresolved.entrySet()
						          .stream()
						          .map(e -> "\t#" + e.getKey() + "-> #" + e.getValue().target() + "\n")
						          .collect(Collectors.joining())
				);
			}

			return new ModelTextures(resolved);
		}
	}

	/** Запечатанный тип записи текстуры: либо прямой спрайт, либо ссылка на другой слот. */
	@Environment(EnvType.CLIENT)
	public sealed interface Entry permits ModelTextures.SpriteEntry, ModelTextures.TextureReferenceEntry {
	}

	@Environment(EnvType.CLIENT)
	record SpriteEntry(SpriteIdentifier material) implements ModelTextures.Entry {
	}

	@Environment(EnvType.CLIENT)
	record TextureReferenceEntry(String target) implements ModelTextures.Entry {
	}

	/**
	 * Набор текстурных записей одного слоя модели (из JSON-поля {@code textures}).
	 * Строится через вложенный {@link Builder}.
	 */
	@Environment(EnvType.CLIENT)
	public record Textures(Map<String, ModelTextures.Entry> values) {

		public static final ModelTextures.Textures EMPTY = new ModelTextures.Textures(Map.of());

		@Environment(EnvType.CLIENT)
		public static class Builder {

			private final Map<String, ModelTextures.Entry> entries = new HashMap<>();

			public ModelTextures.Textures.Builder addTextureReference(String textureId, String target) {
				entries.put(textureId, new ModelTextures.TextureReferenceEntry(target));
				return this;
			}

			public ModelTextures.Textures.Builder addSprite(String textureId, SpriteIdentifier spriteId) {
				entries.put(textureId, new ModelTextures.SpriteEntry(spriteId));
				return this;
			}

			public ModelTextures.Textures build() {
				return entries.isEmpty()
						? ModelTextures.Textures.EMPTY
						: new ModelTextures.Textures(Map.copyOf(entries));
			}
		}
	}
}
