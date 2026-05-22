package net.minecraft.screen.slot;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * Тип действия игрока со слотом инвентаря.
 * <p>
 * Определяет семантику клик-события, отправляемого сервером при взаимодействии
 * с экраном. Каждый тип обрабатывается отдельной ветвью логики в
 * {@link net.minecraft.screen.ScreenHandler#onSlotClick}.
 */
public enum SlotActionType {
	/** Обычный подбор/укладка предмета левой или правой кнопкой мыши. */
	PICKUP(0),
	/** Быстрое перемещение (Shift+клик) между инвентарями. */
	QUICK_MOVE(1),
	/** Обмен предмета в слоте с предметом на горячей панели (клавиши 1–9 или F). */
	SWAP(2),
	/** Клонирование предмета в режиме творчества (средняя кнопка мыши). */
	CLONE(3),
	/** Выброс предмета из слота (Q или Ctrl+Q). */
	THROW(4),
	/** Быстрый крафт — распределение предметов по слотам перетаскиванием. */
	QUICK_CRAFT(5),
	/** Подбор всех предметов того же типа двойным кликом. */
	PICKUP_ALL(6);

	private static final IntFunction<SlotActionType> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
			SlotActionType::getIndex, values(), ValueLists.OutOfBoundsHandling.ZERO
	);
	public static final PacketCodec<ByteBuf, SlotActionType> PACKET_CODEC =
			PacketCodecs.indexed(INDEX_MAPPER, SlotActionType::getIndex);

	private final int index;

	SlotActionType(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}
}
