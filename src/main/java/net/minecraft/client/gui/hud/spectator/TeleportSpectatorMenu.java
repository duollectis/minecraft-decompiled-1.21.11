package net.minecraft.client.gui.hud.spectator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.GameMode;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
/**
 * {@code TeleportSpectatorMenu}.
 */
public class TeleportSpectatorMenu implements SpectatorMenuCommandGroup, SpectatorMenuCommand {

	private static final Identifier TELEPORT_TO_PLAYER_TEXTURE = Identifier.ofVanilla("spectator/teleport_to_player");
	private static final Comparator<PlayerListEntry> ORDERING = Comparator.comparing(a -> a.getProfile().id());
	private static final Text TELEPORT_TEXT = Text.translatable("spectatorMenu.teleport");
	private static final Text PROMPT_TEXT = Text.translatable("spectatorMenu.teleport.prompt");
	private final List<SpectatorMenuCommand> elements;

	public TeleportSpectatorMenu() {
		this(MinecraftClient.getInstance().getNetworkHandler().getListedPlayerListEntries());
	}

	public TeleportSpectatorMenu(Collection<PlayerListEntry> entries) {
		this.elements = entries.stream()
		                       .filter(entry -> entry.getGameMode() != GameMode.SPECTATOR)
		                       .sorted(ORDERING)
		                       .map(TeleportToSpecificPlayerSpectatorCommand::new)
		                       .collect(Collectors.toUnmodifiableList());
	}

	@Override
	public List<SpectatorMenuCommand> getCommands() {
		return this.elements;
	}

	@Override
	public Text getPrompt() {
		return PROMPT_TEXT;
	}

	@Override
	public void use(SpectatorMenu menu) {
		menu.selectElement(this);
	}

	@Override
	public Text getName() {
		return TELEPORT_TEXT;
	}

	@Override
	public void renderIcon(DrawContext context, float brightness, float alpha) {
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				TELEPORT_TO_PLAYER_TEXTURE,
				0,
				0,
				16,
				16,
				ColorHelper.fromFloats(alpha, brightness, brightness, brightness)
		);
	}

	@Override
	public boolean isEnabled() {
		return !this.elements.isEmpty();
	}
}
