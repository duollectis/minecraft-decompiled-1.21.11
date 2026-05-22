package net.minecraft.client.gui.hud.spectator;

import com.google.common.base.MoreObjects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;

/**
 * Снимок состояния спектаторского меню: список команд текущей страницы
 * и индекс выбранного слота. Используется для рендеринга в {@code SpectatorHud}.
 */
@Environment(EnvType.CLIENT)
public class SpectatorMenuState {

	public static final int NO_SELECTION = -1;

	private final List<SpectatorMenuCommand> commands;
	private final int selectedSlot;

	public SpectatorMenuState(List<SpectatorMenuCommand> commands, int selectedSlot) {
		this.commands = commands;
		this.selectedSlot = selectedSlot;
	}

	public SpectatorMenuCommand getCommand(int slot) {
		return slot >= 0 && slot < commands.size()
			? (SpectatorMenuCommand) MoreObjects.firstNonNull(commands.get(slot), SpectatorMenu.BLANK_COMMAND)
			: SpectatorMenu.BLANK_COMMAND;
	}

	public int getSelectedSlot() {
		return selectedSlot;
	}
}
