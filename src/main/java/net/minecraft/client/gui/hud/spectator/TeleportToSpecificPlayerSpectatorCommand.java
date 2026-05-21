package net.minecraft.client.gui.hud.spectator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.play.SpectatorTeleportC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code TeleportToSpecificPlayerSpectatorCommand}.
 */
public class TeleportToSpecificPlayerSpectatorCommand implements SpectatorMenuCommand {

	private final PlayerListEntry player;
	private final Text name;

	public TeleportToSpecificPlayerSpectatorCommand(PlayerListEntry player) {
		this.player = player;
		this.name = Text.literal(player.getProfile().name());
	}

	@Override
	public void use(SpectatorMenu menu) {
		MinecraftClient
				.getInstance()
				.getNetworkHandler()
				.sendPacket(new SpectatorTeleportC2SPacket(this.player.getProfile().id()));
	}

	@Override
	public Text getName() {
		return this.name;
	}

	@Override
	public void renderIcon(DrawContext context, float brightness, float alpha) {
		PlayerSkinDrawer.draw(context, this.player.getSkinTextures(), 2, 2, 12, ColorHelper.getWhite(alpha));
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
