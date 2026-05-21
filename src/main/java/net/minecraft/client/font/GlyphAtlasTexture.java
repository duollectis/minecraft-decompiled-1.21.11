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

@Environment(EnvType.CLIENT)
/**
 * {@code GlyphAtlasTexture}.
 */
public class GlyphAtlasTexture extends AbstractTexture implements DynamicTexture {

	private static final int SLOT_LENGTH = 256;
	private final TextRenderLayerSet textRenderLayers;
	private final boolean hasColor;
	private final GlyphAtlasTexture.Slot rootSlot;

	public GlyphAtlasTexture(Supplier<String> nameSupplier, TextRenderLayerSet textRenderLayers, boolean hasColor) {
		this.hasColor = hasColor;
		this.rootSlot = new GlyphAtlasTexture.Slot(0, 0, 256, 256);
		GpuDevice gpuDevice = RenderSystem.getDevice();
		this.glTexture =
				gpuDevice.createTexture(
						nameSupplier,
						7,
						hasColor ? TextureFormat.RGBA8 : TextureFormat.RED8,
						256,
						256,
						1,
						1
				);
		this.sampler = RenderSystem.getSamplerCache().getRepeated(FilterMode.NEAREST);
		this.glTextureView = gpuDevice.createTextureView(this.glTexture);
		this.textRenderLayers = textRenderLayers;
	}

	public @Nullable BakedGlyphImpl bake(GlyphMetrics metrics, UploadableGlyph glyph) {
		if (glyph.hasColor() != this.hasColor) {
			return null;
		}
		else {
			GlyphAtlasTexture.Slot slot = this.rootSlot.findSlotFor(glyph);
			if (slot != null) {
				glyph.upload(slot.x, slot.y, this.getGlTexture());
				float f = 256.0F;
				float g = 256.0F;
				float h = 0.01F;
				return new BakedGlyphImpl(
						metrics,
						this.textRenderLayers,
						this.getGlTextureView(),
						(slot.x + 0.01F) / 256.0F,
						(slot.x - 0.01F + glyph.getWidth()) / 256.0F,
						(slot.y + 0.01F) / 256.0F,
						(slot.y - 0.01F + glyph.getHeight()) / 256.0F,
						glyph.getXMin(),
						glyph.getXMax(),
						glyph.getYMin(),
						glyph.getYMax()
				);
			}
			else {
				return null;
			}
		}
	}

	@Override
	public void save(Identifier id, Path path) {
		if (this.glTexture != null) {
			String string = id.toUnderscoreSeparatedString();
			TextureUtil.writeAsPNG(
					path,
					string,
					this.glTexture,
					0,
					color -> (color & 0xFF000000) == 0 ? -16777216 : color
			);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Slot}.
	 */
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
			if (this.subSlot1 != null && this.subSlot2 != null) {
				GlyphAtlasTexture.Slot slot = this.subSlot1.findSlotFor(glyph);
				if (slot == null) {
					slot = this.subSlot2.findSlotFor(glyph);
				}

				return slot;
			}
			else if (this.occupied) {
				return null;
			}
			else {
				int i = glyph.getWidth();
				int j = glyph.getHeight();
				if (i > this.width || j > this.height) {
					return null;
				}
				else if (i == this.width && j == this.height) {
					this.occupied = true;
					return this;
				}
				else {
					int k = this.width - i;
					int l = this.height - j;
					if (k > l) {
						this.subSlot1 = new GlyphAtlasTexture.Slot(this.x, this.y, i, this.height);
						this.subSlot2 =
								new GlyphAtlasTexture.Slot(this.x + i + 1, this.y, this.width - i - 1, this.height);
					}
					else {
						this.subSlot1 = new GlyphAtlasTexture.Slot(this.x, this.y, this.width, j);
						this.subSlot2 =
								new GlyphAtlasTexture.Slot(this.x, this.y + j + 1, this.width, this.height - j - 1);
					}

					return this.subSlot1.findSlotFor(glyph);
				}
			}
		}
	}
}
