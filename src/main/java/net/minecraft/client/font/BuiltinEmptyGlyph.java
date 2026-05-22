package net.minecraft.client.font;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Встроенные пустые глифы движка: белый прямоугольник и символ «отсутствующего» глифа.
 * Оба реализуют {@link GlyphMetrics} напрямую и создают своё изображение при инициализации.
 * <p>
 * {@code WHITE} — сплошной белый прямоугольник, используется для эффектов (подчёркивание, фон).
 * {@code MISSING} — рамка (граница белая, внутри прозрачно), отображается для неизвестных символов.
 */
@Environment(EnvType.CLIENT)
public enum BuiltinEmptyGlyph implements GlyphMetrics {
	WHITE(() -> createRectImage(5, 8, (x, y) -> -1)),
	MISSING(() -> {
		// Рисуем только граничные пиксели: x==0, x==4, y==0, y==7
		return createRectImage(5, 8, (x, y) -> {
			boolean isBorder = x == 0 || x + 1 == 5 || y == 0 || y + 1 == 8;
			return isBorder ? -1 : 0;
		});
	});

	final NativeImage image;

	BuiltinEmptyGlyph(final Supplier<NativeImage> imageSupplier) {
		image = imageSupplier.get();
	}

	@Override
	public float getAdvance() {
		return image.getWidth() + 1;
	}

	/**
	 * Запекает этот глиф в атлас через переданный {@link GlyphBaker}.
	 *
	 * @param glyphBaker пекарь, управляющий атласом текстур
	 * @return запечённый глиф или {@code null}, если атлас переполнен
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

	private static NativeImage createRectImage(int width, int height, BuiltinEmptyGlyph.ColorSupplier colorSupplier) {
		NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				nativeImage.setColorArgb(x, y, colorSupplier.getColor(x, y));
			}
		}

		nativeImage.untrack();
		return nativeImage;
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	interface ColorSupplier {

		int getColor(int x, int y);
	}
}
