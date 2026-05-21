package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.screen.ScreenHandler;

@Environment(EnvType.CLIENT)
/**
 * {@code ScreenHandlerProvider}.
 */
public interface ScreenHandlerProvider<T extends ScreenHandler> {

	T getScreenHandler();
}
