package net.minecraft.client.gui.hud.spectator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Одна команда в спектаторском меню (телепорт, смена страницы, закрытие и т.п.). */
@Environment(EnvType.CLIENT)
public interface SpectatorMenuCommand {

	void use(SpectatorMenu menu);

	Text getName();

	void renderIcon(DrawContext context, float brightness, float alpha);

	boolean isEnabled();
}
