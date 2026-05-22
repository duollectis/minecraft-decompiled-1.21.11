package net.minecraft.test;

/**
 * Слушатель глобального завершения тестов.
 * Используется для агрегированной отчётности (например, XML-отчёт).
 */
public interface TestCompletionListener {

	void onTestFailed(GameTestState test);

	void onTestPassed(GameTestState test);

	default void onStopped() {
	}
}
