package net.minecraft.test;

/**
 * {@code BatchListener}.
 */
public interface BatchListener {

	void onStarted(GameTestBatch batch);

	void onFinished(GameTestBatch batch);
}
