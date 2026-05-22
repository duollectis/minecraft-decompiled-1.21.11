package net.minecraft.predicate.item;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.predicate.component.ComponentSubPredicate;

import java.util.List;
import java.util.function.Function;

/**
 * Базовый предикат для проверки зачарований предмета.
 * Подклассы {@link Enchantments} и {@link StoredEnchantments} различаются
 * типом компонента: активные зачарования vs. сохранённые (книги зачарований).
 */
public abstract class EnchantmentsPredicate implements ComponentSubPredicate<ItemEnchantmentsComponent> {

	private final List<EnchantmentPredicate> enchantments;

	protected EnchantmentsPredicate(List<EnchantmentPredicate> enchantments) {
		this.enchantments = enchantments;
	}

	public static <T extends EnchantmentsPredicate> Codec<T> createCodec(Function<List<EnchantmentPredicate>, T> factory) {
		return EnchantmentPredicate.CODEC.listOf().xmap(factory, EnchantmentsPredicate::getEnchantments);
	}

	public static EnchantmentsPredicate.Enchantments enchantments(List<EnchantmentPredicate> enchantments) {
		return new EnchantmentsPredicate.Enchantments(enchantments);
	}

	public static EnchantmentsPredicate.StoredEnchantments storedEnchantments(List<EnchantmentPredicate> storedEnchantments) {
		return new EnchantmentsPredicate.StoredEnchantments(storedEnchantments);
	}

	protected List<EnchantmentPredicate> getEnchantments() {
		return enchantments;
	}

	public boolean test(ItemEnchantmentsComponent component) {
		for (EnchantmentPredicate enchantmentPredicate : enchantments) {
			if (!enchantmentPredicate.test(component)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Предикат для активных зачарований предмета (компонент {@code ENCHANTMENTS}).
	 */
	public static class Enchantments extends EnchantmentsPredicate {

		public static final Codec<EnchantmentsPredicate.Enchantments> CODEC = createCodec(EnchantmentsPredicate.Enchantments::new);

		protected Enchantments(List<EnchantmentPredicate> enchantments) {
			super(enchantments);
		}

		@Override
		public ComponentType<ItemEnchantmentsComponent> getComponentType() {
			return DataComponentTypes.ENCHANTMENTS;
		}
	}

	/**
	 * Предикат для сохранённых зачарований (компонент {@code STORED_ENCHANTMENTS}, книги зачарований).
	 */
	public static class StoredEnchantments extends EnchantmentsPredicate {

		public static final Codec<EnchantmentsPredicate.StoredEnchantments> CODEC = createCodec(EnchantmentsPredicate.StoredEnchantments::new);

		protected StoredEnchantments(List<EnchantmentPredicate> enchantments) {
			super(enchantments);
		}

		@Override
		public ComponentType<ItemEnchantmentsComponent> getComponentType() {
			return DataComponentTypes.STORED_ENCHANTMENTS;
		}
	}
}
