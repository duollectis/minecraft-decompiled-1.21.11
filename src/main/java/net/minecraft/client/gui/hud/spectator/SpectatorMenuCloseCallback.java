package net.minecraft.client.gui.hud.spectator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Колбэк закрытия спектаторского меню. */
@Environment(EnvType.CLIENT)
public interface SpectatorMenuCloseCallback {

	void close(SpectatorMenu menu);
}
