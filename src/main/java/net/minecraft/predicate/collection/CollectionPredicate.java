package net.minecraft.predicate.collection;

import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.predicate.NumberRange;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Составной предикат для проверки коллекции элементов.
 * Объединяет три независимых условия: наличие элементов ({@code contains}),
 * подсчёт совпадений ({@code counts}) и размер коллекции ({@code size}).
 *
 * @param <T> тип элементов коллекции
 * @param <P> тип предиката для элементов
 */
public record CollectionPredicate<T, P extends Predicate<T>>(
		Optional<CollectionContainsPredicate<T, P>> contains,
		Optional<CollectionCountsPredicate<T, P>> counts,
		Optional<NumberRange.IntRange> size
) implements Predicate<Iterable<T>> {

	public static <T, P extends Predicate<T>> Codec<CollectionPredicate<T, P>> createCodec(Codec<P> predicateCodec) {
		return RecordCodecBuilder.create(
				instance -> instance.group(
						CollectionContainsPredicate.createCodec(predicateCodec)
								.optionalFieldOf("contains")
								.forGetter(CollectionPredicate::contains),
						CollectionCountsPredicate.createCodec(predicateCodec)
								.optionalFieldOf("count")
								.forGetter(CollectionPredicate::counts),
						NumberRange.IntRange.CODEC.optionalFieldOf("size").forGetter(CollectionPredicate::size)
				)
				.apply(instance, CollectionPredicate::new)
		);
	}

	public boolean test(Iterable<T> iterable) {
		if (contains.isPresent() && !contains.get().test(iterable)) {
			return false;
		}

		if (counts.isPresent() && !counts.get().test(iterable)) {
			return false;
		}

		return size.isEmpty() || size.get().test(Iterables.size(iterable));
	}
}
