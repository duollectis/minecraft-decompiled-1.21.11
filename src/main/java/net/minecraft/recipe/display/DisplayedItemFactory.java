package net.minecraft.recipe.display;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.List;

/**
 * Фабрика для преобразования {@link ItemStack} в отображаемый тип {@code T}.
 * Используется в {@link SlotDisplay#appendStacks} для абстрагирования
 * от конкретного типа отображаемого элемента (стек, иконка, виджет и т.д.).
 */
public interface DisplayedItemFactory<T> {

	/**
	 * Фабрика, которая дополнительно принимает список остатков (remainder)
	 * для отображения предметов, возвращаемых после крафта.
	 */
	interface FromRemainder<T> extends DisplayedItemFactory<T> {

		T toDisplayed(T input, List<T> remainders);
	}

	/**
	 * Фабрика, создающая отображаемый элемент из {@link ItemStack}.
	 * Предоставляет удобные перегрузки для {@link RegistryEntry} и {@link Item}.
	 */
	interface FromStack<T> extends DisplayedItemFactory<T> {

		default T toDisplayed(RegistryEntry<Item> item) {
			return toDisplayed(new ItemStack(item));
		}

		default T toDisplayed(Item item) {
			return toDisplayed(new ItemStack(item));
		}

		T toDisplayed(ItemStack stack);
	}
}
