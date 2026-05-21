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

@Environment(EnvType.CLIENT)
/**
 * {@code SpriteAtlasGlyphs}.
 */
public class SpriteAtlasGlyphs {

	static final GlyphMetrics EMPTY_SPRITE_METRICS = GlyphMetrics.empty(8.0F);
	final SpriteAtlasTexture atlasTexture;
	final TextRenderLayerSet renderLayerSet;
	private final GlyphProvider missingGlyphProvider;
	private final Map<Identifier, GlyphProvider> cachedGlyphs = new HashMap<>();
	private final Function<Identifier, GlyphProvider> computeSprite;

	public SpriteAtlasGlyphs(SpriteAtlasTexture atlasTexture) {
		this.atlasTexture = atlasTexture;
		this.renderLayerSet = TextRenderLayerSet.of(atlasTexture.getId());
		Sprite sprite = atlasTexture.getMissingSprite();
		this.missingGlyphProvider = this.createFixedGlyphProvider(sprite);
		this.computeSprite = id -> {
			Sprite sprite2 = atlasTexture.getSprite(id);
			return sprite2 == sprite ? this.missingGlyphProvider : this.createFixedGlyphProvider(sprite2);
		};
	}

	public GlyphProvider getGlyphProvider(Identifier id) {
		return this.cachedGlyphs.computeIfAbsent(id, this.computeSprite);
	}

	private GlyphProvider createFixedGlyphProvider(Sprite sprite) {
		return new FixedGlyphProvider(
				new BakedGlyph() {
					@Override
					public GlyphMetrics getMetrics() {
						return SpriteAtlasGlyphs.EMPTY_SPRITE_METRICS;
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
						return new SpriteAtlasGlyphs.AtlasGlyph(
								SpriteAtlasGlyphs.this.renderLayerSet,
								SpriteAtlasGlyphs.this.atlasTexture.getGlTextureView(),
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

	@Environment(EnvType.CLIENT)
	/**
	 * {@code AtlasGlyph}.
	 */
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
			float f = x + this.getEffectiveMinX();
			float g = x + this.getEffectiveMaxX();
			float h = y + this.getEffectiveMinY();
			float i = y + this.getEffectiveMaxY();
			vertexConsumer
					.vertex(matrix, f, h, z)
					.texture(this.sprite.getMinU(), this.sprite.getMinV())
					.color(color)
					.light(light);
			vertexConsumer
					.vertex(matrix, f, i, z)
					.texture(this.sprite.getMinU(), this.sprite.getMaxV())
					.color(color)
					.light(light);
			vertexConsumer
					.vertex(matrix, g, i, z)
					.texture(this.sprite.getMaxU(), this.sprite.getMaxV())
					.color(color)
					.light(light);
			vertexConsumer
					.vertex(matrix, g, h, z)
					.texture(this.sprite.getMaxU(), this.sprite.getMinV())
					.color(color)
					.light(light);
		}

		@Override
		public RenderLayer getRenderLayer(TextRenderer.TextLayerType type) {
			return this.renderTypes.getRenderLayer(type);
		}

		@Override
		public RenderPipeline getPipeline() {
			return this.renderTypes.guiPipeline();
		}
	}
}
