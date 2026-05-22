package net.minecraft.util.profiler;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.TimeHelper;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Фабрика сэмплеров на основе целей профилирования из {@link ReadableProfiler#getSampleTargets()}.
 * Отслеживает уже созданные сэмплеры, чтобы не дублировать их при повторных вызовах.
 */
public class SamplerFactory {

	private final Set<String> sampledFullPaths = new ObjectOpenHashSet<>();

	/**
	 * Создаёт новые сэмплеры для путей, которые ещё не были зарегистрированы.
	 * Каждый сэмплер измеряет максимальное время секции в миллисекундах.
	 */
	public Set<Sampler> createSamplers(Supplier<ReadableProfiler> profilerSupplier) {
		Set<Sampler> newSamplers = profilerSupplier.get()
			.getSampleTargets()
			.stream()
			.filter(target -> !sampledFullPaths.contains(target.getLeft()))
			.map(target -> createSampler(profilerSupplier, target.getLeft(), target.getRight()))
			.collect(Collectors.toSet());

		for (Sampler sampler : newSamplers) {
			sampledFullPaths.add(sampler.getName());
		}

		return newSamplers;
	}

	private static Sampler createSampler(Supplier<ReadableProfiler> profilerSupplier, String id, SampleType type) {
		return Sampler.create(id, type, () -> {
			ProfilerSystem.LocatedInfo info = profilerSupplier.get().getInfo(id);
			return info == null ? 0.0 : (double) info.getMaxTime() / TimeHelper.MILLI_IN_NANOS;
		});
	}
}
