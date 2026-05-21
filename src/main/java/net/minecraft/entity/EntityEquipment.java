package net.minecraft.entity;

import com.mojang.serialization.Codec;
import net.minecraft.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * {@code EntityEquipment}.
 */
public class EntityEquipment {

	public static final Codec<EntityEquipment> CODEC = Codec.unboundedMap(EquipmentSlot.CODEC, ItemStack.CODEC).xmap(
			map -> {
				EnumMap<EquipmentSlot, ItemStack> enumMap = new EnumMap<>(EquipmentSlot.class);
				enumMap.putAll(map);
				return new EntityEquipment(enumMap);
			}, equipment -> {
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
	 * Put.
	 *
	 * @param slot slot
	 * @param itemStack item stack
	 *
	 * @return ItemStack — результат операции
	 */
	public ItemStack put(EquipmentSlot slot, ItemStack itemStack) {
		return Objects.requireNonNullElse(this.map.put(slot, itemStack), ItemStack.EMPTY);
	}

	/**
	 * Get.
	 *
	 * @param slot slot
	 *
	 * @return ItemStack — 
	 */
	public ItemStack get(EquipmentSlot slot) {
		return this.map.getOrDefault(slot, ItemStack.EMPTY);
	}

	public boolean isEmpty() {
		for (ItemStack itemStack : this.map.values()) {
			if (!itemStack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Tick.
	 *
	 * @param entity entity
	 */
	public void tick(Entity entity) {
		for (Entry<EquipmentSlot, ItemStack> entry : this.map.entrySet()) {
			ItemStack itemStack = entry.getValue();
			if (!itemStack.isEmpty()) {
				itemStack.inventoryTick(entity.getEntityWorld(), entity, entry.getKey());
			}
		}
	}

	/**
	 * Создаёт копию from.
	 *
	 * @param equipment equipment
	 */
	public void copyFrom(EntityEquipment equipment) {
		this.map.clear();
		this.map.putAll(equipment.map);
	}

	/**
	 * Бросает all.
	 *
	 * @param entity entity
	 */
	public void dropAll(LivingEntity entity) {
		for (ItemStack itemStack : this.map.values()) {
			entity.dropItem(itemStack, true, false);
		}

		this.clear();
	}

	/**
	 * Clear.
	 */
	public void clear() {
		this.map.replaceAll((slot, stack) -> ItemStack.EMPTY);
	}
}
