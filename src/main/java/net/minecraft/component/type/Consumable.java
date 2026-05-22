package net.minecraft.component.type;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
	 * Маркерный интерфейс для компонентов, которые могут быть применены
	 * при поедании/питье предмета (например, эффекты зелий, телепортация).
	 */
public interface Consumable {

	/**
		 * Вызывается при завершении потребления предмета игроком.
		 *
		 * @param world мир, в котором происходит потребление
		 * @param user существо, потребляющее предмет
		 * @param stack стек потребляемого предмета
		 * @param consumable компонент потребления, инициировавший вызов
		 */
	void onConsume(World world, LivingEntity user, ItemStack stack, ConsumableComponent consumable);
}
