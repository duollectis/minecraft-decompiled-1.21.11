package net.minecraft.util.profiler;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

/**
 * Накопленная статистика одной именованной секции профайлера:
 * суммарное и максимальное время, количество посещений и счётчики маркеров.
 */
public interface ProfileLocationInfo {

	long getTotalTime();

	long getMaxTime();

	long getVisitCount();

	Object2LongMap<String> getCounts();
}
