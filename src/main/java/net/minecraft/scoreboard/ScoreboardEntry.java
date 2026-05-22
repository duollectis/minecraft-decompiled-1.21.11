package net.minecraft.scoreboard;

import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Снимок одной записи скорборда для конкретной цели.
 * Используется при отображении таблицы результатов и при сетевой синхронизации.
 * <p>
 * Владельцы, чьё имя начинается с {@code #}, считаются скрытыми
 * и не отображаются в пользовательском интерфейсе.
 */
public record ScoreboardEntry(
		String owner,
		int value,
		@Nullable Text display,
		@Nullable NumberFormat numberFormatOverride
) {

	/**
	 * Возвращает {@code true}, если запись скрыта от отображения.
	 * Скрытые записи используются для хранения данных без показа игрокам.
	 */
	public boolean hidden() {
		return owner.startsWith(Scoreboard.TEAM_SCORE_PREFIX);
	}

	/**
	 * Возвращает отображаемое имя: кастомный текст или литерал из имени владельца.
	 */
	public Text name() {
		return display != null ? display : Text.literal(owner());
	}

	/**
	 * Форматирует числовое значение очка, используя переопределённый формат
	 * или переданный резервный формат.
	 */
	public MutableText formatted(NumberFormat format) {
		return Objects.requireNonNullElse(numberFormatOverride, format).format(value);
	}
}
