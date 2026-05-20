package net.minecraft.util.profiler;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;

public interface ReadableProfiler extends Profiler {
   ProfileResult getResult();

   ProfilerSystem.@Nullable LocatedInfo getInfo(String name);

   Set<Pair<String, SampleType>> getSampleTargets();
}
