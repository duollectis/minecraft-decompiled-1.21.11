package net.minecraft.util.profiling.jfr.sample;

import java.time.Duration;

/**
 * {@code LongRunningSample}.
 */
public interface LongRunningSample {

	Duration duration();
}
