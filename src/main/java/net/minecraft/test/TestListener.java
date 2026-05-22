package net.minecraft.test;

/**
 * Слушатель событий жизненного цикла отдельного теста.
 * Используется для отслеживания старта, прохождения, провала и повтора теста.
 */
public interface TestListener {

	void onStarted(GameTestState test);

	void onPassed(GameTestState test, TestRunContext context);

	void onFailed(GameTestState test, TestRunContext context);

	void onRetry(GameTestState lastState, GameTestState nextState, TestRunContext context);
}
