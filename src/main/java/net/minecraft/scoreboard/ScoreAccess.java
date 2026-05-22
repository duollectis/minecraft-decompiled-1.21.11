package net.minecraft.scoreboard;

import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

/**
 * Интерфейс полного доступа к очку скорборда — чтение и запись.
 * Предоставляет атомарные операции изменения значения, блокировки
 * и настройки отображения конкретного очка держателя по цели.
 */
public interface ScoreAccess {

	int getScore();

	void setScore(int score);

	/**
	 * Увеличивает текущее значение очка на заданную величину.
	 *
	 * @param amount величина прибавки (может быть отрицательной)
	 * @return новое значение очка после увеличения
	 */
	default int incrementScore(int amount) {
		int newScore = getScore() + amount;
		setScore(newScore);
		return newScore;
	}

	default int incrementScore() {
		return incrementScore(1);
	}

	default void resetScore() {
		setScore(0);
	}

	boolean isLocked();

	void unlock();

	void lock();

	@Nullable Text getDisplayText();

	void setDisplayText(@Nullable Text text);

	void setNumberFormat(@Nullable NumberFormat numberFormat);
}
