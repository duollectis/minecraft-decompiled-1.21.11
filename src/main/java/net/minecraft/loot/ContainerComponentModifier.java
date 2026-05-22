package net.minecraft.loot;

import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.slot.ItemStream;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Интерфейс для модификации компонента-контейнера предмета через поток содержимого.
 *
 * <p>Позволяет читать и записывать содержимое компонента (например, {@code CONTAINER},
 * {@code BUNDLE_CONTENTS}) как поток {@link ItemStack}, применяя к нему произвольные
 * преобразования лут-функций.</p>
 *
 * @param <T> тип компонента-контейнера
 */
public interface ContainerComponentModifier<T> {

	ComponentType<T> getComponentType();

	T getDefault();

	T apply(T component, Stream<ItemStack> contents);

	Stream<ItemStack> stream(T component);

	/**
	 * Применяет модификацию к компоненту стака, читая текущее значение или используя
	 * переданный {@code component} как значение по умолчанию, если компонент отсутствует.
	 */
	default void apply(ItemStack stack, T component, Stream<ItemStack> contents) {
		T current = stack.getOrDefault(getComponentType(), component);
		T updated = apply(current, contents);
		stack.set(getComponentType(), updated);
	}

	default void apply(ItemStack stack, Stream<ItemStack> contents) {
		apply(stack, getDefault(), contents);
	}

	/**
	 * Применяет оператор преобразования к каждому предмету внутри компонента-контейнера.
	 * Пустые стаки пропускаются; результат обрезается до максимального размера стака.
	 */
	default void apply(ItemStack stack, UnaryOperator<ItemStack> contentsOperator) {
		T component = stack.get(getComponentType());

		if (component == null) {
			return;
		}

		UnaryOperator<ItemStack> safeOperator = contentStack -> {
			if (contentStack.isEmpty()) {
				return contentStack;
			}

			ItemStack result = contentsOperator.apply(contentStack);
			result.capCount(result.getMaxCount());

			return result;
		};

		apply(stack, stream(component).map(safeOperator));
	}

	default ItemStream stream(ItemStack stack) {
		return () -> {
			T component = stack.get(getComponentType());
			return component != null ? stream(component).filter(s -> !s.isEmpty()) : Stream.empty();
		};
	}
}
