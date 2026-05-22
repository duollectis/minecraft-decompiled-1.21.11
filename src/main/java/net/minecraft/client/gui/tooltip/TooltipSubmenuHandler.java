package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Обработчик взаимодействий с подменю тултипа слота инвентаря.
 * Позволяет реализовать специфическое поведение при прокрутке и кликах
 * для предметов с вложенным содержимым (например, сумки).
 *
 * @see BundleTooltipSubmenuHandler
 */
@Environment(EnvType.CLIENT)
public interface TooltipSubmenuHandler {

	/**
	 * Проверяет, применим ли данный обработчик к указанному слоту.
	 *
	 * @param slot слот инвентаря
	 * @return {@code true} если обработчик должен обрабатывать этот слот
	 */
	boolean isApplicableTo(Slot slot);

	/**
	 * Обрабатывает прокрутку колёсика мыши над слотом.
	 *
	 * @param horizontal горизонтальная составляющая прокрутки
	 * @param vertical   вертикальная составляющая прокрутки
	 * @param slotId     идентификатор слота
	 * @param item       предмет в слоте
	 * @return {@code true} если событие было обработано и не должно передаваться дальше
	 */
	boolean onScroll(double horizontal, double vertical, int slotId, ItemStack item);

	/** Сбрасывает состояние подменю для указанного слота. */
	void reset(Slot slot);

	/**
	 * Вызывается при клике мышью по слоту.
	 * Используется для сброса состояния при определённых типах действий.
	 *
	 * @param slot       слот, по которому кликнули
	 * @param actionType тип действия со слотом
	 */
	void onMouseClick(Slot slot, SlotActionType actionType);
}
