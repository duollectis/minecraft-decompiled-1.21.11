package net.minecraft.world.biome;

/**
 * Утилитарный класс для получения цвета листвы биома.
 * Цвет вычисляется по карте цветов на основе температуры и осадков.
 * Для особых деревьев (ель, берёза, мангровое) предусмотрены фиксированные константы.
 */
public class FoliageColors {

	/** Цвет листвы ели (холодный сине-зелёный). */
	public static final int SPRUCE = -10380959;
	/** Цвет листвы берёзы (светло-зелёный). */
	public static final int BIRCH = -8345771;
	/** Цвет листвы по умолчанию, используется при выходе за пределы карты. */
	public static final int DEFAULT = -12012264;
	/** Цвет листвы мангрового дерева (насыщенный зелёный). */
	public static final int MANGROVE = -7158200;

	private static int[] colorMap = new int[65536];

	public static void setColorMap(int[] pixels) {
		colorMap = pixels;
	}

	public static int getColor(double temperature, double downfall) {
		return BiomeColors.getColor(temperature, downfall, colorMap, DEFAULT);
	}
}
