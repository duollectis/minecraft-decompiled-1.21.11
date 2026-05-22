package net.minecraft.test;

/**
 * Конфигурация попыток запуска теста.
 * Определяет максимальное число прогонов и поведение при провале.
 *
 * @param numberOfTries   максимальное число попыток (значение {@code < 1} означает «без ограничений»)
 * @param haltOnFailure   если {@code true}, серия прогонов прерывается при первом провале
 */
public record TestAttemptConfig(int numberOfTries, boolean haltOnFailure) {

	private static final TestAttemptConfig ONCE = new TestAttemptConfig(1, true);

	/** @return конфигурация для одиночного обязательного прогона */
	public static TestAttemptConfig once() {
		return ONCE;
	}

	/** @return {@code true} если лимит попыток не задан (numberOfTries < 1) */
	public boolean isDisabled() {
		return numberOfTries < 1;
	}

	/**
	 * Определяет, нужно ли запустить тест ещё раз.
	 *
	 * @param attempt   текущее число попыток
	 * @param successes текущее число успехов
	 * @return {@code true} если следует повторить прогон
	 */
	public boolean shouldTestAgain(int attempt, int successes) {
		boolean hadFailure = attempt != successes;
		boolean attemptsLeft = isDisabled() || attempt < numberOfTries;
		return attemptsLeft && (!hadFailure || !haltOnFailure);
	}

	/** @return {@code true} если конфигурация предполагает более одного прогона */
	public boolean needsMultipleAttempts() {
		return numberOfTries != 1;
	}
}
