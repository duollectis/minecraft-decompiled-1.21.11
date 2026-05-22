package net.minecraft.util.thread;

import net.minecraft.util.profiler.Sampler;

import java.util.List;

/**
 * Исполнитель, способный предоставлять {@link Sampler}-ы для мониторинга своей очереди задач.
 * Реализации регистрируются в {@link ExecutorSampling} для централизованного сбора метрик.
 */
public interface SampleableExecutor {

	List<Sampler> createSamplers();
}
