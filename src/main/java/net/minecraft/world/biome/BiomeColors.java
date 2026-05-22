package net.minecraft.world.biome;

/**
 * Утилитарный интерфейс для вычисления цвета биома по карте цветов.
 * <p>
 * Карта цветов — одномерный массив размером 65536 (256×256), индексируемый
 * по температуре и количеству осадков. Используется для травы, листвы и т.д.
 */
public interface BiomeColors {

	/**
	 * Возвращает цвет из карты цветов по температуре и количеству осадков.
	 * <p>
	 * Алгоритм: количество осадков умножается на температуру (для корректного
	 * отображения в пространстве карты), затем вычисляется индекс в массиве.
	 * Если индекс выходит за пределы — возвращается резервный цвет.
	 *
	 * @param temperature температура биома в диапазоне [0.0, 1.0]
	 * @param downfall    количество осадков в диапазоне [0.0, 1.0]
	 * @param colormap    карта цветов размером 65536
	 * @param fallback    резервный цвет при выходе индекса за пределы
	 * @return цвет в формате RGB
	 */
	static int getColor(double temperature, double downfall, int[] colormap, int fallback) {
		downfall *= temperature;
		int tempIndex = (int) ((1.0 - temperature) * 255.0);
		int downfallIndex = (int) ((1.0 - downfall) * 255.0);
		int index = downfallIndex << 8 | tempIndex;
		return index >= colormap.length ? fallback : colormap[index];
	}
}
