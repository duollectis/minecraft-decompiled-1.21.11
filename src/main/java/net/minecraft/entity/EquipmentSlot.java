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
 * {@code EquipmentSlot}.
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
	public static final StringIdentifiable.EnumCodec<EquipmentSlot>
			CODEC =
			StringIdentifiable.createCodec(EquipmentSlot::values);
	public static final PacketCodec<ByteBuf, EquipmentSlot>
			PACKET_CODEC =
			PacketCodecs.indexed(FROM_INDEX, EquipmentSlot::getIndex);
	private final EquipmentSlot.Type type;
	private final int entityId;
	private final int maxCount;
	private final int index;
	private final String name;

	private EquipmentSlot(
			final EquipmentSlot.Type type,
			final int entityId,
			final int maxCount,
			final int index,
			final String name
	) {
		this.type = type;
		this.entityId = entityId;
		this.maxCount = maxCount;
		this.index = index;
		this.name = name;
	}

	private EquipmentSlot(final EquipmentSlot.Type type, final int entityId, final int index, final String name) {
		this(type, entityId, 0, index, name);
	}

	public EquipmentSlot.Type getType() {
		return this.type;
	}

	public int getEntitySlotId() {
		return this.entityId;
	}

	public int getOffsetEntitySlotId(int offset) {
		return offset + this.entityId;
	}

	public ItemStack split(ItemStack stack) {
		return this.maxCount > 0 ? stack.split(this.maxCount) : stack;
	}

	public int getIndex() {
		return this.index;
	}

	public int getOffsetIndex(int offset) {
		return this.index + offset;
	}

	public String getName() {
		return this.name;
	}

	public boolean isArmorSlot() {
		return this.type == EquipmentSlot.Type.HUMANOID_ARMOR || this.type == EquipmentSlot.Type.ANIMAL_ARMOR;
	}

	@Override
	public String asString() {
		return this.name;
	}

	public boolean increasesDroppedExperience() {
		return this.type != EquipmentSlot.Type.SADDLE;
	}

	public static EquipmentSlot byName(String name) {
		EquipmentSlot equipmentSlot = CODEC.byId(name);
		if (equipmentSlot != null) {
			return equipmentSlot;
		}
		else {
			throw new IllegalArgumentException("Invalid slot '" + name + "'");
		}
	}

	/**
	 * {@code Type}.
	 */
	public static enum Type {
		HAND,
		HUMANOID_ARMOR,
		ANIMAL_ARMOR,
		SADDLE;
	}
}
