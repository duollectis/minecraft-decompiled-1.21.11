package net.minecraft.scoreboard;

import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.text.MutableText;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Интерфейс для чтения данных очка скорборда без права на изменение.
 * Используется в клиентском коде и при отображении результатов,
 * где запись значений не требуется.
 */
public interface ReadableScoreboardScore {

	int getScore();

	boolean isLocked();

	@Nullable NumberFormat getNumberFormat();

	/**
	 * Форматирует числовое значение очка с использованием формата этого очка,
	 * либо резервного формата, если собственный формат не задан.
	 *
	 * @param fallbackFormat формат, применяемый при отсутствии собственного
	 * @return отформатированный текст очка
	 */
	default MutableText getFormattedScore(NumberFormat fallbackFormat) {
		return Objects.requireNonNullElse(getNumberFormat(), fallbackFormat).format(getScore());
	}

	/**
	 * Статический вариант форматирования: если очко равно {@code null},
	 * форматирует нулевое значение резервным форматом.
	 *
	 * @param score          очко для форматирования, может быть {@code null}
	 * @param fallbackFormat резервный формат
	 * @return отформатированный текст
	 */
	static MutableText getFormattedScore(@Nullable ReadableScoreboardScore score, NumberFormat fallbackFormat) {
		return score != null ? score.getFormattedScore(fallbackFormat) : fallbackFormat.format(0);
	}
}
