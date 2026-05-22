package net.minecraft.test;

/**
 * Глобальный диспетчер событий завершения тестов.
 * Делегирует вызовы текущему {@link TestCompletionListener}, который можно подменить
 * через {@link #setCompletionListener(TestCompletionListener)} для кастомной обработки результатов.
 */
public class TestFailureLogger {

	private static TestCompletionListener completionListener = new FailureLoggingTestCompletionListener();

	public static void setCompletionListener(TestCompletionListener listener) {
		completionListener = listener;
	}

	public static void failTest(GameTestState test) {
		completionListener.onTestFailed(test);
	}

	public static void passTest(GameTestState test) {
		completionListener.onTestPassed(test);
	}

	public static void stop() {
		completionListener.onStopped();
	}
}
