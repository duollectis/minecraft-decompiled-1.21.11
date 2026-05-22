package net.minecraft.inventory;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Реестр именованных диапазонов слотов, используемых в командах Minecraft
 * (например, {@code /item replace} и {@code /loot}).
 *
 * <p>Каждый диапазон имеет строковое имя и список идентификаторов слотов.
 * Диапазоны с суффиксом {@code .*} охватывают все слоты группы,
 * диапазоны с числовым суффиксом — конкретный слот.
 *
 * <p>Смещения слотов для экипировки вычисляются через
 * {@link EquipmentSlot#getOffsetEntitySlotId(int)} с базовыми константами,
 * определёнными в этом классе.
 */
public class SlotRanges {

	private static final int WEAPON_SLOT_OFFSET = 98;
	private static final int ARMOR_SLOT_OFFSET = 100;
	private static final int BODY_SLOT_OFFSET = 105;
	private static final int SADDLE_SLOT_OFFSET = 106;

	/** Идентификатор слота сундука лошади и курсора игрока (оба используют одно значение). */
	private static final int HORSE_CHEST_SLOT_ID = 499;
	private static final int PLAYER_CRAFTING_START_SLOT = 500;

	private static final List<SlotRange> SLOT_RANGES = Util.make(
		new ArrayList<>(), list -> {
			createAndAdd(list, "contents", 0);
			createAndAdd(list, "container.", 0, 54);
			createAndAdd(list, "hotbar.", 0, 9);
			createAndAdd(list, "inventory.", 9, 27);
			createAndAdd(list, "enderchest.", 200, 27);
			createAndAdd(list, "villager.", 300, 8);
			createAndAdd(list, "horse.", 500, 15);

			int mainhandSlot = EquipmentSlot.MAINHAND.getOffsetEntitySlotId(WEAPON_SLOT_OFFSET);
			int offhandSlot = EquipmentSlot.OFFHAND.getOffsetEntitySlotId(WEAPON_SLOT_OFFSET);
			createAndAdd(list, "weapon", mainhandSlot);
			createAndAdd(list, "weapon.mainhand", mainhandSlot);
			createAndAdd(list, "weapon.offhand", offhandSlot);
			createAndAdd(list, "weapon.*", mainhandSlot, offhandSlot);

			int headSlot = EquipmentSlot.HEAD.getOffsetEntitySlotId(ARMOR_SLOT_OFFSET);
			int chestSlot = EquipmentSlot.CHEST.getOffsetEntitySlotId(ARMOR_SLOT_OFFSET);
			int legsSlot = EquipmentSlot.LEGS.getOffsetEntitySlotId(ARMOR_SLOT_OFFSET);
			int feetSlot = EquipmentSlot.FEET.getOffsetEntitySlotId(ARMOR_SLOT_OFFSET);
			int bodySlot = EquipmentSlot.BODY.getOffsetEntitySlotId(BODY_SLOT_OFFSET);
			createAndAdd(list, "armor.head", headSlot);
			createAndAdd(list, "armor.chest", chestSlot);
			createAndAdd(list, "armor.legs", legsSlot);
			createAndAdd(list, "armor.feet", feetSlot);
			createAndAdd(list, "armor.body", bodySlot);
			createAndAdd(list, "armor.*", headSlot, chestSlot, legsSlot, feetSlot, bodySlot);

			createAndAdd(list, "saddle", EquipmentSlot.SADDLE.getOffsetEntitySlotId(SADDLE_SLOT_OFFSET));
			createAndAdd(list, "horse.chest", HORSE_CHEST_SLOT_ID);
			createAndAdd(list, "player.cursor", HORSE_CHEST_SLOT_ID);
			createAndAdd(list, "player.crafting.", PLAYER_CRAFTING_START_SLOT, 4);
		}
	);

	public static final Codec<SlotRange> CODEC =
		StringIdentifiable.createBasicCodec(() -> SLOT_RANGES.toArray(SlotRange[]::new));

	private static final Function<String, @Nullable SlotRange> FROM_NAME =
		StringIdentifiable.createMapper(SLOT_RANGES.toArray(SlotRange[]::new));

	private static SlotRange create(String name, int slotId) {
		return SlotRange.create(name, IntLists.singleton(slotId));
	}

	private static SlotRange create(String name, IntList slotIds) {
		return SlotRange.create(name, IntLists.unmodifiable(slotIds));
	}

	private static SlotRange create(String name, int... slotIds) {
		return SlotRange.create(name, IntList.of(slotIds));
	}

	private static void createAndAdd(List<SlotRange> list, String name, int slotId) {
		list.add(create(name, slotId));
	}

	/**
	 * Создаёт и добавляет в список диапазон из {@code slotCount} последовательных слотов,
	 * начиная с {@code firstSlotId}. Для каждого слота создаётся именованный диапазон
	 * {@code baseName + index}, а также сводный диапазон {@code baseName + "*"}.
	 *
	 * @param list        целевой список диапазонов
	 * @param baseName    базовое имя (например, {@code "hotbar."})
	 * @param firstSlotId идентификатор первого слота диапазона
	 * @param slotCount   количество слотов в диапазоне
	 */
	private static void createAndAdd(List<SlotRange> list, String baseName, int firstSlotId, int slotCount) {
		IntList slotIds = new IntArrayList(slotCount);

		for (int index = 0; index < slotCount; index++) {
			int slotId = firstSlotId + index;
			list.add(create(baseName + index, slotId));
			slotIds.add(slotId);
		}

		list.add(create(baseName + "*", slotIds));
	}

	private static void createAndAdd(List<SlotRange> list, String name, int... slots) {
		list.add(create(name, slots));
	}

	public static @Nullable SlotRange fromName(String name) {
		return FROM_NAME.apply(name);
	}

	public static Stream<String> streamNames() {
		return SLOT_RANGES.stream().map(StringIdentifiable::asString);
	}

	public static Stream<String> streamSingleSlotNames() {
		return SLOT_RANGES.stream()
			.filter(slotRange -> slotRange.getSlotCount() == 1)
			.map(StringIdentifiable::asString);
	}
}
