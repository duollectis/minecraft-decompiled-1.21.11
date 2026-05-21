package net.minecraft.test;

/**
 * {@code TestCompletionListener}.
 */
public interface TestCompletionListener {

	void onTestFailed(GameTestState test);

	void onTestPassed(GameTestState test);

	default void onStopped() {
	}
}
