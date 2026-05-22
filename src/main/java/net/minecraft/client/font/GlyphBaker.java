package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Управляет набором атласов текстур для одного шрифта. При нехватке места
 * в существующих атласах автоматически создаёт новый и регистрирует его в {@link TextureManager}.
 */
@Environment(EnvType.CLIENT)
public class GlyphBaker implements AutoCloseable {

	private final TextureManager textureManager;
	private final Identifier fontId;
	private final List<GlyphAtlasTexture> glyphAtlases = new ArrayList<>();

	public GlyphBaker(TextureManager textureManager, Identifier fontId) {
		this.textureManager = textureManager;
		this.fontId = fontId;
	}

	public void clear() {
		int atlasCount = glyphAtlases.size();
		glyphAtlases.clear();

		for (int index = 0; index < atlasCount; index++) {
			textureManager.destroyTexture(getAtlasId(index));
		}
	}

	@Override
	public void close() {
		clear();
	}

	/**
	 * Запекает глиф в первый подходящий атлас. Если ни один атлас не принял глиф,
	 * создаёт новый атлас нужного цветового формата и повторяет попытку.
	 *
	 * @param metrics метрики глифа
	 * @param glyph загружаемый глиф с пиксельными данными
	 * @return запечённый глиф, или {@code null} если глиф не поместился даже в новый атлас
	 */
	public @Nullable BakedGlyphImpl bake(GlyphMetrics metrics, UploadableGlyph glyph) {
		for (GlyphAtlasTexture atlas : glyphAtlases) {
			BakedGlyphImpl baked = atlas.bake(metrics, glyph);
			if (baked != null) {
				return baked;
			}
		}

		int newIndex = glyphAtlases.size();
		Identifier atlasId = getAtlasId(newIndex);
		boolean colored = glyph.hasColor();
		TextRenderLayerSet layers = colored
				? TextRenderLayerSet.of(atlasId)
				: TextRenderLayerSet.ofIntensity(atlasId);
		GlyphAtlasTexture newAtlas = new GlyphAtlasTexture(atlasId::toString, layers, colored);
		glyphAtlases.add(newAtlas);
		textureManager.registerTexture(atlasId, newAtlas);
		return newAtlas.bake(metrics, glyph);
	}

	private Identifier getAtlasId(int atlasIndex) {
		return fontId.withSuffixedPath("/" + atlasIndex);
	}
}
