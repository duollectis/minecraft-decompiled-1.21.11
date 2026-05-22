package net.minecraft.client.gui.hud.spectator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

import java.util.List;

/** Группа команд спектаторского меню с заголовком-подсказкой. */
@Environment(EnvType.CLIENT)
public interface SpectatorMenuCommandGroup {

	List<SpectatorMenuCommand> getCommands();

	Text getPrompt();
}
