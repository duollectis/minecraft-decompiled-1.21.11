package net.minecraft.item;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Фабрика для создания множеств стеков предметов с кастомной стратегией хэширования.
 * <p>Два стека считаются равными, если у них одинаковый тип предмета и компоненты
 * (количество не учитывается). Используется в {@link ItemGroup} для хранения
 * отображаемых предметов без дублирования.</p>
 */
public class ItemStackSet {

	private static final Strategy<? super ItemStack> HASH_STRATEGY = new Strategy<>() {
		@Override
		public int hashCode(@Nullable ItemStack stack) {
			return ItemStack.hashCode(stack);
		}

		@Override
		public boolean equals(@Nullable ItemStack left, @Nullable ItemStack right) {
			return left == right
					|| left != null
					&& right != null
					&& left.isEmpty() == right.isEmpty()
					&& ItemStack.areItemsAndComponentsEqual(left, right);
		}
	};

	/**
	 * Создаёт новое пустое множество стеков предметов с хэшированием по типу и компонентам.
	 *
	 * @return новое пустое множество стеков
	 */
	public static Set<ItemStack> create() {
		return new ObjectLinkedOpenCustomHashSet<>(HASH_STRATEGY);
	}
}
