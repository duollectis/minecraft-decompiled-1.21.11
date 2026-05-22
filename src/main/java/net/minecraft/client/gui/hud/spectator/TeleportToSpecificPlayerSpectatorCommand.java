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

/** Команда телепорта к конкретному игроку по UUID его профиля. */
@Environment(EnvType.CLIENT)
public class TeleportToSpecificPlayerSpectatorCommand implements SpectatorMenuCommand {

	private final PlayerListEntry player;
	private final Text name;

	public TeleportToSpecificPlayerSpectatorCommand(PlayerListEntry player) {
		this.player = player;
		name = Text.literal(player.getProfile().name());
	}

	@Override
	public void use(SpectatorMenu menu) {
		MinecraftClient.getInstance()
			.getNetworkHandler()
			.sendPacket(new SpectatorTeleportC2SPacket(player.getProfile().id()));
	}

	@Override
	public Text getName() {
		return name;
	}

	@Override
	public void renderIcon(DrawContext context, float brightness, float alpha) {
		PlayerSkinDrawer.draw(context, player.getSkinTextures(), 2, 2, 12, ColorHelper.getWhite(alpha));
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
