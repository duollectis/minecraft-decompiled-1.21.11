package net.minecraft.world.biome;

/**
 * Утилитарный класс для получения цвета сухой листвы биома.
 * Цвет вычисляется по карте цветов на основе температуры и осадков.
 */
public class DryFoliageColors {

	/** Цвет сухой листвы по умолчанию (тёмно-коричневый), используется при выходе за пределы карты. */
	public static final int DEFAULT = -10732494;

	private static int[] colorMap = new int[65536];

	public static void setColorMap(int[] pixels) {
		colorMap = pixels;
	}

	public static int getColor(double temperature, double downfall) {
		return BiomeColors.getColor(temperature, downfall, colorMap, DEFAULT);
	}
}
