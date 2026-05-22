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

/**
 * Провайдер глифов для отображения голов игроков в тексте (например, в чате).
 * Кеширует провайдеры по профилю игрока с TTL равным {@link PlayerSkinCache#TIME_TO_LIVE}.
 */
@Environment(EnvType.CLIENT)
public class PlayerHeadGlyphs {

	static final GlyphMetrics EMPTY_SPRITE_METRICS = GlyphMetrics.empty(8.0F);
	final PlayerSkinCache playerSkinCache;
	private final LoadingCache<StyleSpriteSource.Player, GlyphProvider> fetchingCache = CacheBuilder.newBuilder()
			.expireAfterAccess(PlayerSkinCache.TIME_TO_LIVE)
			.build(new CacheLoader<>() {
				public GlyphProvider load(StyleSpriteSource.Player player) {
					Supplier<PlayerSkinCache.Entry> skinSupplier =
							PlayerHeadGlyphs.this.playerSkinCache.getSupplier(player.profile());
					boolean showHat = player.hat();
					return new FixedGlyphProvider(new BakedGlyph() {
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
									skinSupplier,
									showHat,
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

	public GlyphProvider get(StyleSpriteSource.Player source) {
		return fetchingCache.getUnchecked(source);
	}

	/**
	 * Запечённый глиф головы игрока. Рисует базовый слой лица и опционально слой шляпы
	 * из скин-текстуры 64×64 пикселя.
	 */
	@Environment(EnvType.CLIENT)
	record HeadGlyph(
			Supplier<PlayerSkinCache.Entry> skin,
			boolean hat,
			float x,
			float y,
			int color,
			int shadowColor,
			float shadowOffset,
			Style style
	) implements DrawnSpriteGlyph {

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
			float xMin = x + getEffectiveMinX();
			float xMax = x + getEffectiveMaxX();
			float yMin = y + getEffectiveMinY();
			float yMax = y + getEffectiveMaxY();
			drawInternal(matrix, vertexConsumer, light, xMin, xMax, yMin, yMax, z, color, 8.0F, 8.0F, 8, 8, 64, 64);

			if (hat) {
				drawInternal(matrix, vertexConsumer, light, xMin, xMax, yMin, yMax, z, color, 40.0F, 8.0F, 8, 8, 64, 64);
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
			float uMin = regionTop / textureWidth;
			float uMax = (regionTop + regionWidth) / textureWidth;
			float vMin = regionLeft / textureHeight;
			float vMax = (regionLeft + regionHeight) / textureHeight;
			vertexConsumer.vertex(matrix, xMin, yMin, z).texture(uMin, vMin).color(color).light(light);
			vertexConsumer.vertex(matrix, xMin, yMax, z).texture(uMin, vMax).color(color).light(light);
			vertexConsumer.vertex(matrix, xMax, yMax, z).texture(uMax, vMax).color(color).light(light);
			vertexConsumer.vertex(matrix, xMax, yMin, z).texture(uMax, vMin).color(color).light(light);
		}

		@Override
		public RenderLayer getRenderLayer(TextRenderer.TextLayerType type) {
			return skin.get().getTextRenderLayers().getRenderLayer(type);
		}

		@Override
		public RenderPipeline getPipeline() {
			return skin.get().getTextRenderLayers().guiPipeline();
		}

		@Override
		public GpuTextureView textureView() {
			return skin.get().getTextureView();
		}
	}
}
