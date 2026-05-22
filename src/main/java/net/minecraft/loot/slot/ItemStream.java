package net.minecraft.loot.slot;

import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Ленивый поток копий предметов из слотов инвентаря.
 * Операции {@link #filter}, {@link #map} и {@link #limit} создают новые обёртки
 * без немедленного вычисления — вычисление происходит только при вызове {@link #itemCopies()}.
 */
public interface ItemStream {

	ItemStream EMPTY = Stream::empty;

	Stream<ItemStack> itemCopies();

	default ItemStream filter(Predicate<ItemStack> predicate) {
		return new ItemStream.Filter(this, predicate);
	}

	default ItemStream map(Function<ItemStack, ? extends ItemStream> function) {
		return new ItemStream.Map(this, function);
	}

	default ItemStream limit(int count) {
		return new ItemStream.Limit(this, count);
	}

	static ItemStream of(StackReference stackReference) {
		return () -> Stream.of(stackReference.get().copy());
	}

	static ItemStream of(Collection<? extends StackReference> stackReferences) {
		return switch (stackReferences.size()) {
			case 0 -> EMPTY;
			case 1 -> of(stackReferences.iterator().next());
			default -> () -> stackReferences.stream().map(StackReference::get).map(ItemStack::copy);
		};
	}

	static ItemStream concat(ItemStream first, ItemStream second) {
		return () -> Stream.concat(first.itemCopies(), second.itemCopies());
	}

	static ItemStream concat(List<? extends ItemStream> streams) {
		return switch (streams.size()) {
			case 0 -> EMPTY;
			case 1 -> (ItemStream) streams.getFirst();
			case 2 -> concat(streams.get(0), streams.get(1));
			default -> () -> streams.stream().flatMap(ItemStream::itemCopies);
		};
	}

	/**
	 * Обёртка, фильтрующая предметы из дочернего потока по предикату.
	 * Несколько последовательных фильтров объединяются в один через {@link Predicate#and}.
	 */
	record Filter(ItemStream slots, Predicate<ItemStack> filter) implements ItemStream {

		@Override
		public Stream<ItemStack> itemCopies() {
			return slots.itemCopies().filter(filter);
		}

		@Override
		public ItemStream filter(Predicate<ItemStack> predicate) {
			return new ItemStream.Filter(slots, filter.and(predicate));
		}
	}

	/**
	 * Обёртка, ограничивающая количество предметов из дочернего потока.
	 * Повторный вызов {@link #limit} берёт минимум из двух ограничений.
	 */
	record Limit(ItemStream slots, int limit) implements ItemStream {

		@Override
		public Stream<ItemStack> itemCopies() {
			return slots.itemCopies().limit(limit);
		}

		@Override
		public ItemStream limit(int count) {
			return new ItemStream.Limit(slots, Math.min(limit, count));
		}
	}

	/**
	 * Обёртка, применяющая функцию-маппер к каждому предмету дочернего потока
	 * и объединяющая результирующие потоки через {@code flatMap}.
	 */
	record Map(ItemStream slots, Function<ItemStack, ? extends ItemStream> mapper) implements ItemStream {

		@Override
		public Stream<ItemStack> itemCopies() {
			return slots.itemCopies().map(mapper).flatMap(ItemStream::itemCopies);
		}
	}
}
