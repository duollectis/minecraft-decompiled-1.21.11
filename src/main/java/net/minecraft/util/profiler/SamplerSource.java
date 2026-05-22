package net.minecraft.util.profiler;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Источник набора {@link Sampler}-ов для конкретного контекста записи.
 * Позволяет динамически формировать список сэмплеров на основе текущего профайлера.
 */
public interface SamplerSource {

	Set<Sampler> getSamplers(Supplier<ReadableProfiler> profilerSupplier);
}
