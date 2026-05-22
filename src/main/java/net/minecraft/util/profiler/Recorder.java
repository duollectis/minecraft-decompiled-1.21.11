package net.minecraft.util.profiler;

/**
 * Управляет жизненным циклом записи профилировочных данных за несколько тиков.
 * Реализации координируют работу {@link Sampler}-ов и {@link Profiler}-а,
 * накапливая статистику до момента остановки и сброса дампа.
 */
public interface Recorder {

	void stop();

	void forceStop();

	void startTick();

	boolean isActive();

	Profiler getProfiler();

	void endTick();
}
