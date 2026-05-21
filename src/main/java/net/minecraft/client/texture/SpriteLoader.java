package net.minecraft.client.texture;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.sprite.FabricStitchResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.TextureFilteringMode;
import net.minecraft.client.texture.atlas.AtlasLoader;
import net.minecraft.client.texture.atlas.AtlasSource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.profiler.ScopedProfiler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
/**
 * {@code SpriteLoader}.
 */
public class SpriteLoader {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final Identifier id;
	private final int maxTextureSize;

	public SpriteLoader(Identifier id, int maxTextureSize) {
		this.id = id;
		this.maxTextureSize = maxTextureSize;
	}

	/**
	 * From atlas.
	 *
	 * @param atlasTexture atlas texture
	 *
	 * @return SpriteLoader — результат операции
	 */
	public static SpriteLoader fromAtlas(SpriteAtlasTexture atlasTexture) {
		return new SpriteLoader(atlasTexture.getId(), atlasTexture.getMaxTextureSize());
	}

	private SpriteLoader.StitchResult stitch(List<SpriteContents> sprites, int mipLevel, Executor executor) {
		SpriteLoader.StitchResult var19;
		try (ScopedProfiler scopedProfiler = Profilers.get().scoped(() -> "stitch " + this.id)) {
			int i = this.maxTextureSize;
			int j = Integer.MAX_VALUE;
			int k = 1 << mipLevel;

			for (SpriteContents spriteContents : sprites) {
				j = Math.min(j, Math.min(spriteContents.getWidth(), spriteContents.getHeight()));
				int
						l =
						Math.min(
								Integer.lowestOneBit(spriteContents.getWidth()),
								Integer.lowestOneBit(spriteContents.getHeight())
						);
				if (l < k) {
					LOGGER.warn(
							"Texture {} with size {}x{} limits mip level from {} to {}",
							new Object[]{
									spriteContents.getId(),
									spriteContents.getWidth(),
									spriteContents.getHeight(),
									MathHelper.floorLog2(k),
									MathHelper.floorLog2(l)
							}
					);
					k = l;
				}
			}

			int m = Math.min(j, k);
			int n = MathHelper.floorLog2(m);
			int l;
			if (n < mipLevel) {
				LOGGER.warn(
						"{}: dropping miplevel from {} to {}, because of minimum power of two: {}",
						new Object[]{this.id, mipLevel, n, m}
				);
				l = n;
			}
			else {
				l = mipLevel;
			}

			GameOptions gameOptions = MinecraftClient.getInstance().options;
			int
					o =
					l != 0 && gameOptions.getTextureFiltering().getValue() == TextureFilteringMode.ANISOTROPIC
					? gameOptions.getMaxAnisotropy().getValue() : 0;
			TextureStitcher<SpriteContents> textureStitcher = new TextureStitcher<>(i, i, l, o);

			for (SpriteContents spriteContents2 : sprites) {
				textureStitcher.add(spriteContents2);
			}

			try {
				textureStitcher.stitch();
			}
			catch (TextureStitcherCannotFitException var21) {
				CrashReport crashReport = CrashReport.create(var21, "Stitching");
				CrashReportSection crashReportSection = crashReport.addElement("Stitcher");
				crashReportSection.add(
						"Sprites",
						var21.getSprites()
						     .stream()
						     .map(spritex -> String.format(
								     Locale.ROOT,
								     "%s[%dx%d]",
								     spritex.getId(),
								     spritex.getWidth(),
								     spritex.getHeight()
						     ))
						     .collect(Collectors.joining(","))
				);
				crashReportSection.add("Max Texture Size", i);
				throw new CrashException(crashReport);
			}

			int p = textureStitcher.getWidth();
			int q = textureStitcher.getHeight();
			Map<Identifier, Sprite> map = this.collectStitchedSprites(textureStitcher, p, q);
			Sprite sprite = map.get(MissingSprite.getMissingSpriteId());
			CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(
					() -> map.values().forEach(spritex -> spritex.getContents().generateMipmaps(l)), executor
			);
			var19 = new SpriteLoader.StitchResult(p, q, l, sprite, map, completableFuture);
		}

		return var19;
	}

	private static CompletableFuture<List<SpriteContents>> loadAll(
			SpriteOpener opener,
			List<AtlasSource.SpriteSource> sources,
			Executor executor
	) {
		List<CompletableFuture<SpriteContents>> list = sources.stream()
		                                                      .map(source -> CompletableFuture.supplyAsync(
				                                                      () -> source.load(opener), executor))
		                                                      .toList();
		return Util.combineSafe(list).thenApply(sprites -> sprites.stream().filter(Objects::nonNull).toList());
	}

	public CompletableFuture<SpriteLoader.StitchResult> load(
			ResourceManager resourceManager,
			Identifier path,
			int mipLevel,
			Executor executor,
			Set<ResourceMetadataSerializer<?>> additionalMetadata
	) {
		SpriteOpener spriteOpener = SpriteOpener.create(additionalMetadata);
		return CompletableFuture
				.<List<AtlasSource.SpriteSource>>supplyAsync(
						() -> AtlasLoader
								.of(resourceManager, path)
								.loadSources(resourceManager), executor
				)
				.thenCompose(sources -> loadAll(spriteOpener, (List<AtlasSource.SpriteSource>) sources, executor))
				.thenApply(sprites -> this.stitch((List<SpriteContents>) sprites, mipLevel, executor));
	}

	private Map<Identifier, Sprite> collectStitchedSprites(
			TextureStitcher<SpriteContents> stitcher,
			int atlasWidth,
			int atlasHeight
	) {
		Map<Identifier, Sprite> map = new HashMap<>();
		stitcher.getStitchedSprites((info, x, y, padding) -> map.put(
				info.getId(),
				new Sprite(this.id, info, atlasWidth, atlasHeight, x, y, padding)
		));
		return map;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code StitchResult}.
	 */
	public record StitchResult(
			int width,
			int height,
			int mipLevel,
			Sprite missing,
			Map<Identifier, Sprite> sprites,
			CompletableFuture<Void> readyForUpload
	)
			implements FabricStitchResult {

		public @Nullable Sprite getSprite(Identifier id) {
			return this.sprites.get(id);
		}
	}
}
