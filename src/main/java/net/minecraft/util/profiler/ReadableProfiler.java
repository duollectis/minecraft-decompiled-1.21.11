package net.minecraft.util.profiler;

import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * {@code ReadableProfiler}.
 */
public interface ReadableProfiler extends Profiler {

	ProfileResult getResult();

	ProfilerSystem.@Nullable LocatedInfo getInfo(String name);

	Set<Pair<String, SampleType>> getSampleTargets();
}
