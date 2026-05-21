package net.minecraft.client.gui.hud.spectator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code SpectatorMenuCommandGroup}.
 */
public interface SpectatorMenuCommandGroup {

	List<SpectatorMenuCommand> getCommands();

	Text getPrompt();
}
