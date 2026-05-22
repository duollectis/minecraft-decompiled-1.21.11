package net.minecraft.client.gui.hud.spectator;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

import java.util.List;

/** Корневая группа спектаторского меню: телепорт к игроку и телепорт к команде. */
@Environment(EnvType.CLIENT)
public class RootSpectatorCommandGroup implements SpectatorMenuCommandGroup {

	private static final Text PROMPT_TEXT = Text.translatable("spectatorMenu.root.prompt");

	private final List<SpectatorMenuCommand> elements = Lists.newArrayList();

	public RootSpectatorCommandGroup() {
		elements.add(new TeleportSpectatorMenu());
		elements.add(new TeamTeleportSpectatorMenu());
	}

	@Override
	public List<SpectatorMenuCommand> getCommands() {
		return elements;
	}

	@Override
	public Text getPrompt() {
		return PROMPT_TEXT;
	}
}
