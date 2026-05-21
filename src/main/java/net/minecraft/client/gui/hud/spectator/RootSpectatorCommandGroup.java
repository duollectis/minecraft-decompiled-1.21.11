package net.minecraft.client.gui.hud.spectator;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code RootSpectatorCommandGroup}.
 */
public class RootSpectatorCommandGroup implements SpectatorMenuCommandGroup {

	private static final Text PROMPT_TEXT = Text.translatable("spectatorMenu.root.prompt");
	private final List<SpectatorMenuCommand> elements = Lists.newArrayList();

	public RootSpectatorCommandGroup() {
		this.elements.add(new TeleportSpectatorMenu());
		this.elements.add(new TeamTeleportSpectatorMenu());
	}

	@Override
	public List<SpectatorMenuCommand> getCommands() {
		return this.elements;
	}

	@Override
	public Text getPrompt() {
		return PROMPT_TEXT;
	}
}
