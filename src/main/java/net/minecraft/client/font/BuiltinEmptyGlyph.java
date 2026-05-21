package net.minecraft.client.font;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
/**
 * {@code BuiltinEmptyGlyph}.
 */
public enum BuiltinEmptyGlyph implements GlyphMetrics {
	WHITE(() -> createRectImage(5, 8, (x, y) -> -1)),
	MISSING(() -> {
		int i = 5;
		int j = 8;
		return createRectImage(
				5, 8, (x, y) -> {
					boolean bl = x == 0 || x + 1 == 5 || y == 0 || y + 1 == 8;
					return bl ? -1 : 0;
				}
		);
	});

	final NativeImage image;

	private static NativeImage createRectImage(int width, int height, BuiltinEmptyGlyph.ColorSupplier colorSupplier) {
		NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				nativeImage.setColorArgb(j, i, colorSupplier.getColor(j, i));
			}
		}

		nativeImage.untrack();
		return nativeImage;
	}

	private BuiltinEmptyGlyph(final Supplier<NativeImage> imageSupplier) {
		this.image = imageSupplier.get();
	}

	@Override
	public float getAdvance() {
		return this.image.getWidth() + 1;
	}

	/**
	 * Bake.
	 *
	 * @param glyphBaker glyph baker
	 *
	 * @return @Nullable BakedGlyphImpl — результат операции
	 */
	public @Nullable BakedGlyphImpl bake(GlyphBaker glyphBaker) {
		return glyphBaker.bake(
				this,
				new UploadableGlyph() {
					@Override
					public int getWidth() {
						return BuiltinEmptyGlyph.this.image.getWidth();
					}

					@Override
					public int getHeight() {
						return BuiltinEmptyGlyph.this.image.getHeight();
					}

					@Override
					public float getOversample() {
						return 1.0F;
					}

					@Override
					public void upload(int x, int y, GpuTexture texture) {
						RenderSystem.getDevice()
						            .createCommandEncoder()
						            .writeToTexture(
								            texture,
								            BuiltinEmptyGlyph.this.image,
								            0,
								            0,
								            x,
								            y,
								            BuiltinEmptyGlyph.this.image.getWidth(),
								            BuiltinEmptyGlyph.this.image.getHeight(),
								            0,
								            0
						            );
					}

					@Override
					public boolean hasColor() {
						return true;
					}
				}
		);
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code ColorSupplier}.
	 */
	interface ColorSupplier {

		int getColor(int x, int y);
	}
}
