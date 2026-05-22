package net.minecraft.util.thread;

import net.minecraft.util.profiler.Sampler;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * Синглтон-реестр всех активных {@link SampleableExecutor}-ов.
 * Хранит слабые ссылки, чтобы не препятствовать сборке мусора завершённых исполнителей.
 * Предоставляет агрегированный список {@link Sampler}-ов для мониторинга очередей.
 */
public class ExecutorSampling {

	public static final ExecutorSampling INSTANCE = new ExecutorSampling();

	private final WeakHashMap<SampleableExecutor, Void> activeExecutors = new WeakHashMap<>();

	private ExecutorSampling() {
	}

	public void add(SampleableExecutor executor) {
		activeExecutors.put(executor, null);
	}

	/**
	 * Собирает сэмплеры всех зарегистрированных исполнителей и объединяет
	 * одноимённые сэмплеры в {@link MergedSampler}, усредняющий их значения.
	 */
	public List<Sampler> createSamplers() {
		Map<String, List<Sampler>> grouped = activeExecutors
			.keySet()
			.stream()
			.flatMap(executor -> executor.createSamplers().stream())
			.collect(Collectors.groupingBy(Sampler::getName));

		return mergeSimilarSamplers(grouped);
	}

	private static List<Sampler> mergeSimilarSamplers(Map<String, List<Sampler>> samplers) {
		return samplers.entrySet()
			.stream()
			.map(entry -> {
				String name = entry.getKey();
				List<Sampler> list = entry.getValue();
				return list.size() > 1 ? new MergedSampler(name, list) : list.get(0);
			})
			.collect(Collectors.toList());
	}

	/**
	 * Агрегирующий сэмплер, усредняющий значения нескольких одноимённых сэмплеров.
	 * Используется, когда несколько исполнителей имеют одинаковое имя очереди.
	 */
	static class MergedSampler extends Sampler {

		private final List<Sampler> delegates;

		MergedSampler(String id, List<Sampler> delegates) {
			super(
				id,
				delegates.get(0).getType(),
				() -> averageRetrievers(delegates),
				() -> start(delegates),
				combineDeviationCheckers(delegates)
			);
			this.delegates = delegates;
		}

		private static Sampler.DeviationChecker combineDeviationCheckers(List<Sampler> delegates) {
			return value -> delegates
				.stream()
				.anyMatch(sampler -> sampler.deviationChecker != null && sampler.deviationChecker.check(value));
		}

		private static void start(List<Sampler> samplers) {
			for (Sampler sampler : samplers) {
				sampler.start();
			}
		}

		private static double averageRetrievers(List<Sampler> samplers) {
			double sum = 0.0;

			for (Sampler sampler : samplers) {
				sum += sampler.getRetriever().getAsDouble();
			}

			return sum / samplers.size();
		}

		@Override
		public boolean equals(@Nullable Object object) {
			if (this == object) {
				return true;
			}

			if (object == null || getClass() != object.getClass()) {
				return false;
			}

			if (!super.equals(object)) {
				return false;
			}

			MergedSampler mergedSampler = (MergedSampler) object;
			return delegates.equals(mergedSampler.delegates);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), delegates);
		}
	}
}
