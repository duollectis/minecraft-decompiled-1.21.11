package net.minecraft.entity;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Таблица снаряжения, связывающая лут-таблицу с шансами выпадения по слотам.
 * Поддерживает компактную сериализацию: если все слоты имеют одинаковый шанс,
 * кодируется как одно число {@code float}, иначе — как карта слот→шанс.
 */
public record EquipmentTable(RegistryKey<LootTable> lootTable, Map<EquipmentSlot, Float> slotDropChances) {

	public static final Codec<Map<EquipmentSlot, Float>> SLOT_DROP_CHANCES_CODEC = Codec.either(
		Codec.FLOAT,
		Codec.unboundedMap(EquipmentSlot.CODEC, Codec.FLOAT)
	).xmap(
		either -> (Map<EquipmentSlot, Float>) either.map(
			EquipmentTable::createSlotDropChances,
			Function.identity()
		),
		map -> {
			boolean allSame = map.values().stream().distinct().count() == 1L;
			boolean allSlots = map.keySet().containsAll(EquipmentSlot.VALUES);
			return allSame && allSlots
				? Either.left(map.values().stream().findFirst().orElse(0.0F))
				: Either.right(map);
		}
	);

	public static final Codec<EquipmentTable> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			LootTable.TABLE_KEY.fieldOf("loot_table").forGetter(EquipmentTable::lootTable),
			SLOT_DROP_CHANCES_CODEC
				.optionalFieldOf("slot_drop_chances", Map.of())
				.forGetter(EquipmentTable::slotDropChances)
		).apply(instance, EquipmentTable::new)
	);

	public EquipmentTable(RegistryKey<LootTable> lootTable, float uniformDropChance) {
		this(lootTable, createSlotDropChances(uniformDropChance));
	}

	private static Map<EquipmentSlot, Float> createSlotDropChances(float dropChance) {
		return createSlotDropChances(List.of(EquipmentSlot.values()), dropChance);
	}

	private static Map<EquipmentSlot, Float> createSlotDropChances(List<EquipmentSlot> slots, float dropChance) {
		Map<EquipmentSlot, Float> chances = Maps.newHashMap();

		for (EquipmentSlot slot : slots) {
			chances.put(slot, dropChance);
		}

		return chances;
	}
}
