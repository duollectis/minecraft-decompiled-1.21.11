package net.minecraft.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;

import java.util.List;
import java.util.function.IntFunction;

/**
 * Перечисление слотов снаряжения сущности.
 * Охватывает руки, броню гуманоидов, броню животных и седло.
 */
public enum EquipmentSlot implements StringIdentifiable {
	MAINHAND(EquipmentSlot.Type.HAND, 0, 0, "mainhand"),
	OFFHAND(EquipmentSlot.Type.HAND, 1, 5, "offhand"),
	FEET(EquipmentSlot.Type.HUMANOID_ARMOR, 0, 1, 1, "feet"),
	LEGS(EquipmentSlot.Type.HUMANOID_ARMOR, 1, 1, 2, "legs"),
	CHEST(EquipmentSlot.Type.HUMANOID_ARMOR, 2, 1, 3, "chest"),
	HEAD(EquipmentSlot.Type.HUMANOID_ARMOR, 3, 1, 4, "head"),
	BODY(EquipmentSlot.Type.ANIMAL_ARMOR, 0, 1, 6, "body"),
	SADDLE(EquipmentSlot.Type.SADDLE, 0, 1, 7, "saddle");

	public static final int NO_MAX_COUNT = 0;
	public static final List<EquipmentSlot> VALUES = List.of(values());
	public static final IntFunction<EquipmentSlot> FROM_INDEX = ValueLists.createIndexToValueFunction(
		(EquipmentSlot slot) -> slot.index, values(), ValueLists.OutOfBoundsHandling.ZERO
	);
	public static final StringIdentifiable.EnumCodec<EquipmentSlot> CODEC = StringIdentifiable.createCodec(EquipmentSlot::values);
	public static final PacketCodec<ByteBuf, EquipmentSlot> PACKET_CODEC = PacketCodecs.indexed(FROM_INDEX, EquipmentSlot::getIndex);

	private final EquipmentSlot.Type type;
	private final int entityId;
	private final int maxCount;
	private final int index;
	private final String name;

	private EquipmentSlot(EquipmentSlot.Type type, int entityId, int maxCount, int index, String name) {
		this.type = type;
		this.entityId = entityId;
		this.maxCount = maxCount;
		this.index = index;
		this.name = name;
	}

	private EquipmentSlot(EquipmentSlot.Type type, int entityId, int index, String name) {
		this(type, entityId, 0, index, name);
	}

	public EquipmentSlot.Type getType() {
		return type;
	}

	public int getEntitySlotId() {
		return entityId;
	}

	public int getOffsetEntitySlotId(int offset) {
		return offset + entityId;
	}

	/**
	 * Разделяет стак предмета согласно ограничению слота.
	 * Если {@code maxCount > 0}, возвращает стак размером не более {@code maxCount}.
	 * Иначе возвращает весь стак без изменений.
	 *
	 * @param stack исходный стак предмета
	 * @return стак, помещённый в данный слот
	 */
	public ItemStack split(ItemStack stack) {
		return maxCount > 0 ? stack.split(maxCount) : stack;
	}

	public int getIndex() {
		return index;
	}

	public int getOffsetIndex(int offset) {
		return index + offset;
	}

	public String getName() {
		return name;
	}

	public boolean isArmorSlot() {
		return type == EquipmentSlot.Type.HUMANOID_ARMOR || type == EquipmentSlot.Type.ANIMAL_ARMOR;
	}

	@Override
	public String asString() {
		return name;
	}

	/**
	 * Определяет, влияет ли предмет в данном слоте на количество опыта при выпадении.
	 * Слот {@link Type#SADDLE} не учитывается в расчёте опыта.
	 *
	 * @return {@code true} если слот участвует в расчёте выпадаемого опыта
	 */
	public boolean increasesDroppedExperience() {
		return type != EquipmentSlot.Type.SADDLE;
	}

	/**
	 * Находит слот по строковому имени.
	 *
	 * @param name строковый идентификатор слота
	 * @return соответствующий слот
	 * @throws IllegalArgumentException если слот с таким именем не существует
	 */
	public static EquipmentSlot byName(String name) {
		EquipmentSlot slot = CODEC.byId(name);
		if (slot != null) {
			return slot;
		}

		throw new IllegalArgumentException("Invalid slot '" + name + "'");
	}

	/**
	 * Категория слота снаряжения, определяющая тип носителя.
	 */
	public enum Type {
		HAND,
		HUMANOID_ARMOR,
		ANIMAL_ARMOR,
		SADDLE;
	}
}
