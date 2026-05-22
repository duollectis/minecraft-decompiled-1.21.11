package net.minecraft.entity;

import com.mojang.serialization.Codec;
import net.minecraft.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Хранит снаряжение сущности по слотам экипировки.
 * Внутренне использует {@link EnumMap} для эффективного доступа по {@link EquipmentSlot}.
 * Пустые стаки нормализуются к {@link ItemStack#EMPTY} при чтении и записи через кодек.
 */
public class EntityEquipment {

	public static final Codec<EntityEquipment> CODEC = Codec.unboundedMap(EquipmentSlot.CODEC, ItemStack.CODEC).xmap(
		map -> {
			EnumMap<EquipmentSlot, ItemStack> enumMap = new EnumMap<>(EquipmentSlot.class);
			enumMap.putAll(map);
			return new EntityEquipment(enumMap);
		},
		equipment -> {
			Map<EquipmentSlot, ItemStack> map = new EnumMap<>(equipment.map);
			map.values().removeIf(ItemStack::isEmpty);
			return map;
		}
	);

	private final EnumMap<EquipmentSlot, ItemStack> map;

	private EntityEquipment(EnumMap<EquipmentSlot, ItemStack> map) {
		this.map = map;
	}

	public EntityEquipment() {
		this(new EnumMap<>(EquipmentSlot.class));
	}

	/**
	 * Устанавливает предмет в слот, возвращая предыдущий.
	 *
	 * @param slot      целевой слот экипировки
	 * @param itemStack новый предмет
	 * @return предыдущий предмет в слоте, или {@link ItemStack#EMPTY} если слот был пуст
	 */
	public ItemStack put(EquipmentSlot slot, ItemStack itemStack) {
		return Objects.requireNonNullElse(map.put(slot, itemStack), ItemStack.EMPTY);
	}

	public ItemStack get(EquipmentSlot slot) {
		return map.getOrDefault(slot, ItemStack.EMPTY);
	}

	public boolean isEmpty() {
		for (ItemStack itemStack : map.values()) {
			if (!itemStack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Вызывает {@code inventoryTick} для каждого непустого предмета в снаряжении.
	 *
	 * @param entity сущность-носитель снаряжения
	 */
	public void tick(Entity entity) {
		for (Entry<EquipmentSlot, ItemStack> entry : map.entrySet()) {
			ItemStack itemStack = entry.getValue();
			if (!itemStack.isEmpty()) {
				itemStack.inventoryTick(entity.getEntityWorld(), entity, entry.getKey());
			}
		}
	}

	public void copyFrom(EntityEquipment equipment) {
		map.clear();
		map.putAll(equipment.map);
	}

	/**
	 * Выбрасывает все предметы снаряжения в мир и очищает слоты.
	 *
	 * @param entity сущность, от которой выбрасываются предметы
	 */
	public void dropAll(LivingEntity entity) {
		for (ItemStack itemStack : map.values()) {
			entity.dropItem(itemStack, true, false);
		}

		clear();
	}

	public void clear() {
		map.replaceAll((slot, stack) -> ItemStack.EMPTY);
	}
}
