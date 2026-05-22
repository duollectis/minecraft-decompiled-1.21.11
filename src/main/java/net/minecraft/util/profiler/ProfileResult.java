package net.minecraft.util.profiler;

import java.nio.file.Path;
import java.util.List;

/**
 * Результат профилирования за диапазон тиков: иерархия замеров, временные метки
 * и возможность сохранения в текстовый файл.
 */
public interface ProfileResult {

	char SPLITTER_CHAR = '\u001e';

	List<ProfilerTiming> getTimings(String parentPath);

	boolean save(Path path);

	long getStartTime();

	int getStartTick();

	long getEndTime();

	int getEndTick();

	default long getTimeSpan() {
		return getEndTime() - getStartTime();
	}

	default int getTickSpan() {
		return getEndTick() - getStartTick();
	}

	String getRootTimings();

	static String getHumanReadableName(String path) {
		return path.replace('\u001e', '.');
	}
}
