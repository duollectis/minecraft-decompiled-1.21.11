package net.minecraft.client.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code MipmapHelper}.
 */
public class MipmapHelper {

	private static final String ITEM_PREFIX = "item/";
	private static final float DEFAULT_ALPHA_THRESHOLD = 0.5F;
	private static final float STRICT_CUTOUT_ALPHA_THRESHOLD = 0.3F;

	private MipmapHelper() {
	}

	private static float getOpacityCoverage(NativeImage image, float alphaThreshold, float alphaMulti) {
		int i = image.getWidth();
		int j = image.getHeight();
		float f = 0.0F;
		int k = 4;

		for (int l = 0; l < j - 1; l++) {
			for (int m = 0; m < i - 1; m++) {
				float g = Math.clamp(ColorHelper.getAlphaFloat(image.getColorArgb(m, l)) * alphaMulti, 0.0F, 1.0F);
				float h = Math.clamp(ColorHelper.getAlphaFloat(image.getColorArgb(m + 1, l)) * alphaMulti, 0.0F, 1.0F);
				float n = Math.clamp(ColorHelper.getAlphaFloat(image.getColorArgb(m, l + 1)) * alphaMulti, 0.0F, 1.0F);
				float
						o =
						Math.clamp(
								ColorHelper.getAlphaFloat(image.getColorArgb(m + 1, l + 1)) * alphaMulti,
								0.0F,
								1.0F
						);
				float p = 0.0F;

				for (int q = 0; q < 4; q++) {
					float r = (q + 0.5F) / 4.0F;

					for (int s = 0; s < 4; s++) {
						float t = (s + 0.5F) / 4.0F;
						float u = g * (1.0F - t) * (1.0F - r) + h * t * (1.0F - r) + n * (1.0F - t) * r + o * t * r;
						if (u > alphaThreshold) {
							p++;
						}
					}
				}

				f += p / 16.0F;
			}
		}

		return f / ((i - 1) * (j - 1));
	}

	private static void adjustAlphaForTargetCoverage(
			NativeImage image,
			float targetCoverage,
			float alphaThreshold,
			float cutoffBias
	) {
		float f = 0.0F;
		float g = 4.0F;
		float h = 1.0F;
		float i = 1.0F;
		float j = Float.MAX_VALUE;
		int k = image.getWidth();
		int l = image.getHeight();

		for (int m = 0; m < 5; m++) {
			float n = getOpacityCoverage(image, alphaThreshold, h);
			float o = Math.abs(n - targetCoverage);
			if (o < j) {
				j = o;
				i = h;
			}

			if (n < targetCoverage) {
				f = h;
			}
			else {
				if (!(n > targetCoverage)) {
					break;
				}

				g = h;
			}

			h = (f + g) * 0.5F;
		}

		for (int m = 0; m < l; m++) {
			for (int p = 0; p < k; p++) {
				int q = image.getColorArgb(p, m);
				float r = ColorHelper.getAlphaFloat(q);
				r = r * i + cutoffBias + 0.025F;
				r = Math.clamp(r, 0.0F, 1.0F);
				image.setColorArgb(p, m, ColorHelper.withAlpha(r, q));
			}
		}
	}

	public static NativeImage[] getMipmapLevelsImages(
			Identifier id, NativeImage[] mipmapLevelImages, int mipmapLevels, MipmapStrategy strategy, float cutoffBias
	) {
		if (strategy == MipmapStrategy.AUTO) {
			strategy = hasAlpha(mipmapLevelImages[0]) ? MipmapStrategy.CUTOUT : MipmapStrategy.MEAN;
		}

		if (mipmapLevelImages.length == 1 && !id.getPath().startsWith("item/")) {
			if (strategy == MipmapStrategy.CUTOUT || strategy == MipmapStrategy.STRICT_CUTOUT) {
				TextureUtil.solidify(mipmapLevelImages[0]);
			}
			else if (strategy == MipmapStrategy.DARK_CUTOUT) {
				TextureUtil.fillEmptyAreasWithDarkColor(mipmapLevelImages[0]);
			}
		}

		if (mipmapLevels + 1 <= mipmapLevelImages.length) {
			return mipmapLevelImages;
		}
		else {
			NativeImage[] nativeImages = new NativeImage[mipmapLevels + 1];
			nativeImages[0] = mipmapLevelImages[0];
			boolean
					bl =
					strategy == MipmapStrategy.CUTOUT || strategy == MipmapStrategy.STRICT_CUTOUT
							|| strategy == MipmapStrategy.DARK_CUTOUT;
			float f = strategy == MipmapStrategy.STRICT_CUTOUT ? 0.3F : 0.5F;
			float g = bl ? getOpacityCoverage(mipmapLevelImages[0], f, 1.0F) : 0.0F;

			for (int i = 1; i <= mipmapLevels; i++) {
				if (i < mipmapLevelImages.length) {
					nativeImages[i] = mipmapLevelImages[i];
				}
				else {
					NativeImage nativeImage = nativeImages[i - 1];
					NativeImage
							nativeImage2 =
							new NativeImage(nativeImage.getWidth() >> 1, nativeImage.getHeight() >> 1, false);
					int j = nativeImage2.getWidth();
					int k = nativeImage2.getHeight();

					for (int l = 0; l < j; l++) {
						for (int m = 0; m < k; m++) {
							int n = nativeImage.getColorArgb(l * 2 + 0, m * 2 + 0);
							int o = nativeImage.getColorArgb(l * 2 + 1, m * 2 + 0);
							int p = nativeImage.getColorArgb(l * 2 + 0, m * 2 + 1);
							int q = nativeImage.getColorArgb(l * 2 + 1, m * 2 + 1);
							int r;
							if (strategy == MipmapStrategy.DARK_CUTOUT) {
								r = blendDarkenedCutout(n, o, p, q);
							}
							else {
								r = ColorHelper.interpolate(n, o, p, q);
							}

							nativeImage2.setColorArgb(l, m, r);
						}
					}

					nativeImages[i] = nativeImage2;
				}

				if (bl) {
					adjustAlphaForTargetCoverage(nativeImages[i], g, f, cutoffBias);
				}
			}

			return nativeImages;
		}
	}

	private static boolean hasAlpha(NativeImage image) {
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				if (ColorHelper.getAlpha(image.getColorArgb(i, j)) == 0) {
					return true;
				}
			}
		}

		return false;
	}

	private static int blendDarkenedCutout(int nw, int ne, int sw, int se) {
		float f = 0.0F;
		float g = 0.0F;
		float h = 0.0F;
		float i = 0.0F;
		if (ColorHelper.getAlpha(nw) != 0) {
			f += ColorHelper.srgbToLinear(ColorHelper.getAlpha(nw));
			g += ColorHelper.srgbToLinear(ColorHelper.getRed(nw));
			h += ColorHelper.srgbToLinear(ColorHelper.getGreen(nw));
			i += ColorHelper.srgbToLinear(ColorHelper.getBlue(nw));
		}

		if (ColorHelper.getAlpha(ne) != 0) {
			f += ColorHelper.srgbToLinear(ColorHelper.getAlpha(ne));
			g += ColorHelper.srgbToLinear(ColorHelper.getRed(ne));
			h += ColorHelper.srgbToLinear(ColorHelper.getGreen(ne));
			i += ColorHelper.srgbToLinear(ColorHelper.getBlue(ne));
		}

		if (ColorHelper.getAlpha(sw) != 0) {
			f += ColorHelper.srgbToLinear(ColorHelper.getAlpha(sw));
			g += ColorHelper.srgbToLinear(ColorHelper.getRed(sw));
			h += ColorHelper.srgbToLinear(ColorHelper.getGreen(sw));
			i += ColorHelper.srgbToLinear(ColorHelper.getBlue(sw));
		}

		if (ColorHelper.getAlpha(se) != 0) {
			f += ColorHelper.srgbToLinear(ColorHelper.getAlpha(se));
			g += ColorHelper.srgbToLinear(ColorHelper.getRed(se));
			h += ColorHelper.srgbToLinear(ColorHelper.getGreen(se));
			i += ColorHelper.srgbToLinear(ColorHelper.getBlue(se));
		}

		f /= 4.0F;
		g /= 4.0F;
		h /= 4.0F;
		i /= 4.0F;
		return ColorHelper.getArgb(
				ColorHelper.linearToSrgb(f),
				ColorHelper.linearToSrgb(g),
				ColorHelper.linearToSrgb(h),
				ColorHelper.linearToSrgb(i)
		);
	}
}
