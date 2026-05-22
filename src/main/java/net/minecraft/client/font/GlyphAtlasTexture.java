package net.minecraft.client.font;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.DynamicTexture;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Атлас текстур для запечённых глифов. Использует алгоритм бинарного разбиения
 * ({@link Slot}) для упаковки глифов в единую текстуру размером {@value ATLAS_SIZE}×{@value ATLAS_SIZE}.
 */
@Environment(EnvType.CLIENT)
public class GlyphAtlasTexture extends AbstractTexture implements DynamicTexture {

	private static final int ATLAS_SIZE = 256;
	private static final float ATLAS_SIZE_F = 256.0F;
	private static final float UV_PADDING = 0.01F;

	private final TextRenderLayerSet textRenderLayers;
	private final boolean hasColor;
	private final GlyphAtlasTexture.Slot rootSlot;

	public GlyphAtlasTexture(Supplier<String> nameSupplier, TextRenderLayerSet textRenderLayers, boolean hasColor) {
		this.hasColor = hasColor;
		rootSlot = new GlyphAtlasTexture.Slot(0, 0, ATLAS_SIZE, ATLAS_SIZE);
		GpuDevice gpuDevice = RenderSystem.getDevice();
		glTexture = gpuDevice.createTexture(
				nameSupplier,
				7,
				hasColor ? TextureFormat.RGBA8 : TextureFormat.RED8,
				ATLAS_SIZE,
				ATLAS_SIZE,
				1,
				1
		);
		sampler = RenderSystem.getSamplerCache().getRepeated(FilterMode.NEAREST);
		glTextureView = gpuDevice.createTextureView(glTexture);
		this.textRenderLayers = textRenderLayers;
	}

	/**
	 * Запекает глиф в атлас текстур, находя свободный слот и загружая пиксели.
	 * Возвращает {@code null}, если цветовой формат глифа не совпадает с форматом атласа
	 * или в атласе нет свободного места.
	 *
	 * @param metrics метрики глифа (размеры, отступы)
	 * @param glyph загружаемый глиф с пиксельными данными
	 * @return запечённый глиф с UV-координатами, или {@code null} при неудаче
	 */
	public @Nullable BakedGlyphImpl bake(GlyphMetrics metrics, UploadableGlyph glyph) {
		if (glyph.hasColor() != hasColor) {
			return null;
		}

		GlyphAtlasTexture.Slot slot = rootSlot.findSlotFor(glyph);
		if (slot == null) {
			return null;
		}

		glyph.upload(slot.x, slot.y, getGlTexture());
		return new BakedGlyphImpl(
				metrics,
				textRenderLayers,
				getGlTextureView(),
				(slot.x + UV_PADDING) / ATLAS_SIZE_F,
				(slot.x - UV_PADDING + glyph.getWidth()) / ATLAS_SIZE_F,
				(slot.y + UV_PADDING) / ATLAS_SIZE_F,
				(slot.y - UV_PADDING + glyph.getHeight()) / ATLAS_SIZE_F,
				glyph.getXMin(),
				glyph.getXMax(),
				glyph.getYMin(),
				glyph.getYMax()
		);
	}

	@Override
	public void save(Identifier id, Path path) {
		if (glTexture == null) {
			return;
		}

		String name = id.toUnderscoreSeparatedString();
		TextureUtil.writeAsPNG(
				path,
				name,
				glTexture,
				0,
				color -> (color & 0xFF000000) == 0 ? -16777216 : color
		);
	}

	/**
	 * Узел бинарного дерева разбиения атласа. Рекурсивно делит свободное пространство
	 * на два подслота при размещении глифа, минимизируя фрагментацию.
	 */
	@Environment(EnvType.CLIENT)
	static class Slot {

		final int x;
		final int y;
		private final int width;
		private final int height;
		private GlyphAtlasTexture.@Nullable Slot subSlot1;
		private GlyphAtlasTexture.@Nullable Slot subSlot2;
		private boolean occupied;

		Slot(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		GlyphAtlasTexture.@Nullable Slot findSlotFor(UploadableGlyph glyph) {
			if (subSlot1 != null && subSlot2 != null) {
				GlyphAtlasTexture.Slot found = subSlot1.findSlotFor(glyph);
				return found != null ? found : subSlot2.findSlotFor(glyph);
			}

			if (occupied) {
				return null;
			}

			int glyphWidth = glyph.getWidth();
			int glyphHeight = glyph.getHeight();

			if (glyphWidth > width || glyphHeight > height) {
				return null;
			}

			if (glyphWidth == width && glyphHeight == height) {
				occupied = true;
				return this;
			}

			int remainingWidth = width - glyphWidth;
			int remainingHeight = height - glyphHeight;

			// Разбиваем по более широкой оси, чтобы минимизировать фрагментацию
			if (remainingWidth > remainingHeight) {
				subSlot1 = new GlyphAtlasTexture.Slot(x, y, glyphWidth, height);
				subSlot2 = new GlyphAtlasTexture.Slot(x + glyphWidth + 1, y, width - glyphWidth - 1, height);
			} else {
				subSlot1 = new GlyphAtlasTexture.Slot(x, y, width, glyphHeight);
				subSlot2 = new GlyphAtlasTexture.Slot(x, y + glyphHeight + 1, width, height - glyphHeight - 1);
			}

			return subSlot1.findSlotFor(glyph);
		}
	}
}
