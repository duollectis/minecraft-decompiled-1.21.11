package net.minecraft.screen;

import net.minecraft.item.ItemStack;

/**
 * Слушатель изменений состояния {@link ScreenHandler}.
 * <p>
 * Реализуется на клиентской стороне для обновления визуального состояния экрана
 * при получении пакетов синхронизации от сервера.
 */
public interface ScreenHandlerListener {

	void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack);

	void onPropertyUpdate(ScreenHandler handler, int property, int value);
}
