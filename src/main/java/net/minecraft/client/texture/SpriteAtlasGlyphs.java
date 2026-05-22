package net.minecraft.client.texture;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Адаптер между системой шрифтов и текстурным атласом спрайтов.
 *
 * <p>Позволяет рендерить спрайты из атласа как глифы шрифта.
 * Для каждого {@link Identifier} спрайта лениво создаёт и кэширует
 * {@link GlyphProvider}, который оборачивает спрайт в {@link AtlasGlyph}.
 */
@Environment(EnvType.CLIENT)
public class SpriteAtlasGlyphs {

	static final GlyphMetrics EMPTY_SPRITE_METRICS = GlyphMetrics.empty(8.0F);

	final SpriteAtlasTexture atlasTexture;
	final TextRenderLayerSet renderLayerSet;

	private final GlyphProvider missingGlyphProvider;
	private final Map<Identifier, GlyphProvider> cachedGlyphs = new HashMap<>();
	private final Function<Identifier, GlyphProvider> computeSprite;

	public SpriteAtlasGlyphs(SpriteAtlasTexture atlasTexture) {
		this.atlasTexture = atlasTexture;
		renderLayerSet = TextRenderLayerSet.of(atlasTexture.getId());
		Sprite missingSprite = atlasTexture.getMissingSprite();
		missingGlyphProvider = createFixedGlyphProvider(missingSprite);
		computeSprite = id -> {
			Sprite sprite = atlasTexture.getSprite(id);
			return sprite == missingSprite
				? missingGlyphProvider
				: createFixedGlyphProvider(sprite);
		};
	}

	public GlyphProvider getGlyphProvider(Identifier id) {
		return cachedGlyphs.computeIfAbsent(id, computeSprite);
	}

	private GlyphProvider createFixedGlyphProvider(Sprite sprite) {
		return new FixedGlyphProvider(
			new BakedGlyph() {
				@Override
				public GlyphMetrics getMetrics() {
					return EMPTY_SPRITE_METRICS;
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
					return new AtlasGlyph(
						renderLayerSet,
						atlasTexture.getGlTextureView(),
						sprite,
						x,
						y,
						color,
						shadowColor,
						shadowOffset,
						style
					);
				}
			}
		);
	}

	/**
	 * Конкретный глиф, рендерящий спрайт из атласа как символ шрифта.
	 * Координаты UV берутся напрямую из {@link Sprite}.
	 */
	@Environment(EnvType.CLIENT)
	record AtlasGlyph(
		TextRenderLayerSet renderTypes,
		GpuTextureView textureView,
		Sprite sprite,
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
			float left = x + getEffectiveMinX();
			float right = x + getEffectiveMaxX();
			float top = y + getEffectiveMinY();
			float bottom = y + getEffectiveMaxY();

			vertexConsumer
				.vertex(matrix, left, top, z)
				.texture(sprite.getMinU(), sprite.getMinV())
				.color(color)
				.light(light);
			vertexConsumer
				.vertex(matrix, left, bottom, z)
				.texture(sprite.getMinU(), sprite.getMaxV())
				.color(color)
				.light(light);
			vertexConsumer
				.vertex(matrix, right, bottom, z)
				.texture(sprite.getMaxU(), sprite.getMaxV())
				.color(color)
				.light(light);
			vertexConsumer
				.vertex(matrix, right, top, z)
				.texture(sprite.getMaxU(), sprite.getMinV())
				.color(color)
				.light(light);
		}

		@Override
		public RenderLayer getRenderLayer(TextRenderer.TextLayerType type) {
			return renderTypes.getRenderLayer(type);
		}

		@Override
		public RenderPipeline getPipeline() {
			return renderTypes.guiPipeline();
		}
	}
}
