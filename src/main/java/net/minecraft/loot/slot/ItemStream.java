package net.minecraft.loot.slot;

import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * {@code ItemStream}.
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
	 * {@code Filter}.
	 */
	public record Filter(ItemStream slots, Predicate<ItemStack> filter) implements ItemStream {

		@Override
		public Stream<ItemStack> itemCopies() {
			return this.slots.itemCopies().filter(this.filter);
		}

		@Override
		public ItemStream filter(Predicate<ItemStack> predicate) {
			return new ItemStream.Filter(this.slots, this.filter.and(predicate));
		}
	}

	/**
	 * {@code Limit}.
	 */
	public record Limit(ItemStream slots, int limit) implements ItemStream {

		@Override
		public Stream<ItemStack> itemCopies() {
			return this.slots.itemCopies().limit(this.limit);
		}

		@Override
		public ItemStream limit(int count) {
			return new ItemStream.Limit(this.slots, Math.min(this.limit, count));
		}
	}

	/**
	 * {@code Map}.
	 */
	public record Map(ItemStream slots, Function<ItemStack, ? extends ItemStream> mapper) implements ItemStream {

		@Override
		public Stream<ItemStack> itemCopies() {
			return this.slots.itemCopies().map(this.mapper).flatMap(ItemStream::itemCopies);
		}
	}
}
