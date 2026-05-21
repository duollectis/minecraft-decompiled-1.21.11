package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code GlyphBaker}.
 */
public class GlyphBaker implements AutoCloseable {

	private final TextureManager textureManager;
	private final Identifier fontId;
	private final List<GlyphAtlasTexture> glyphAtlases = new ArrayList<>();

	public GlyphBaker(TextureManager textureManager, Identifier fontId) {
		this.textureManager = textureManager;
		this.fontId = fontId;
	}

	public void clear() {
		int i = this.glyphAtlases.size();
		this.glyphAtlases.clear();

		for (int j = 0; j < i; j++) {
			this.textureManager.destroyTexture(this.getAtlasId(j));
		}
	}

	@Override
	public void close() {
		this.clear();
	}

	public @Nullable BakedGlyphImpl bake(GlyphMetrics metrics, UploadableGlyph glyph) {
		for (GlyphAtlasTexture glyphAtlasTexture : this.glyphAtlases) {
			BakedGlyphImpl bakedGlyphImpl = glyphAtlasTexture.bake(metrics, glyph);
			if (bakedGlyphImpl != null) {
				return bakedGlyphImpl;
			}
		}

		int i = this.glyphAtlases.size();
		Identifier identifier = this.getAtlasId(i);
		boolean bl = glyph.hasColor();
		TextRenderLayerSet
				textRenderLayerSet =
				bl ? TextRenderLayerSet.of(identifier) : TextRenderLayerSet.ofIntensity(identifier);
		GlyphAtlasTexture glyphAtlasTexture2 = new GlyphAtlasTexture(identifier::toString, textRenderLayerSet, bl);
		this.glyphAtlases.add(glyphAtlasTexture2);
		this.textureManager.registerTexture(identifier, glyphAtlasTexture2);
		return glyphAtlasTexture2.bake(metrics, glyph);
	}

	private Identifier getAtlasId(int atlasIndex) {
		return this.fontId.withSuffixedPath("/" + atlasIndex);
	}
}
