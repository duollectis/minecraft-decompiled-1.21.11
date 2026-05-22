package net.minecraft.predicate.collection;

import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Предикат, проверяющий, что коллекция содержит элементы, удовлетворяющие
 * каждому из заданных предикатов (семантика «все предикаты должны найти совпадение»).
 * Оптимизирован через три реализации: пустую, одиночную и множественную.
 */
public interface CollectionContainsPredicate<T, P extends Predicate<T>> extends Predicate<Iterable<T>> {

	List<P> getPredicates();

	static <T, P extends Predicate<T>> Codec<CollectionContainsPredicate<T, P>> createCodec(Codec<P> predicateCodec) {
		return predicateCodec
				.listOf()
				.xmap(CollectionContainsPredicate::create, CollectionContainsPredicate::getPredicates);
	}

	@SafeVarargs
	static <T, P extends Predicate<T>> CollectionContainsPredicate<T, P> create(P... predicates) {
		return create(List.of(predicates));
	}

	@SuppressWarnings("unchecked")
	static <T, P extends Predicate<T>> CollectionContainsPredicate<T, P> create(List<P> predicates) {
		return (CollectionContainsPredicate<T, P>) switch (predicates.size()) {
			case 0 -> new CollectionContainsPredicate.Empty<>();
			case 1 -> new CollectionContainsPredicate.Single<>(predicates.getFirst());
			default -> new CollectionContainsPredicate.Multiple<>(predicates);
		};
	}

	class Empty<T, P extends Predicate<T>> implements CollectionContainsPredicate<T, P> {

		@Override
		public boolean test(Iterable<T> iterable) {
			return true;
		}

		@Override
		public List<P> getPredicates() {
			return List.of();
		}
	}

	/**
	 * Проверяет, что для каждого предиката из списка найдётся хотя бы один
	 * подходящий элемент. Использует изменяемую копию списка предикатов
	 * и удаляет уже «закрытые» предикаты по мере обхода коллекции.
	 */
	record Multiple<T, P extends Predicate<T>>(List<P> tests) implements CollectionContainsPredicate<T, P> {

		@Override
		public boolean test(Iterable<T> iterable) {
			List<Predicate<T>> remaining = new ArrayList<>(tests);

			for (T element : iterable) {
				remaining.removeIf(predicate -> predicate.test(element));

				if (remaining.isEmpty()) {
					return true;
				}
			}

			return false;
		}

		@Override
		public List<P> getPredicates() {
			return tests;
		}
	}

	record Single<T, P extends Predicate<T>>(P test) implements CollectionContainsPredicate<T, P> {

		@Override
		public boolean test(Iterable<T> iterable) {
			for (T element : iterable) {
				if (test.test(element)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public List<P> getPredicates() {
			return List.of(test);
		}
	}
}
