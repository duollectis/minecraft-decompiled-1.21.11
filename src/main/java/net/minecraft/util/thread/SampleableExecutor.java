package net.minecraft.util.thread;

import net.minecraft.util.profiler.Sampler;

import java.util.List;

/**
 * {@code SampleableExecutor}.
 */
public interface SampleableExecutor {

	List<Sampler> createSamplers();
}
