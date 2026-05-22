package net.minecraft.screen;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.sync.TrackedSlot;

import java.util.List;

/**
 * Обработчик синхронизации состояния {@link ScreenHandler} между сервером и клиентом.
 * <p>
 * Реализуется сетевым слоем (ServerPlayNetworkHandler) и отвечает за отправку
 * пакетов обновления слотов, свойств и курсора конкретному игроку.
 */
public interface ScreenHandlerSyncHandler {

	void updateState(ScreenHandler handler, List<ItemStack> stacks, ItemStack cursorStack, int[] properties);

	void updateSlot(ScreenHandler handler, int slot, ItemStack stack);

	void updateCursorStack(ScreenHandler handler, ItemStack stack);

	void updateProperty(ScreenHandler handler, int property, int value);

	TrackedSlot createTrackedSlot();
}
