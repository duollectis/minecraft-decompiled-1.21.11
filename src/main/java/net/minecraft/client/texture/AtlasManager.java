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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

@Environment(EnvType.CLIENT)
/**
 * {@code AtlasManager}.
 */
public class AtlasManager implements ResourceReloader, SpriteHolder, AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final List<AtlasManager.Metadata> ATLAS_METADATA = List.of(
			new AtlasManager.Metadata(TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE, Atlases.ARMOR_TRIMS, false),
			new AtlasManager.Metadata(
					TexturedRenderLayers.BANNER_PATTERNS_ATLAS_TEXTURE,
					Atlases.BANNER_PATTERNS,
					false
			),
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
			new AtlasManager.Metadata(
					TexturedRenderLayers.MAP_DECORATIONS_ATLAS_TEXTURE,
					Atlases.MAP_DECORATIONS,
					false
			),
			new AtlasManager.Metadata(TexturedRenderLayers.PAINTINGS_ATLAS_TEXTURE, Atlases.PAINTINGS, false),
			new AtlasManager.Metadata(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE, Atlases.PARTICLES, false),
			new AtlasManager.Metadata(
					TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE,
					Atlases.SHIELD_PATTERNS,
					false
			),
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
			this.entriesByTextureId.put(metadata.textureId, entry);
			this.entriesByDefinitionId.put(metadata.definitionId, entry);
		}

		this.mipmapLevels = mipmapLevels;
	}

	public SpriteAtlasTexture getAtlasTexture(Identifier id) {
		AtlasManager.Entry entry = this.entriesByDefinitionId.get(id);
		if (entry == null) {
			throw new IllegalArgumentException("Invalid atlas id: " + id);
		}
		else {
			return entry.atlas();
		}
	}

	/**
	 * Accept atlas textures.
	 *
	 * @param consumer consumer
	 */
	public void acceptAtlasTextures(BiConsumer<Identifier, SpriteAtlasTexture> consumer) {
		this.entriesByDefinitionId.forEach((definitionId, entry) -> consumer.accept(definitionId, entry.atlas));
	}

	public void setMipmapLevels(int mipmapLevels) {
		this.mipmapLevels = mipmapLevels;
	}

	@Override
	public void close() {
		this.sprites = Map.of();
		this.entriesByDefinitionId.values().forEach(AtlasManager.Entry::close);
		this.entriesByDefinitionId.clear();
		this.entriesByTextureId.clear();
	}

	@Override
	public Sprite getSprite(SpriteIdentifier id) {
		Sprite sprite = this.sprites.get(id);
		if (sprite != null) {
			return sprite;
		}
		else {
			Identifier identifier = id.getAtlasId();
			AtlasManager.Entry entry = this.entriesByTextureId.get(identifier);
			if (entry == null) {
				throw new IllegalArgumentException("Invalid atlas texture id: " + identifier);
			}
			else {
				return entry.atlas().getMissingSprite();
			}
		}
	}

	@Override
	public void prepareSharedState(ResourceReloader.Store store) {
		int i = this.entriesByDefinitionId.size();
		List<AtlasManager.CompletableEntry> list = new ArrayList<>(i);
		Map<Identifier, CompletableFuture<SpriteLoader.StitchResult>> map = new HashMap<>(i);
		List<CompletableFuture<?>> list2 = new ArrayList<>(i);
		this.entriesByDefinitionId.forEach((textureId, metadata) -> {
			CompletableFuture<SpriteLoader.StitchResult> completableFuturex = new CompletableFuture<>();
			map.put(textureId, completableFuturex);
			list.add(new AtlasManager.CompletableEntry(metadata, completableFuturex));
			list2.add(completableFuturex.thenCompose(SpriteLoader.StitchResult::readyForUpload));
		});
		CompletableFuture<?> completableFuture = CompletableFuture.allOf(list2.toArray(CompletableFuture[]::new));
		store.put(stitchKey, new AtlasManager.Stitch(list, map, completableFuture));
	}

	@Override
	public CompletableFuture<Void> reload(
			ResourceReloader.Store store,
			Executor executor,
			ResourceReloader.Synchronizer synchronizer,
			Executor executor2
	) {
		AtlasManager.Stitch stitch = store.getOrThrow(stitchKey);
		ResourceManager resourceManager = store.getResourceManager();
		stitch.entries.forEach(entry -> entry.entry
				.load(resourceManager, executor, this.mipmapLevels)
				.whenComplete((stitchResult, throwable) -> {
					if (stitchResult != null) {
						entry.preparations.complete(stitchResult);
					}
					else {
						entry.preparations.completeExceptionally(throwable);
					}
				}));
		return stitch.readyForUpload
				.thenCompose(synchronizer::whenPrepared)
				.thenAcceptAsync(v -> this.logDuplicates(stitch), executor2);
	}

	private void logDuplicates(AtlasManager.Stitch stitch) {
		this.sprites = stitch.createSpriteMap();
		Map<Identifier, Sprite> map = new HashMap<>();
		this.sprites
				.forEach(
						(id, sprite) -> {
							if (!id.getTextureId().equals(MissingSprite.getMissingSpriteId())) {
								Sprite sprite2 = map.putIfAbsent(id.getTextureId(), sprite);
								if (sprite2 != null) {
									LOGGER.warn(
											"Duplicate sprite {} from atlas {}, already defined in atlas {}. This will be rejected in a future version",
											new Object[]{id.getTextureId(), id.getAtlasId(), sprite2.getAtlasId()}
									);
								}
							}
						}
				);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code CompletableEntry}.
	 */
	record CompletableEntry(AtlasManager.Entry entry, CompletableFuture<SpriteLoader.StitchResult> preparations) {

		/**
		 * Fill sprite map.
		 *
		 * @param sprites sprites
		 */
		public void fillSpriteMap(Map<SpriteIdentifier, Sprite> sprites) {
			SpriteLoader.StitchResult stitchResult = this.preparations.join();
			this.entry.atlas.create(stitchResult);
			stitchResult
					.sprites()
					.forEach((id, sprite) -> sprites.put(
							new SpriteIdentifier(this.entry.metadata.textureId, id),
							sprite
					));
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Entry}.
	 */
	record Entry(SpriteAtlasTexture atlas, AtlasManager.Metadata metadata) implements AutoCloseable {

		@Override
		public void close() {
			this.atlas.clear();
		}

		CompletableFuture<SpriteLoader.StitchResult> load(ResourceManager manager, Executor executor, int mipLevel) {
			return SpriteLoader.fromAtlas(this.atlas)
			                   .load(
					                   manager,
					                   this.metadata.definitionId,
					                   this.metadata.createMipmaps ? mipLevel : 0,
					                   executor,
					                   this.metadata.additionalMetadata
			                   );
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Metadata}.
	 */
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

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Stitch}.
	 */
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

		/**
		 * Создаёт sprite map.
		 *
		 * @return Map — результат операции
		 */
		public Map<SpriteIdentifier, Sprite> createSpriteMap() {
			Map<SpriteIdentifier, Sprite> map = new HashMap<>();
			this.entries.forEach(entry -> entry.fillSpriteMap(map));
			return map;
		}

		public CompletableFuture<SpriteLoader.StitchResult> getPreparations(Identifier atlasTextureId) {
			return Objects.requireNonNull(this.preparations.get(atlasTextureId));
		}
	}
}
