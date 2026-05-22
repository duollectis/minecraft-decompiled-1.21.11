package net.minecraft.predicate.collection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.predicate.NumberRange;

import java.util.List;
import java.util.function.Predicate;

/**
 * Предикат, проверяющий количество элементов коллекции, удовлетворяющих
 * каждому из заданных предикатов-записей {@link Entry}.
 * Оптимизирован через три реализации: пустую, одиночную и множественную.
 */
public interface CollectionCountsPredicate<T, P extends Predicate<T>> extends Predicate<Iterable<T>> {

	List<CollectionCountsPredicate.Entry<T, P>> getEntries();

	static <T, P extends Predicate<T>> Codec<CollectionCountsPredicate<T, P>> createCodec(Codec<P> predicateCodec) {
		return CollectionCountsPredicate.Entry
				.createCodec(predicateCodec)
				.listOf()
				.xmap(CollectionCountsPredicate::create, CollectionCountsPredicate::getEntries);
	}

	@SafeVarargs
	static <T, P extends Predicate<T>> CollectionCountsPredicate<T, P> create(
			CollectionCountsPredicate.Entry<T, P>... entries
	) {
		return create(List.of(entries));
	}

	@SuppressWarnings("unchecked")
	static <T, P extends Predicate<T>> CollectionCountsPredicate<T, P> create(
			List<CollectionCountsPredicate.Entry<T, P>> entries
	) {
		return (CollectionCountsPredicate<T, P>) switch (entries.size()) {
			case 0 -> new CollectionCountsPredicate.Empty<>();
			case 1 -> new CollectionCountsPredicate.Single<>(entries.getFirst());
			default -> new CollectionCountsPredicate.Multiple<>(entries);
		};
	}

	class Empty<T, P extends Predicate<T>> implements CollectionCountsPredicate<T, P> {

		@Override
		public boolean test(Iterable<T> iterable) {
			return true;
		}

		@Override
		public List<CollectionCountsPredicate.Entry<T, P>> getEntries() {
			return List.of();
		}
	}

	/**
	 * Пара «предикат + допустимый диапазон количества совпадений».
	 * Метод {@link #test(Iterable)} подсчитывает совпадения и проверяет диапазон.
	 */
	record Entry<T, P extends Predicate<T>>(P test, NumberRange.IntRange count) {

		public static <T, P extends Predicate<T>> Codec<CollectionCountsPredicate.Entry<T, P>> createCodec(
				Codec<P> predicateCodec
		) {
			return RecordCodecBuilder.create(
					instance -> instance.group(
							predicateCodec.fieldOf("test").forGetter(CollectionCountsPredicate.Entry::test),
							NumberRange.IntRange.CODEC.fieldOf("count").forGetter(CollectionCountsPredicate.Entry::count)
					).apply(instance, CollectionCountsPredicate.Entry::new)
			);
		}

		public boolean test(Iterable<T> collection) {
			int matchCount = 0;

			for (T element : collection) {
				if (test.test(element)) {
					matchCount++;
				}
			}

			return count.test(matchCount);
		}
	}

	record Multiple<T, P extends Predicate<T>>(
			List<CollectionCountsPredicate.Entry<T, P>> entries
	) implements CollectionCountsPredicate<T, P> {

		@Override
		public boolean test(Iterable<T> iterable) {
			for (CollectionCountsPredicate.Entry<T, P> entry : entries) {
				if (!entry.test(iterable)) {
					return false;
				}
			}

			return true;
		}

		@Override
		public List<CollectionCountsPredicate.Entry<T, P>> getEntries() {
			return entries;
		}
	}

	record Single<T, P extends Predicate<T>>(
			CollectionCountsPredicate.Entry<T, P> entry
	) implements CollectionCountsPredicate<T, P> {

		@Override
		public boolean test(Iterable<T> iterable) {
			return entry.test(iterable);
		}

		@Override
		public List<CollectionCountsPredicate.Entry<T, P>> getEntries() {
			return List.of(entry);
		}
	}
}
