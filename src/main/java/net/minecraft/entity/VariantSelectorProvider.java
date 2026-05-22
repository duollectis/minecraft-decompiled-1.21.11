package net.minecraft.entity;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Провайдер для выбора варианта сущности на основе набора условий и приоритетов.
 * Каждый вариант содержит список {@link Selector}, которые фильтруются по контексту спауна.
 */
public interface VariantSelectorProvider<Context, Condition extends VariantSelectorProvider.SelectorCondition<Context>> {

	List<VariantSelectorProvider.Selector<Context, Condition>> getSelectors();

	/**
	 * Выбирает все подходящие варианты из потока записей реестра для заданного контекста.
	 * Алгоритм: разворачивает все селекторы, сортирует по убыванию приоритета,
	 * затем оставляет только те, чьё условие выполнено при наивысшем приоритете.
	 */
	@SuppressWarnings("unchecked")
	static <C, T> Stream<T> select(
			Stream<T> entries,
			Function<T, VariantSelectorProvider<C, ?>> providerGetter,
			C context
	) {
		List<UnwrappedSelector<C, T>> candidates = new ArrayList<>();

		entries.forEach(entry -> {
			VariantSelectorProvider<C, ?> provider = providerGetter.apply(entry);

			for (Selector<C, ?> selector : provider.getSelectors()) {
				SelectorCondition<C> condition = (SelectorCondition<C>) DataFixUtils.orElseGet(
						selector.condition(),
						SelectorCondition::alwaysTrue
				);
				candidates.add(new UnwrappedSelector<>(entry, selector.priority(), condition));
			}
		});

		candidates.sort(UnwrappedSelector.PRIORITY_COMPARATOR);

		// Двухпроходный алгоритм: сначала находим наивысший приоритет среди подходящих,
		// затем оставляем только кандидатов с этим приоритетом, чьё условие выполнено.
		int highestMatchedPriority = Integer.MIN_VALUE;

		for (UnwrappedSelector<C, T> candidate : candidates) {
			if (candidate.priority >= highestMatchedPriority && candidate.condition.test(context)) {
				highestMatchedPriority = candidate.priority;
			}
		}

		final int finalPriority = highestMatchedPriority;
		candidates.removeIf(candidate -> candidate.priority < finalPriority || !candidate.condition.test(context));

		return candidates.stream().map(UnwrappedSelector::entry);
	}

	static <C, T> Optional<T> select(
			Stream<T> entries,
			Function<T, VariantSelectorProvider<C, ?>> providerGetter,
			Random random,
			C context
	) {
		List<T> matched = select(entries, providerGetter, context).toList();
		return Util.getRandomOrEmpty(matched, random);
	}

	static <Context, Condition extends SelectorCondition<Context>> List<Selector<Context, Condition>> createSingle(
			Condition condition, int priority
	) {
		return List.of(new Selector<>(condition, priority));
	}

	static <Context, Condition extends SelectorCondition<Context>> List<Selector<Context, Condition>> createFallback(
			int priority
	) {
		return List.of(new Selector<>(Optional.empty(), priority));
	}

	/**
	 * Пара «условие + приоритет», определяющая, когда данный вариант может быть выбран.
	 */
	record Selector<Context, Condition extends VariantSelectorProvider.SelectorCondition<Context>>(
			Optional<Condition> condition,
			int priority
	) {

		public Selector(Condition condition, int priority) {
			this(Optional.of(condition), priority);
		}

		public Selector(int priority) {
			this(Optional.empty(), priority);
		}

		public static <Context, Condition extends SelectorCondition<Context>> Codec<Selector<Context, Condition>> createCodec(
				Codec<Condition> conditionCodec
		) {
			return RecordCodecBuilder.create(
					instance -> instance.group(
							conditionCodec.optionalFieldOf("condition").forGetter(Selector::condition),
							Codec.INT.fieldOf("priority").forGetter(Selector::priority)
					).apply(instance, Selector::new)
			);
		}
	}

	/**
		* Условие выбора варианта, проверяемое против контекста спауна.
		*/
	@FunctionalInterface
	interface SelectorCondition<C> extends Predicate<C> {

		static <C> SelectorCondition<C> alwaysTrue() {
			return context -> true;
		}
	}

	/**
		* Развёрнутый селектор: хранит ссылку на вариант, его приоритет и условие без {@link Optional}.
		*/
	record UnwrappedSelector<C, T>(
			T entry,
			int priority,
			SelectorCondition<C> condition
	) {

		public static final Comparator<UnwrappedSelector<?, ?>> PRIORITY_COMPARATOR = Comparator
				.<UnwrappedSelector<?, ?>, Integer>comparing(s -> s.priority())
				.reversed();
	}
}
