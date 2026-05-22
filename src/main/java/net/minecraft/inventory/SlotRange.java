package net.minecraft.inventory;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.util.StringIdentifiable;

/**
 * Именованный диапазон слотов инвентаря, используемый в командах и лут-механике.
 * Каждый диапазон имеет строковое имя (например, {@code "hotbar.0"} или {@code "armor.*"})
 * и список идентификаторов слотов, на которые он ссылается.
 */
public interface SlotRange extends StringIdentifiable {

	IntList getSlotIds();

	default int getSlotCount() {
		return getSlotIds().size();
	}

	/**
	 * Создаёт анонимную реализацию {@link SlotRange} с заданным именем и списком слотов.
	 *
	 * @param name    строковый идентификатор диапазона
	 * @param slotIds неизменяемый список идентификаторов слотов
	 * @return новый экземпляр {@link SlotRange}
	 */
	static SlotRange create(String name, IntList slotIds) {
		return new SlotRange() {
			@Override
			public IntList getSlotIds() {
				return slotIds;
			}

			@Override
			public String asString() {
				return name;
			}

			@Override
			public String toString() {
				return name;
			}
		};
	}
}
