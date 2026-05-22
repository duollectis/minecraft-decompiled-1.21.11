package net.minecraft.scoreboard.number;

import net.minecraft.text.MutableText;

/**
 * Стратегия форматирования числового значения очка скорборда в текст.
 * <p>
 * Реализации: {@link BlankNumberFormat} (пустой текст),
 * {@link FixedNumberFormat} (фиксированный текст),
 * {@link StyledNumberFormat} (число с заданным стилем).
 */
public interface NumberFormat {

	/**
	 * Форматирует числовое значение очка в отображаемый текст.
	 *
	 * @param number числовое значение очка
	 * @return отформатированный текст
	 */
	MutableText format(int number);

	NumberFormatType<? extends NumberFormat> getType();
}
