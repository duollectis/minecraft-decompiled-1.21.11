package net.minecraft.enchantment;

import net.minecraft.registry.entry.RegistryEntry;

/**
 * Запись об уровне зачарования: связывает конкретное зачарование с его уровнем.
 * Используется при генерации зачарований для предметов и в системе взвешенного выбора.
 */
public record EnchantmentLevelEntry(RegistryEntry<Enchantment> enchantment, int level) {

	/**
	 * Возвращает вес зачарования для взвешенного случайного выбора.
	 * Чем выше вес — тем чаще зачарование встречается при генерации.
	 */
	public int getWeight() {
		return enchantment.value().getWeight();
	}
}
