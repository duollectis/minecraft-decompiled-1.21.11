package net.minecraft.inventory;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Изменяемая ссылка на стак предмета в произвольном хранилище.
 * Абстрагирует источник стака (инвентарь, экипировка, список) за единым API
 * чтения и записи, что позволяет командам и лут-механике работать
 * с любым контейнером без привязки к конкретной реализации.
 */
public interface StackReference {

	ItemStack get();

	boolean set(ItemStack stack);

	/**
	 * Создаёт ссылку на основе произвольных геттера и сеттера.
	 *
	 * @param getter поставщик текущего стака
	 * @param setter потребитель нового стака
	 * @return ссылка, всегда возвращающая {@code true} при {@link #set}
	 */
	static StackReference of(Supplier<ItemStack> getter, Consumer<ItemStack> setter) {
		return new StackReference() {
			@Override
			public ItemStack get() {
				return getter.get();
			}

			@Override
			public boolean set(ItemStack stack) {
				setter.accept(stack);
				return true;
			}
		};
	}

	/**
	 * Создаёт ссылку на слот экипировки существа с фильтрацией допустимых стаков.
	 *
	 * @param entity существо-владелец экипировки
	 * @param slot   слот экипировки
	 * @param filter предикат, разрешающий или запрещающий установку конкретного стака
	 * @return ссылка, возвращающая {@code false} при {@link #set}, если фильтр не пройден
	 */
	static StackReference of(LivingEntity entity, EquipmentSlot slot, Predicate<ItemStack> filter) {
		return new StackReference() {
			@Override
			public ItemStack get() {
				return entity.getEquippedStack(slot);
			}

			@Override
			public boolean set(ItemStack stack) {
				if (!filter.test(stack)) {
					return false;
				}

				entity.equipStack(slot, stack);
				return true;
			}
		};
	}

	static StackReference of(LivingEntity entity, EquipmentSlot slot) {
		return of(entity, slot, stack -> true);
	}

	/**
	 * Создаёт ссылку на элемент списка стаков по индексу.
	 *
	 * @param stacks список стаков
	 * @param index  индекс целевого элемента
	 * @return ссылка, всегда возвращающая {@code true} при {@link #set}
	 */
	static StackReference of(List<ItemStack> stacks, int index) {
		return new StackReference() {
			@Override
			public ItemStack get() {
				return stacks.get(index);
			}

			@Override
			public boolean set(ItemStack stack) {
				stacks.set(index, stack);
				return true;
			}
		};
	}
}
