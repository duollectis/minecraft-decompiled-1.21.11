package net.minecraft.client.font;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.FixedGlyphProvider;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import org.joml.Matrix4f;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
/**
 * {@code PlayerHeadGlyphs}.
 */
public class PlayerHeadGlyphs {

	static final GlyphMetrics EMPTY_SPRITE_METRICS = GlyphMetrics.empty(8.0F);
	final PlayerSkinCache playerSkinCache;
	private final LoadingCache<StyleSpriteSource.Player, GlyphProvider> fetchingCache = CacheBuilder.newBuilder()
	                                                                                                .expireAfterAccess(
			                                                                                                PlayerSkinCache.TIME_TO_LIVE)
	                                                                                                .build(new CacheLoader<StyleSpriteSource.Player, GlyphProvider>() {
		                                                                                                public GlyphProvider load(
				                                                                                                StyleSpriteSource.Player player
		                                                                                                ) {
			                                                                                                final Supplier<PlayerSkinCache.Entry>
					                                                                                                supplier =
					                                                                                                PlayerHeadGlyphs.this.playerSkinCache.getSupplier(
							                                                                                                player.profile());
			                                                                                                final boolean
					                                                                                                bl =
					                                                                                                player.hat();
			                                                                                                return new FixedGlyphProvider(
					                                                                                                new BakedGlyph() {
						                                                                                                @Override
						                                                                                                public GlyphMetrics getMetrics() {
							                                                                                                return PlayerHeadGlyphs.EMPTY_SPRITE_METRICS;
						                                                                                                }

						                                                                                                @Override
						                                                                                                public TextDrawable.DrawnGlyphRect create(
								                                                                                                float x,
								                                                                                                float y,
								                                                                                                int color,
								                                                                                                int shadowColor,
								                                                                                                Style style,
								                                                                                                float boldOffset,
								                                                                                                float shadowOffset
						                                                                                                ) {
							                                                                                                return new PlayerHeadGlyphs.HeadGlyph(
									                                                                                                supplier,
									                                                                                                bl,
									                                                                                                x,
									                                                                                                y,
									                                                                                                color,
									                                                                                                shadowColor,
									                                                                                                shadowOffset,
									                                                                                                style
							                                                                                                );
						                                                                                                }
					                                                                                                });
		                                                                                                }
	                                                                                                });

	public PlayerHeadGlyphs(PlayerSkinCache playerSkinCache) {
		this.playerSkinCache = playerSkinCache;
	}

	/**
	 * Get.
	 *
	 * @param source source
	 *
	 * @return GlyphProvider — 
	 */
	public GlyphProvider get(StyleSpriteSource.Player source) {
		return (GlyphProvider) this.fetchingCache.getUnchecked(source);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code HeadGlyph}.
	 */
	record HeadGlyph(
			Supplier<PlayerSkinCache.Entry> skin,
			boolean hat,
			float x,
			float y,
			int color,
			int shadowColor,
			float shadowOffset,
			Style style
	)
			implements DrawnSpriteGlyph {

		@Override
		public void draw(
				Matrix4f matrix,
				VertexConsumer vertexConsumer,
				int light,
				float x,
				float y,
				float z,
				int color
		) {
			float f = x + this.getEffectiveMinX();
			float g = x + this.getEffectiveMaxX();
			float h = y + this.getEffectiveMinY();
			float i = y + this.getEffectiveMaxY();
			drawInternal(matrix, vertexConsumer, light, f, g, h, i, z, color, 8.0F, 8.0F, 8, 8, 64, 64);
			if (this.hat) {
				drawInternal(matrix, vertexConsumer, light, f, g, h, i, z, color, 40.0F, 8.0F, 8, 8, 64, 64);
			}
		}

		private static void drawInternal(
				Matrix4f matrix,
				VertexConsumer vertexConsumer,
				int light,
				float xMin,
				float xMax,
				float yMin,
				float yMax,
				float z,
				int color,
				float regionTop,
				float regionLeft,
				int regionWidth,
				int regionHeight,
				int textureWidth,
				int textureHeight
		) {
			float f = (regionTop + 0.0F) / textureWidth;
			float g = (regionTop + regionWidth) / textureWidth;
			float h = (regionLeft + 0.0F) / textureHeight;
			float i = (regionLeft + regionHeight) / textureHeight;
			vertexConsumer.vertex(matrix, xMin, yMin, z).texture(f, h).color(color).light(light);
			vertexConsumer.vertex(matrix, xMin, yMax, z).texture(f, i).color(color).light(light);
			vertexConsumer.vertex(matrix, xMax, yMax, z).texture(g, i).color(color).light(light);
			vertexConsumer.vertex(matrix, xMax, yMin, z).texture(g, h).color(color).light(light);
		}

		@Override
		public RenderLayer getRenderLayer(TextRenderer.TextLayerType type) {
			return this.skin.get().getTextRenderLayers().getRenderLayer(type);
		}

		@Override
		public RenderPipeline getPipeline() {
			return this.skin.get().getTextRenderLayers().guiPipeline();
		}

		@Override
		public GpuTextureView textureView() {
			return this.skin.get().getTextureView();
		}
	}
}
