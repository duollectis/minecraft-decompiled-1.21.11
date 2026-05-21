package net.minecraft.util.profiler;

/**
 * {@code Recorder}.
 */
public interface Recorder {

	void stop();

	void forceStop();

	void startTick();

	boolean isActive();

	Profiler getProfiler();

	void endTick();
}
