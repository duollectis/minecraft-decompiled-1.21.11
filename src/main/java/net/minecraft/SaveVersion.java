package net.minecraft;

/**
 * Версия сохранения мира, включающая числовой идентификатор и серию.
 * <p>
 * Используется для определения совместимости миров при загрузке.
 * Основная серия обозначается константой {@link #MAIN_SERIES}.
 */
public record SaveVersion(int id, String series) {

	public static final String MAIN_SERIES = "main";

	/**
	 * Проверяет, принадлежит ли версия не основной серии.
	 *
	 * @return {@code true}, если серия отличается от {@link #MAIN_SERIES}
	 */
	public boolean isNotMainSeries() {
		return !series.equals(MAIN_SERIES);
	}

	/**
	 * Проверяет, доступна ли данная версия для загрузки в контексте другой версии.
	 * <p>
	 * Если включён флаг {@link SharedConstants#OPEN_INCOMPATIBLE_WORLDS}, совместимость
	 * не проверяется. Иначе версии должны принадлежать одной серии.
	 *
	 * @param other версия, с которой проверяется совместимость
	 * @return {@code true}, если версии совместимы
	 */
	public boolean isAvailableTo(SaveVersion other) {
		return SharedConstants.OPEN_INCOMPATIBLE_WORLDS || series().equals(other.series());
	}
}
