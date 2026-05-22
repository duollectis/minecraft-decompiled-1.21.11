package net.minecraft.util.math;

import net.minecraft.util.Util;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Утилитарный класс для работы с цветами в формате ARGB (packed int).
 * Содержит LUT-таблицы для быстрого перевода между sRGB и линейным цветовым пространством,
 * а также операции смешивания, масштабирования, интерполяции и конвертации цветов.
 */
public class ColorHelper {

	private static final int LINEAR_TO_SRGB_LUT_LENGTH = 1024;
	private static final int RGB_CHANNEL_MASK = 0xFFFFFF;

	private static final short[] SRGB_TO_LINEAR = Util.make(
			new short[256], out -> {
				for (int i = 0; i < out.length; i++) {
					float srgb = i / 255.0F;
					out[i] = (short) Math.round(computeSrgbToLinear(srgb) * 1023.0F);
				}
			}
	);

	private static final byte[] LINEAR_TO_SRGB = Util.make(
			new byte[LINEAR_TO_SRGB_LUT_LENGTH], out -> {
				for (int i = 0; i < out.length; i++) {
					float linear = i / 1023.0F;
					out[i] = (byte) Math.round(computeLinearToSrgb(linear) * 255.0F);
				}
			}
	);

	private static float computeSrgbToLinear(float srgb) {
		return srgb >= 0.04045F
				? (float) Math.pow((srgb + 0.055) / 1.055, 2.4)
				: srgb / 12.92F;
	}

	private static float computeLinearToSrgb(float linear) {
		return linear >= 0.0031308F
				? (float) (1.055 * Math.pow(linear, 0.4166666666666667) - 0.055)
				: 12.92F * linear;
	}

	public static float srgbToLinear(int srgb) {
		return SRGB_TO_LINEAR[srgb] / 1023.0F;
	}

	public static int linearToSrgb(float linear) {
		return LINEAR_TO_SRGB[MathHelper.floor(linear * 1023.0F)] & 0xFF;
	}

	/**
	 * Усредняет 4 цвета ARGB с корректным учётом гамма-коррекции (через линейное пространство).
	 * Используется при билинейной фильтрации текстур.
	 */
	public static int interpolate(int a, int b, int c, int d) {
		return getArgb(
				(getAlpha(a) + getAlpha(b) + getAlpha(c) + getAlpha(d)) / 4,
				averageSrgbIntensities(getRed(a), getRed(b), getRed(c), getRed(d)),
				averageSrgbIntensities(getGreen(a), getGreen(b), getGreen(c), getGreen(d)),
				averageSrgbIntensities(getBlue(a), getBlue(b), getBlue(c), getBlue(d))
		);
	}

	private static int averageSrgbIntensities(int a, int b, int c, int d) {
		int linearAvg = (SRGB_TO_LINEAR[a] + SRGB_TO_LINEAR[b] + SRGB_TO_LINEAR[c] + SRGB_TO_LINEAR[d]) / 4;
		return LINEAR_TO_SRGB[linearAvg] & 0xFF;
	}

	public static int getAlpha(int argb) {
		return argb >>> 24;
	}

	public static int getRed(int argb) {
		return argb >> 16 & 0xFF;
	}

	public static int getGreen(int argb) {
		return argb >> 8 & 0xFF;
	}

	public static int getBlue(int argb) {
		return argb & 0xFF;
	}

	public static int getArgb(int alpha, int red, int green, int blue) {
		return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
	}

	public static int getArgb(int red, int green, int blue) {
		return getArgb(255, red, green, blue);
	}

	public static int getArgb(Vec3d rgb) {
		return getArgb(
				channelFromFloat((float) rgb.getX()),
				channelFromFloat((float) rgb.getY()),
				channelFromFloat((float) rgb.getZ())
		);
	}

	public static int mix(int first, int second) {
		if (first == -1) {
			return second;
		}

		if (second == -1) {
			return first;
		}

		return getArgb(
				getAlpha(first) * getAlpha(second) / 255,
				getRed(first) * getRed(second) / 255,
				getGreen(first) * getGreen(second) / 255,
				getBlue(first) * getBlue(second) / 255
		);
	}

	public static int add(int a, int b) {
		return getArgb(
				getAlpha(a),
				Math.min(getRed(a) + getRed(b), 255),
				Math.min(getGreen(a) + getGreen(b), 255),
				Math.min(getBlue(a) + getBlue(b), 255)
		);
	}

	public static int subtract(int a, int b) {
		return getArgb(
				getAlpha(a),
				Math.max(getRed(a) - getRed(b), 0),
				Math.max(getGreen(a) - getGreen(b), 0),
				Math.max(getBlue(a) - getBlue(b), 0)
		);
	}

	public static int scaleAlpha(int argb, float scale) {
		if (argb == 0 || scale <= 0.0F) {
			return 0;
		}

		return scale >= 1.0F ? argb : withAlpha(getAlphaFloat(argb) * scale, argb);
	}

	public static int scaleRgb(int argb, float scale) {
		return scaleRgb(argb, scale, scale, scale);
	}

	public static int scaleRgb(int argb, float redScale, float greenScale, float blueScale) {
		return getArgb(
				getAlpha(argb),
				Math.clamp((long) ((int) (getRed(argb) * redScale)), 0, 255),
				Math.clamp((long) ((int) (getGreen(argb) * greenScale)), 0, 255),
				Math.clamp((long) ((int) (getBlue(argb) * blueScale)), 0, 255)
		);
	}

	public static int scaleRgb(int argb, int scale) {
		return getArgb(
				getAlpha(argb),
				Math.clamp((long) getRed(argb) * scale / 255L, 0, 255),
				Math.clamp((long) getGreen(argb) * scale / 255L, 0, 255),
				Math.clamp((long) getBlue(argb) * scale / 255L, 0, 255)
		);
	}

	public static int grayscale(int argb) {
		int luminance = (int) (getRed(argb) * 0.3F + getGreen(argb) * 0.59F + getBlue(argb) * 0.11F);
		return getArgb(getAlpha(argb), luminance, luminance, luminance);
	}

	public static int alphaBlend(int a, int b) {
		int alphaA = getAlpha(a);
		int alphaB = getAlpha(b);

		if (alphaB == 255) {
			return b;
		}

		if (alphaB == 0) {
			return a;
		}

		int blendedAlpha = alphaB + alphaA * (255 - alphaB) / 255;
		return getArgb(
				blendedAlpha,
				blend(blendedAlpha, alphaB, getRed(a), getRed(b)),
				blend(blendedAlpha, alphaB, getGreen(a), getGreen(b)),
				blend(blendedAlpha, alphaB, getBlue(a), getBlue(b))
		);
	}

	private static int blend(int blendedAlpha, int alpha, int a, int b) {
		return (b * alpha + a * (blendedAlpha - alpha)) / blendedAlpha;
	}

	public static int lerp(float delta, int start, int end) {
		int alpha = MathHelper.lerp(delta, getAlpha(start), getAlpha(end));
		int red = MathHelper.lerp(delta, getRed(start), getRed(end));
		int green = MathHelper.lerp(delta, getGreen(start), getGreen(end));
		int blue = MathHelper.lerp(delta, getBlue(start), getBlue(end));
		return getArgb(alpha, red, green, blue);
	}

	public static int lerpLinear(float delta, int start, int end) {
		return getArgb(
				MathHelper.lerp(delta, getAlpha(start), getAlpha(end)),
				LINEAR_TO_SRGB[MathHelper.lerp(delta, SRGB_TO_LINEAR[getRed(start)], SRGB_TO_LINEAR[getRed(end)])] & 0xFF,
				LINEAR_TO_SRGB[MathHelper.lerp(delta, SRGB_TO_LINEAR[getGreen(start)], SRGB_TO_LINEAR[getGreen(end)])] & 0xFF,
				LINEAR_TO_SRGB[MathHelper.lerp(delta, SRGB_TO_LINEAR[getBlue(start)], SRGB_TO_LINEAR[getBlue(end)])] & 0xFF
		);
	}

	public static int fullAlpha(int argb) {
		return argb | 0xFF000000;
	}

	public static int zeroAlpha(int argb) {
		return argb & RGB_CHANNEL_MASK;
	}

	public static int withAlpha(int alpha, int rgb) {
		return alpha << 24 | rgb & RGB_CHANNEL_MASK;
	}

	public static int withAlpha(float alpha, int color) {
		return channelFromFloat(alpha) << 24 | color & RGB_CHANNEL_MASK;
	}

	public static int getWhite(float alpha) {
		return channelFromFloat(alpha) << 24 | RGB_CHANNEL_MASK;
	}

	public static int whiteWithAlpha(int alpha) {
		return alpha << 24 | RGB_CHANNEL_MASK;
	}

	public static int toAlpha(float alpha) {
		return channelFromFloat(alpha) << 24;
	}

	public static int toAlpha(int alpha) {
		return alpha << 24;
	}

	public static int fromFloats(float alpha, float red, float green, float blue) {
		return getArgb(channelFromFloat(alpha), channelFromFloat(red), channelFromFloat(green), channelFromFloat(blue));
	}

	public static Vector3f toRgbVector(int rgb) {
		return new Vector3f(getRedFloat(rgb), getGreenFloat(rgb), getBlueFloat(rgb));
	}

	public static Vector4f toRgbaVector(int argb) {
		return new Vector4f(getRedFloat(argb), getGreenFloat(argb), getBlueFloat(argb), getAlphaFloat(argb));
	}

	public static int average(int first, int second) {
		return getArgb(
				(getAlpha(first) + getAlpha(second)) / 2,
				(getRed(first) + getRed(second)) / 2,
				(getGreen(first) + getGreen(second)) / 2,
				(getBlue(first) + getBlue(second)) / 2
		);
	}

	public static int channelFromFloat(float value) {
		return MathHelper.floor(value * 255.0F);
	}

	public static float getAlphaFloat(int argb) {
		return floatFromChannel(getAlpha(argb));
	}

	public static float getRedFloat(int argb) {
		return floatFromChannel(getRed(argb));
	}

	public static float getGreenFloat(int argb) {
		return floatFromChannel(getGreen(argb));
	}

	public static float getBlueFloat(int argb) {
		return floatFromChannel(getBlue(argb));
	}

	private static float floatFromChannel(int channel) {
		return channel / 255.0F;
	}

	public static int toAbgr(int argb) {
		return argb & -16711936 | (argb & 0xFF0000) >> 16 | (argb & 0xFF) << 16;
	}

	public static int fromAbgr(int abgr) {
		return toAbgr(abgr);
	}

	/**
	 * Изменяет яркость цвета ARGB, сохраняя оттенок (hue) и насыщенность (saturation).
	 * Реализует алгоритм HSV: конвертирует RGB → HSV, заменяет V на {@code brightness}, возвращает обратно.
	 */
	public static int withBrightness(int argb, float brightness) {
		int red = getRed(argb);
		int green = getGreen(argb);
		int blue = getBlue(argb);
		int alpha = getAlpha(argb);

		int maxChannel = Math.max(Math.max(red, green), blue);
		int minChannel = Math.min(Math.min(red, green), blue);
		float chroma = maxChannel - minChannel;

		float saturation = maxChannel != 0 ? chroma / maxChannel : 0.0F;

		if (saturation == 0.0F) {
			int gray = Math.round(brightness * 255.0F);
			return getArgb(alpha, gray, gray, gray);
		}

		float hue = computeHue(red, green, blue, maxChannel, chroma);
		float hueSector = (hue - (float) Math.floor(hue)) * 6.0F;
		float hueFraction = hueSector - (float) Math.floor(hueSector);

		float p1 = brightness * (1.0F - saturation);
		float p2 = brightness * (1.0F - saturation * hueFraction);
		float p3 = brightness * (1.0F - saturation * (1.0F - hueFraction));

		return switch ((int) hueSector) {
			case 0 -> getArgb(alpha, Math.round(brightness * 255.0F), Math.round(p3 * 255.0F), Math.round(p1 * 255.0F));
			case 1 -> getArgb(alpha, Math.round(p2 * 255.0F), Math.round(brightness * 255.0F), Math.round(p1 * 255.0F));
			case 2 -> getArgb(alpha, Math.round(p1 * 255.0F), Math.round(brightness * 255.0F), Math.round(p3 * 255.0F));
			case 3 -> getArgb(alpha, Math.round(p1 * 255.0F), Math.round(p2 * 255.0F), Math.round(brightness * 255.0F));
			case 4 -> getArgb(alpha, Math.round(p3 * 255.0F), Math.round(p1 * 255.0F), Math.round(brightness * 255.0F));
			default -> getArgb(alpha, Math.round(brightness * 255.0F), Math.round(p1 * 255.0F), Math.round(p2 * 255.0F));
		};
	}

	private static float computeHue(int red, int green, int blue, int maxChannel, float chroma) {
		float redFactor = (maxChannel - red) / chroma;
		float greenFactor = (maxChannel - green) / chroma;
		float blueFactor = (maxChannel - blue) / chroma;

		float hue;
		if (red == maxChannel) {
			hue = blueFactor - greenFactor;
		} else if (green == maxChannel) {
			hue = 2.0F + redFactor - blueFactor;
		} else {
			hue = 4.0F + greenFactor - redFactor;
		}

		hue /= 6.0F;

		if (hue < 0.0F) {
			hue++;
		}

		return hue;
	}
}
