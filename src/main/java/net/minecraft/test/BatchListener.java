package net.minecraft.test;

/**
 * Слушатель жизненного цикла батча тестов.
 * Уведомляет о начале и завершении выполнения {@link GameTestBatch}.
 */
public interface BatchListener {

	void onStarted(GameTestBatch batch);

	void onFinished(GameTestBatch batch);
}
