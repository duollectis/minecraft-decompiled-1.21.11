package net.minecraft.util.profiler;

/**
 * Классифицирует фазы серверного тика для детализированного профилирования.
 * Используется {@link DebugRecorder} и сэмплерами для разбивки времени тика
 * на логические сегменты: полный тик, метод тика, отложенные задачи и простой.
 */
public enum ServerTickType {
	FULL_TICK,
	TICK_SERVER_METHOD,
	SCHEDULED_TASKS,
	IDLE;
}
