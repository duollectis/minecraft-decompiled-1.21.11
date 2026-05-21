package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.vehicle.CommandBlockMinecartEntity;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockMinecartC2SPacket;
import net.minecraft.world.CommandBlockExecutor;

@Environment(EnvType.CLIENT)
/**
 * {@code MinecartCommandBlockScreen}.
 */
public class MinecartCommandBlockScreen extends AbstractCommandBlockScreen {

	private final CommandBlockMinecartEntity commandBlockMinecart;

	public MinecartCommandBlockScreen(CommandBlockMinecartEntity commandBlockMinecartEntity) {
		this.commandBlockMinecart = commandBlockMinecartEntity;
	}

	@Override
	public CommandBlockExecutor getCommandExecutor() {
		return this.commandBlockMinecart.getCommandExecutor();
	}

	@Override
	int getTrackOutputButtonHeight() {
		return 150;
	}

	@Override
	protected void init() {
		super.init();
		this.consoleCommandTextField.setText(this.getCommandExecutor().getCommand());
	}

	@Override
	protected void syncSettingsToServer() {
		this.client
				.getNetworkHandler()
				.sendPacket(
						new UpdateCommandBlockMinecartC2SPacket(
								this.commandBlockMinecart.getId(),
								this.consoleCommandTextField.getText(),
								this.commandBlockMinecart.getCommandExecutor().isTrackingOutput()
						)
				);
	}
}
