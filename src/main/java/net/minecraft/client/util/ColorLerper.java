package net.minecraft.client.util;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Утилита для плавной интерполяции цветов между элементами палитры.
 * Используется для анимации цвета овец и музыкальных нот.
 */
@Environment(EnvType.CLIENT)
public class ColorLerper {

	public static final DyeColor[] RAINBOW_COLORS = {
		DyeColor.WHITE,
		DyeColor.LIGHT_GRAY,
		DyeColor.LIGHT_BLUE,
		DyeColor.BLUE,
		DyeColor.CYAN,
		DyeColor.GREEN,
		DyeColor.LIME,
		DyeColor.YELLOW,
		DyeColor.ORANGE,
		DyeColor.PINK,
		DyeColor.RED,
		DyeColor.MAGENTA
	};

	/**
	 * Вычисляет интерполированный ARGB-цвет для заданного шага анимации.
	 * Цвета циклически перебираются из палитры типа с линейной интерполяцией между соседними.
	 *
	 * @param type тип цветовой палитры с длительностью каждого цвета
	 * @param step текущий шаг анимации (может быть дробным для плавности)
	 * @return интерполированный ARGB-цвет
	 */
	public static int lerpColor(Type type, float step) {
		int flooredStep = MathHelper.floor(step);
		int colorIndex = flooredStep / type.colorDuration;
		int paletteSize = type.colors.length;
		int fromIndex = colorIndex % paletteSize;
		int toIndex = (colorIndex + 1) % paletteSize;
		float fraction = (flooredStep % type.colorDuration + MathHelper.fractionalPart(step)) / type.colorDuration;
		int fromArgb = type.getArgb(type.colors[fromIndex]);
		int toArgb = type.getArgb(type.colors[toIndex]);
		return ColorHelper.lerp(fraction, fromArgb, toArgb);
	}

	static int getArgb(DyeColor color, float multiplier) {
		// Белый цвет имеет специальное значение, не совпадающее с entity color
		if (color == DyeColor.WHITE) {
			return -1644826;
		}

		int entityColor = color.getEntityColor();
		return ColorHelper.getArgb(
			255,
			MathHelper.floor(ColorHelper.getRed(entityColor) * multiplier),
			MathHelper.floor(ColorHelper.getGreen(entityColor) * multiplier),
			MathHelper.floor(ColorHelper.getBlue(entityColor) * multiplier)
		);
	}

	@Environment(EnvType.CLIENT)
	public enum Type {
		SHEEP(25, DyeColor.values(), 0.75F),
		MUSIC_NOTE(30, ColorLerper.RAINBOW_COLORS, 1.25F);

		final int colorDuration;
		final DyeColor[] colors;
		private final Map<DyeColor, Integer> colorToArgb;

		Type(int colorDuration, DyeColor[] colors, float multiplier) {
			this.colorDuration = colorDuration;
			this.colors = colors;
			colorToArgb = Maps.newHashMap(
				Arrays.stream(colors)
					.collect(Collectors.toMap(
						color -> color,
						color -> ColorLerper.getArgb(color, multiplier)
					))
			);
		}

		public int getArgb(DyeColor color) {
			return colorToArgb.get(color);
		}
	}
}
