package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.screen.ScreenHandler;

/**
 * Маркерный интерфейс для экранов, предоставляющих доступ к своему {@link ScreenHandler}.
 */
@Environment(EnvType.CLIENT)
public interface ScreenHandlerProvider<T extends ScreenHandler> {

	T getScreenHandler();
}
