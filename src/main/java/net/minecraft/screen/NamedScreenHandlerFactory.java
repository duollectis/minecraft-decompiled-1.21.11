package net.minecraft.screen;

import net.fabricmc.fabric.api.screenhandler.v1.FabricScreenHandlerFactory;
import net.minecraft.text.Text;

/**
 * Расширение {@link ScreenHandlerFactory}, добавляющее отображаемое имя для заголовка экрана.
 * <p>
 * Используется при открытии именованных контейнеров (сундуки, печи и т.д.),
 * чтобы передать клиенту текст заголовка вместе с пакетом открытия экрана.
 */
public interface NamedScreenHandlerFactory extends ScreenHandlerFactory, FabricScreenHandlerFactory {

	Text getDisplayName();
}
