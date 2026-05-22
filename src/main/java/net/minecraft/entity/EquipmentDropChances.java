package net.minecraft.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;

import java.util.HashMap;
import java.util.Map;

/**
 * Шансы выпадения предметов снаряжения при смерти сущности.
 * Шанс {@code > 1.0} означает гарантированное выпадение без учёта уровня зачарования Мародёрство.
 * Шанс {@code <= 1.0} — вероятностное выпадение, увеличиваемое Мародёрством.
 * Значение по умолчанию {@link #DEFAULT_CHANCE} применяется ко всем слотам, не заданным явно.
 */
public record EquipmentDropChances(Map<EquipmentSlot, Float> byEquipment) {

	public static final float DEFAULT_CHANCE = 0.085F;
	public static final float UNHARMED_DROP_THRESHOLD = 1.0F;
	public static final int GUARANTEED_DROP_CHANCE = 2;

	public static final EquipmentDropChances DEFAULT =
		new EquipmentDropChances(Util.mapEnum(EquipmentSlot.class, slot -> DEFAULT_CHANCE));

	public static final Codec<EquipmentDropChances> CODEC =
		Codec.unboundedMap(EquipmentSlot.CODEC, Codecs.NON_NEGATIVE_FLOAT)
			.xmap(EquipmentDropChances::getWithDefaultChances, EquipmentDropChances::getWithoutDefaultChances)
			.xmap(EquipmentDropChances::new, EquipmentDropChances::byEquipment);

	private static Map<EquipmentSlot, Float> getWithoutDefaultChances(Map<EquipmentSlot, Float> byEquipment) {
		Map<EquipmentSlot, Float> map = new HashMap<>(byEquipment);
		map.values().removeIf(chance -> chance == DEFAULT_CHANCE);
		return map;
	}

	private static Map<EquipmentSlot, Float> getWithDefaultChances(Map<EquipmentSlot, Float> byEquipment) {
		return Util.mapEnum(EquipmentSlot.class, slot -> byEquipment.getOrDefault(slot, DEFAULT_CHANCE));
	}

	/**
	 * Возвращает копию с гарантированным выпадением для указанного слота.
	 * Значение {@code 2.0F} превышает порог {@link #UNHARMED_DROP_THRESHOLD},
	 * что означает выпадение без учёта Мародёрства.
	 *
	 * @param slot слот, для которого устанавливается гарантированное выпадение
	 * @return новый экземпляр с обновлённым шансом
	 */
	public EquipmentDropChances withGuaranteed(EquipmentSlot slot) {
		return withChance(slot, (float) GUARANTEED_DROP_CHANCE);
	}

	/**
	 * Возвращает копию с заданным шансом выпадения для указанного слота.
	 * Если шанс уже совпадает, возвращает {@code this}.
	 *
	 * @param slot   целевой слот
	 * @param chance новый шанс выпадения (должен быть {@code >= 0})
	 * @return новый экземпляр или {@code this} если значение не изменилось
	 * @throws IllegalArgumentException если шанс отрицательный
	 */
	public EquipmentDropChances withChance(EquipmentSlot slot, float chance) {
		if (chance < 0.0F) {
			throw new IllegalArgumentException("Tried to set invalid equipment chance " + chance + " for " + slot);
		}

		return get(slot) == chance
			? this
			: new EquipmentDropChances(Util.mapEnum(
				EquipmentSlot.class,
				targetSlot -> targetSlot == slot ? chance : get(targetSlot)
			));
	}

	public float get(EquipmentSlot slot) {
		return byEquipment.getOrDefault(slot, DEFAULT_CHANCE);
	}

	/**
	 * Проверяет, гарантировано ли выпадение предмета из данного слота.
	 * Шанс {@code > 1.0} означает выпадение без учёта Мародёрства.
	 *
	 * @param slot проверяемый слот
	 * @return {@code true} если шанс превышает {@link #UNHARMED_DROP_THRESHOLD}
	 */
	public boolean dropsExactly(EquipmentSlot slot) {
		return get(slot) > UNHARMED_DROP_THRESHOLD;
	}
}
