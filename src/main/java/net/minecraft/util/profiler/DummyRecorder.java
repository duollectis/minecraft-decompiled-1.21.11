package net.minecraft.util.profiler;

/**
 * Заглушка {@link Recorder}, которая не выполняет никаких действий.
 * Используется как нейтральный объект по умолчанию, когда запись профиля не активна.
 */
public class DummyRecorder implements Recorder {

	public static final Recorder INSTANCE = new DummyRecorder();

	@Override
	public void stop() {
	}

	@Override
	public void forceStop() {
	}

	@Override
	public void startTick() {
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public Profiler getProfiler() {
		return DummyProfiler.INSTANCE;
	}

	@Override
	public void endTick() {
	}
}
