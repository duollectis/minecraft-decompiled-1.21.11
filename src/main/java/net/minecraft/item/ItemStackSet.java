package net.minecraft.item;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * {@code ItemStackSet}.
 */
public class ItemStackSet {

	private static final Strategy<? super ItemStack> HASH_STRATEGY = new Strategy<ItemStack>() {
		/**
		 * Проверяет наличие h code.
		 *
		 * @param itemStack item stack
		 *
		 * @return int — {@code true} если условие выполнено
		 */
		public int hashCode(@Nullable ItemStack itemStack) {
			return ItemStack.hashCode(itemStack);
		}

		/**
		 * Equals.
		 *
		 * @param itemStack item stack
		 * @param itemStack2 item stack2
		 *
		 * @return boolean — результат операции
		 */
		public boolean equals(@Nullable ItemStack itemStack, @Nullable ItemStack itemStack2) {
			return itemStack == itemStack2
					|| itemStack != null
					&& itemStack2 != null
					&& itemStack.isEmpty() == itemStack2.isEmpty()
					&& ItemStack.areItemsAndComponentsEqual(itemStack, itemStack2);
		}
	};

	/**
	 * Create.
	 *
	 * @return Set — результат операции
	 */
	public static Set<ItemStack> create() {
		return new ObjectLinkedOpenCustomHashSet(HASH_STRATEGY);
	}
}
