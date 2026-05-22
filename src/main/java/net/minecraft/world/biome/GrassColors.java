package net.minecraft.world.biome;

/**
 * Утилитарный класс для получения цвета травы биома.
 * Цвет вычисляется по карте цветов на основе температуры и осадков.
 */
public class GrassColors {

	/** Цвет травы по умолчанию (ярко-пурпурный), используется при выходе за пределы карты. */
	private static final int FALLBACK_COLOR = -65281;

	/** Температура для вычисления цвета травы по умолчанию. */
	private static final double DEFAULT_TEMPERATURE = 0.5;
	/** Количество осадков для вычисления цвета травы по умолчанию. */
	private static final double DEFAULT_DOWNFALL = 1.0;

	private static int[] colorMap = new int[65536];

	public static void setColorMap(int[] map) {
		colorMap = map;
	}

	public static int getColor(double temperature, double downfall) {
		return BiomeColors.getColor(temperature, downfall, colorMap, FALLBACK_COLOR);
	}

	public static int getDefaultColor() {
		return getColor(DEFAULT_TEMPERATURE, DEFAULT_DOWNFALL);
	}
}
