package net.minecraft.util.profiler;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

/**
 * {@code ProfileLocationInfo}.
 */
public interface ProfileLocationInfo {

	long getTotalTime();

	long getMaxTime();

	long getVisitCount();

	Object2LongMap<String> getCounts();
}
