package net.minecraft.client.gui.hud.spectator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Меню телепорта к команде. Показывает список команд из скорборда,
 * у каждой — случайный скин одного из участников.
 */
@Environment(EnvType.CLIENT)
public class TeamTeleportSpectatorMenu implements SpectatorMenuCommandGroup, SpectatorMenuCommand {

	private static final Identifier TEXTURE = Identifier.ofVanilla("spectator/teleport_to_team");
	private static final Text TEAM_TELEPORT_TEXT = Text.translatable("spectatorMenu.team_teleport");
	private static final Text PROMPT_TEXT = Text.translatable("spectatorMenu.team_teleport.prompt");

	private final List<SpectatorMenuCommand> commands;

	public TeamTeleportSpectatorMenu() {
		MinecraftClient client = MinecraftClient.getInstance();
		commands = buildCommands(client, client.world.getScoreboard());
	}

	private static List<SpectatorMenuCommand> buildCommands(MinecraftClient client, Scoreboard scoreboard) {
		return scoreboard.getTeams()
			.stream()
			.flatMap(team -> TeleportToSpecificTeamCommand.create(client, team).stream())
			.toList();
	}

	@Override
	public List<SpectatorMenuCommand> getCommands() {
		return commands;
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
		return TEAM_TELEPORT_TEXT;
	}

	@Override
	public void renderIcon(DrawContext context, float brightness, float alpha) {
		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			TEXTURE,
			0,
			0,
			16,
			16,
			ColorHelper.fromFloats(alpha, brightness, brightness, brightness)
		);
	}

	@Override
	public boolean isEnabled() {
		return !commands.isEmpty();
	}

	@Environment(EnvType.CLIENT)
	static class TeleportToSpecificTeamCommand implements SpectatorMenuCommand {

		private static final int ICON_PADDING = 1;
		private static final int ICON_INNER_SIZE = 14;
		private static final int SKIN_OFFSET = 2;
		private static final int SKIN_SIZE = 12;
		private static final int COLOR_CHANNEL_MAX = 255;

		private final Team team;
		private final Supplier<SkinTextures> skinTexturesSupplier;
		private final List<PlayerListEntry> scoreboardEntries;

		private TeleportToSpecificTeamCommand(
			Team team,
			List<PlayerListEntry> scoreboardEntries,
			Supplier<SkinTextures> skinTexturesSupplier
		) {
			this.team = team;
			this.scoreboardEntries = scoreboardEntries;
			this.skinTexturesSupplier = skinTexturesSupplier;
		}

		/**
		 * Создаёт команду для команды, если в ней есть хотя бы один не-спектатор.
		 * Для иконки выбирается случайный участник.
		 */
		public static Optional<SpectatorMenuCommand> create(MinecraftClient client, Team team) {
			List<PlayerListEntry> nonSpectators = new ArrayList<>();

			for (String playerName : team.getPlayerList()) {
				PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(playerName);
				if (entry != null && entry.getGameMode() != GameMode.SPECTATOR) {
					nonSpectators.add(entry);
				}
			}

			if (nonSpectators.isEmpty()) {
				return Optional.empty();
			}

			PlayerListEntry randomEntry = nonSpectators.get(Random.create().nextInt(nonSpectators.size()));
			return Optional.of(new TeleportToSpecificTeamCommand(team, nonSpectators, randomEntry::getSkinTextures));
		}

		@Override
		public void use(SpectatorMenu menu) {
			menu.selectElement(new TeleportSpectatorMenu(scoreboardEntries));
		}

		@Override
		public Text getName() {
			return team.getDisplayName();
		}

		@Override
		public void renderIcon(DrawContext context, float brightness, float alpha) {
			Integer teamColor = team.getColor().getColorValue();

			if (teamColor != null) {
				float red = (teamColor >> 16 & 0xFF) / (float) COLOR_CHANNEL_MAX;
				float green = (teamColor >> 8 & 0xFF) / (float) COLOR_CHANNEL_MAX;
				float blue = (teamColor & 0xFF) / (float) COLOR_CHANNEL_MAX;
				context.fill(
					ICON_PADDING,
					ICON_PADDING,
					ICON_INNER_SIZE + ICON_PADDING,
					ICON_INNER_SIZE + ICON_PADDING,
					ColorHelper.fromFloats(alpha, red * brightness, green * brightness, blue * brightness)
				);
			}

			PlayerSkinDrawer.draw(
				context,
				skinTexturesSupplier.get(),
				SKIN_OFFSET,
				SKIN_OFFSET,
				SKIN_SIZE,
				ColorHelper.fromFloats(alpha, brightness, brightness, brightness)
			);
		}

		@Override
		public boolean isEnabled() {
			return true;
		}
	}
}
