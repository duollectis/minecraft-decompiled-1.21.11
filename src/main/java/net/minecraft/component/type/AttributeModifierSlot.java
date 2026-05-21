package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;

import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * {@code AttributeModifierSlot}.
 */
public enum AttributeModifierSlot implements StringIdentifiable, Iterable<EquipmentSlot> {
	ANY(0, "any", slot -> true),
	MAINHAND(1, "mainhand", EquipmentSlot.MAINHAND),
	OFFHAND(2, "offhand", EquipmentSlot.OFFHAND),
	HAND(3, "hand", slot -> slot.getType() == EquipmentSlot.Type.HAND),
	FEET(4, "feet", EquipmentSlot.FEET),
	LEGS(5, "legs", EquipmentSlot.LEGS),
	CHEST(6, "chest", EquipmentSlot.CHEST),
	HEAD(7, "head", EquipmentSlot.HEAD),
	ARMOR(8, "armor", EquipmentSlot::isArmorSlot),
	BODY(9, "body", EquipmentSlot.BODY),
	SADDLE(10, "saddle", EquipmentSlot.SADDLE);

	public static final IntFunction<AttributeModifierSlot> ID_TO_VALUE = ValueLists.createIndexToValueFunction(
			(AttributeModifierSlot id) -> id.id, values(), ValueLists.OutOfBoundsHandling.ZERO
	);
	public static final Codec<AttributeModifierSlot>
			CODEC =
			StringIdentifiable.createCodec(AttributeModifierSlot::values);
	public static final PacketCodec<ByteBuf, AttributeModifierSlot>
			PACKET_CODEC =
			PacketCodecs.indexed(ID_TO_VALUE, id -> id.id);
	private final int id;
	private final String name;
	private final Predicate<EquipmentSlot> slotPredicate;
	private final List<EquipmentSlot> slots;

	private AttributeModifierSlot(final int id, final String name, final Predicate<EquipmentSlot> slotPredicate) {
		this.id = id;
		this.name = name;
		this.slotPredicate = slotPredicate;
		this.slots = EquipmentSlot.VALUES.stream().filter(slotPredicate).toList();
	}

	private AttributeModifierSlot(final int id, final String name, final EquipmentSlot slot) {
		this(id, name, slotx -> slotx == slot);
	}

	/**
	 * For equipment slot.
	 *
	 * @param slot slot
	 *
	 * @return AttributeModifierSlot — результат операции
	 */
	public static AttributeModifierSlot forEquipmentSlot(EquipmentSlot slot) {
		return switch (slot) {
			case MAINHAND -> MAINHAND;
			case OFFHAND -> OFFHAND;
			case FEET -> FEET;
			case LEGS -> LEGS;
			case CHEST -> CHEST;
			case HEAD -> HEAD;
			case BODY -> BODY;
			case SADDLE -> SADDLE;
		};
	}

	@Override
	public String asString() {
		return this.name;
	}

	/**
	 * Matches.
	 *
	 * @param slot slot
	 *
	 * @return boolean — результат операции
	 */
	public boolean matches(EquipmentSlot slot) {
		return this.slotPredicate.test(slot);
	}

	public List<EquipmentSlot> getSlots() {
		return this.slots;
	}

	@Override
	public Iterator<EquipmentSlot> iterator() {
		return this.slots.iterator();
	}
}
