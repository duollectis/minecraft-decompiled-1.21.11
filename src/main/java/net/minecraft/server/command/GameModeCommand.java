package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.DefaultPermissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.command.permission.PermissionCheck;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.minecraft.world.rule.GameRules;

import java.util.Collection;
import java.util.Collections;

/**
 * Команда {@code /gamemode}: изменение режима игры игрока.
 */
public class GameModeCommand {

	public static final PermissionCheck PERMISSION_CHECK = new PermissionCheck.Require(DefaultPermissions.GAMEMASTERS);

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager
						.literal("gamemode")
						.requires(CommandManager.requirePermissionLevel(PERMISSION_CHECK))
				)
						.then(
								((RequiredArgumentBuilder) CommandManager
										.argument("gamemode", GameModeArgumentType.gameMode())
										.executes(
												context -> execute(
														context,
														Collections.singleton(((ServerCommandSource) context.getSource()).getPlayerOrThrow()),
														GameModeArgumentType.getGameMode(context, "gamemode")
												)
										)
								)
										.then(
												CommandManager.argument("target", EntityArgumentType.players())
												              .executes(
														              context -> execute(
																              context,
																              EntityArgumentType.getPlayers(
																		              context,
																		              "target"
																              ),
																              GameModeArgumentType.getGameMode(
																		              context,
																		              "gamemode"
																              )
														              )
												              )
										)
						)
		);
	}

	private static void sendFeedback(ServerCommandSource source, ServerPlayerEntity player, GameMode gameMode) {
		Text text = Text.translatable("gameMode." + gameMode.getId());
		if (source.getEntity() == player) {
			source.sendFeedback(() -> Text.translatable("commands.gamemode.success.self", text), true);
		}
		else {
			if (source.getWorld().getGameRules().getValue(GameRules.SEND_COMMAND_FEEDBACK)) {
				player.sendMessage(Text.translatable("gameMode.changed", text));
			}

			source.sendFeedback(
					() -> Text.translatable(
							"commands.gamemode.success.other",
							player.getDisplayName(),
							text
					), true
			);
		}
	}

	private static int execute(
			CommandContext<ServerCommandSource> context,
			Collection<ServerPlayerEntity> targets,
			GameMode gameMode
	) {
		int i = 0;

		for (ServerPlayerEntity serverPlayerEntity : targets) {
			if (execute((ServerCommandSource) context.getSource(), serverPlayerEntity, gameMode)) {
				i++;
			}
		}

		return i;
	}

	/**
	 * Execute.
	 *
	 * @param target target
	 * @param gameMode game mode
	 */
	public static void execute(ServerPlayerEntity target, GameMode gameMode) {
		execute(target.getCommandSource(), target, gameMode);
	}

	private static boolean execute(ServerCommandSource source, ServerPlayerEntity target, GameMode gameMode) {
		if (target.changeGameMode(gameMode)) {
			sendFeedback(source, target, gameMode);
			return true;
		}
		else {
			return false;
		}
	}
}
