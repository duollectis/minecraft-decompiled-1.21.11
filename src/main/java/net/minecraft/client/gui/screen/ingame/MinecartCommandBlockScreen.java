package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.vehicle.CommandBlockMinecartEntity;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockMinecartC2SPacket;
import net.minecraft.world.CommandBlockExecutor;

/**
 * Экран редактирования командного блока в вагонетке.
 */
@Environment(EnvType.CLIENT)
public class MinecartCommandBlockScreen extends AbstractCommandBlockScreen {

	private static final int TRACK_OUTPUT_BUTTON_HEIGHT = 150;

	private final CommandBlockMinecartEntity commandBlockMinecart;

	public MinecartCommandBlockScreen(CommandBlockMinecartEntity commandBlockMinecartEntity) {
		commandBlockMinecart = commandBlockMinecartEntity;
	}

	@Override
	public CommandBlockExecutor getCommandExecutor() {
		return commandBlockMinecart.getCommandExecutor();
	}

	@Override
	int getTrackOutputButtonHeight() {
		return TRACK_OUTPUT_BUTTON_HEIGHT;
	}

	@Override
	protected void init() {
		super.init();
		consoleCommandTextField.setText(getCommandExecutor().getCommand());
	}

	@Override
	protected void syncSettingsToServer() {
		client.getNetworkHandler().sendPacket(new UpdateCommandBlockMinecartC2SPacket(
			commandBlockMinecart.getId(),
			consoleCommandTextField.getText(),
			commandBlockMinecart.getCommandExecutor().isTrackingOutput()
		));
	}
}
