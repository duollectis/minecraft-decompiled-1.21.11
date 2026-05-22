package net.minecraft.enchantment;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Контекст применения эффекта зачарования: содержит предмет, слот экипировки,
 * владельца и колбэк для обработки поломки предмета.
 */
public record EnchantmentEffectContext(
		ItemStack stack,
		@Nullable EquipmentSlot slot,
		@Nullable LivingEntity owner,
		Consumer<Item> breakCallback
) {

	/**
	 * Создаёт контекст для предмета, надетого в конкретный слот живой сущностью.
	 * Колбэк поломки автоматически отправляет статус сломанного предмета.
	 */
	public EnchantmentEffectContext(ItemStack stack, EquipmentSlot slot, LivingEntity owner) {
		this(stack, slot, owner, item -> owner.sendEquipmentBreakStatus(item, slot));
	}
}
