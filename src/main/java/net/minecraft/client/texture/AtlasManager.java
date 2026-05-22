package net.minecraft.client.texture;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.resource.metadata.GuiResourceMetadata;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/**
 * Управляет всеми атласами спрайтов игры: регистрирует их в {@link TextureManager},
 * координирует асинхронную загрузку и сшивку (stitch), а также предоставляет
 * единую точку доступа к спрайтам через {@link SpriteHolder}.
 */
@Environment(EnvType.CLIENT)
public class AtlasManager implements ResourceReloader, SpriteHolder, AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final List<AtlasManager.Metadata> ATLAS_METADATA = List.of(
		new AtlasManager.Metadata(TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE, Atlases.ARMOR_TRIMS, false),
		new AtlasManager.Metadata(TexturedRenderLayers.BANNER_PATTERNS_ATLAS_TEXTURE, Atlases.BANNER_PATTERNS, false),
		new AtlasManager.Metadata(TexturedRenderLayers.BEDS_ATLAS_TEXTURE, Atlases.BEDS, false),
		new AtlasManager.Metadata(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Atlases.BLOCKS, true),
		new AtlasManager.Metadata(SpriteAtlasTexture.ITEMS_ATLAS_TEXTURE, Atlases.ITEMS, false),
		new AtlasManager.Metadata(TexturedRenderLayers.CHEST_ATLAS_TEXTURE, Atlases.CHESTS, false),
		new AtlasManager.Metadata(TexturedRenderLayers.DECORATED_POT_ATLAS_TEXTURE, Atlases.DECORATED_POT, false),
		new AtlasManager.Metadata(
			TexturedRenderLayers.GUI_ATLAS_TEXTURE,
			Atlases.GUI,
			false,
			Set.of(GuiResourceMetadata.SERIALIZER)
		),
		new AtlasManager.Metadata(TexturedRenderLayers.MAP_DECORATIONS_ATLAS_TEXTURE, Atlases.MAP_DECORATIONS, false),
		new AtlasManager.Metadata(TexturedRenderLayers.PAINTINGS_ATLAS_TEXTURE, Atlases.PAINTINGS, false),
		new AtlasManager.Metadata(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE, Atlases.PARTICLES, false),
		new AtlasManager.Metadata(TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE, Atlases.SHIELD_PATTERNS, false),
		new AtlasManager.Metadata(TexturedRenderLayers.SHULKER_BOXES_ATLAS_TEXTURE, Atlases.SHULKER_BOXES, false),
		new AtlasManager.Metadata(TexturedRenderLayers.SIGNS_ATLAS_TEXTURE, Atlases.SIGNS, false),
		new AtlasManager.Metadata(TexturedRenderLayers.CELESTIALS_ATLAS_TEXTURE, Atlases.CELESTIALS, false)
	);

	public static final ResourceReloader.Key<AtlasManager.Stitch> stitchKey = new ResourceReloader.Key<>();

	private final Map<Identifier, AtlasManager.Entry> entriesByTextureId = new HashMap<>();
	private final Map<Identifier, AtlasManager.Entry> entriesByDefinitionId = new HashMap<>();
	private Map<SpriteIdentifier, Sprite> sprites = Map.of();
	private int mipmapLevels;

	public AtlasManager(TextureManager textureManager, int mipmapLevels) {
		for (AtlasManager.Metadata metadata : ATLAS_METADATA) {
			SpriteAtlasTexture spriteAtlasTexture = new SpriteAtlasTexture(metadata.textureId);
			textureManager.registerTexture(metadata.textureId, spriteAtlasTexture);
			AtlasManager.Entry entry = new AtlasManager.Entry(spriteAtlasTexture, metadata);
			entriesByTextureId.put(metadata.textureId, entry);
			entriesByDefinitionId.put(metadata.definitionId, entry);
		}

		this.mipmapLevels = mipmapLevels;
	}

	public SpriteAtlasTexture getAtlasTexture(Identifier id) {
		AtlasManager.Entry entry = entriesByDefinitionId.get(id);
		if (entry == null) {
			throw new IllegalArgumentException("Invalid atlas id: " + id);
		}

		return entry.atlas();
	}

	/**
	 * Передаёт все зарегистрированные атласы в указанный {@code consumer}.
	 * Используется для обхода всех атласов при перезагрузке ресурсов.
	 */
	public void acceptAtlasTextures(BiConsumer<Identifier, SpriteAtlasTexture> consumer) {
		entriesByDefinitionId.forEach((definitionId, entry) -> consumer.accept(definitionId, entry.atlas));
	}

	public void setMipmapLevels(int mipmapLevels) {
		this.mipmapLevels = mipmapLevels;
	}

	@Override
	public void close() {
		sprites = Map.of();
		entriesByDefinitionId.values().forEach(AtlasManager.Entry::close);
		entriesByDefinitionId.clear();
		entriesByTextureId.clear();
	}

	@Override
	public Sprite getSprite(SpriteIdentifier id) {
		Sprite sprite = sprites.get(id);
		if (sprite != null) {
			return sprite;
		}

		Identifier atlasId = id.getAtlasId();
		AtlasManager.Entry entry = entriesByTextureId.get(atlasId);
		if (entry == null) {
			throw new IllegalArgumentException("Invalid atlas texture id: " + atlasId);
		}

		return entry.atlas().getMissingSprite();
	}

	@Override
	public void prepareSharedState(ResourceReloader.Store store) {
		int count = entriesByDefinitionId.size();
		List<AtlasManager.CompletableEntry> completableEntries = new ArrayList<>(count);
		Map<Identifier, CompletableFuture<SpriteLoader.StitchResult>> preparations = new HashMap<>(count);
		List<CompletableFuture<?>> readyFutures = new ArrayList<>(count);

		entriesByDefinitionId.forEach((textureId, entry) -> {
			CompletableFuture<SpriteLoader.StitchResult> future = new CompletableFuture<>();
			preparations.put(textureId, future);
			completableEntries.add(new AtlasManager.CompletableEntry(entry, future));
			readyFutures.add(future.thenCompose(SpriteLoader.StitchResult::readyForUpload));
		});

		CompletableFuture<?> allReady = CompletableFuture.allOf(readyFutures.toArray(CompletableFuture[]::new));
		store.put(stitchKey, new AtlasManager.Stitch(completableEntries, preparations, allReady));
	}

	@Override
	public CompletableFuture<Void> reload(
		ResourceReloader.Store store,
		Executor executor,
		ResourceReloader.Synchronizer synchronizer,
		Executor mainExecutor
	) {
		AtlasManager.Stitch stitch = store.getOrThrow(stitchKey);
		ResourceManager resourceManager = store.getResourceManager();

		stitch.entries.forEach(entry -> entry.entry
			.load(resourceManager, executor, mipmapLevels)
			.whenComplete((stitchResult, throwable) -> {
				if (stitchResult != null) {
					entry.preparations.complete(stitchResult);
				} else {
					entry.preparations.completeExceptionally(throwable);
				}
			}));

		return stitch.readyForUpload
			.thenCompose(synchronizer::whenPrepared)
			.thenAcceptAsync(v -> logDuplicates(stitch), mainExecutor);
	}

	private void logDuplicates(AtlasManager.Stitch stitch) {
		sprites = stitch.createSpriteMap();
		Map<Identifier, Sprite> firstOccurrence = new HashMap<>();

		sprites.forEach((id, sprite) -> {
			if (id.getTextureId().equals(MissingSprite.getMissingSpriteId())) {
				return;
			}

			Sprite existing = firstOccurrence.putIfAbsent(id.getTextureId(), sprite);
			if (existing != null) {
				LOGGER.warn(
					"Duplicate sprite {} from atlas {}, already defined in atlas {}. This will be rejected in a future version",
					new Object[]{id.getTextureId(), id.getAtlasId(), existing.getAtlasId()}
				);
			}
		});
	}

	@Environment(EnvType.CLIENT)
	record CompletableEntry(AtlasManager.Entry entry, CompletableFuture<SpriteLoader.StitchResult> preparations) {

		public void fillSpriteMap(Map<SpriteIdentifier, Sprite> sprites) {
			SpriteLoader.StitchResult stitchResult = preparations.join();
			entry.atlas.create(stitchResult);
			stitchResult.sprites().forEach((id, sprite) -> sprites.put(
				new SpriteIdentifier(entry.metadata.textureId, id),
				sprite
			));
		}
	}

	@Environment(EnvType.CLIENT)
	record Entry(SpriteAtlasTexture atlas, AtlasManager.Metadata metadata) implements AutoCloseable {

		@Override
		public void close() {
			atlas.clear();
		}

		CompletableFuture<SpriteLoader.StitchResult> load(ResourceManager manager, Executor executor, int mipLevel) {
			return SpriteLoader.fromAtlas(atlas)
				.load(
					manager,
					metadata.definitionId,
					metadata.createMipmaps ? mipLevel : 0,
					executor,
					metadata.additionalMetadata
				);
		}
	}

	@Environment(EnvType.CLIENT)
	public record Metadata(
		Identifier textureId,
		Identifier definitionId,
		boolean createMipmaps,
		Set<ResourceMetadataSerializer<?>> additionalMetadata
	) {

		public Metadata(Identifier textureId, Identifier definitionId, boolean createMipmaps) {
			this(textureId, definitionId, createMipmaps, Set.of());
		}
	}

	/**
	 * Хранит промежуточное состояние между фазами prepare и apply перезагрузки ресурсов:
	 * список записей атласов, их futures и общий future готовности к загрузке на GPU.
	 */
	@Environment(EnvType.CLIENT)
	public static class Stitch {

		final List<AtlasManager.CompletableEntry> entries;
		private final Map<Identifier, CompletableFuture<SpriteLoader.StitchResult>> preparations;
		final CompletableFuture<?> readyForUpload;

		Stitch(
			List<AtlasManager.CompletableEntry> entries,
			Map<Identifier, CompletableFuture<SpriteLoader.StitchResult>> preparations,
			CompletableFuture<?> readyForUpload
		) {
			this.entries = entries;
			this.preparations = preparations;
			this.readyForUpload = readyForUpload;
		}

		public Map<SpriteIdentifier, Sprite> createSpriteMap() {
			Map<SpriteIdentifier, Sprite> map = new HashMap<>();
			entries.forEach(entry -> entry.fillSpriteMap(map));
			return map;
		}

		public CompletableFuture<SpriteLoader.StitchResult> getPreparations(Identifier atlasTextureId) {
			return Objects.requireNonNull(preparations.get(atlasTextureId));
		}
	}
}
