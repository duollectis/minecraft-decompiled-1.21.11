package net.minecraft.client.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector2i;

/**
 * Накапливает дробные значения прокрутки колёсика мыши и возвращает
 * целочисленные шаги. Сбрасывает накопленное значение при смене направления,
 * чтобы избежать «инерционного» проскакивания при быстром изменении направления.
 */
@Environment(EnvType.CLIENT)
public class Scroller {

	private double cumulHorizontal;
	private double cumulVertical;

	/**
	 * Добавляет дельту прокрутки и возвращает количество целых шагов.
	 * При смене знака направления накопленное значение сбрасывается в ноль.
	 *
	 * @param horizontal горизонтальная дельта прокрутки
	 * @param vertical вертикальная дельта прокрутки
	 * @return вектор целых шагов (x — горизонталь, y — вертикаль)
	 */
	public Vector2i update(double horizontal, double vertical) {
		if (cumulHorizontal != 0.0 && Math.signum(horizontal) != Math.signum(cumulHorizontal)) {
			cumulHorizontal = 0.0;
		}

		if (cumulVertical != 0.0 && Math.signum(vertical) != Math.signum(cumulVertical)) {
			cumulVertical = 0.0;
		}

		cumulHorizontal += horizontal;
		cumulVertical += vertical;

		int stepsX = (int) cumulHorizontal;
		int stepsY = (int) cumulVertical;

		if (stepsX == 0 && stepsY == 0) {
			return new Vector2i(0, 0);
		}

		cumulHorizontal -= stepsX;
		cumulVertical -= stepsY;
		return new Vector2i(stepsX, stepsY);
	}

	/**
	 * Циклически сдвигает индекс выбранного элемента на основе дельты прокрутки.
	 * Поддерживает wrap-around в обоих направлениях.
	 *
	 * @param amount дельта прокрутки (знак определяет направление)
	 * @param selectedIndex текущий выбранный индекс
	 * @param total общее количество элементов
	 * @return новый индекс в диапазоне [0, total)
	 */
	public static int scrollCycling(double amount, int selectedIndex, int total) {
		int direction = (int) Math.signum(amount);
		selectedIndex -= direction;
		selectedIndex = Math.max(-1, selectedIndex);

		while (selectedIndex < 0) {
			selectedIndex += total;
		}

		while (selectedIndex >= total) {
			selectedIndex -= total;
		}

		return selectedIndex;
	}
}
