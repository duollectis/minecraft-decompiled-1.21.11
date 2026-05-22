package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.network.message.MessageType;
import net.minecraft.server.PlayerManager;

/**
 * Команда {@code /say}: отправка сообщения всем игрокам от имени сервера.
 */
public class SayCommand {

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager.literal("say")
				                                                                 .requires(CommandManager.requirePermissionLevel(
						                                                                 CommandManager.GAMEMASTERS_CHECK))
				)
						.then(CommandManager.argument("message", MessageArgumentType.message()).executes(context -> {
							MessageArgumentType.getSignedMessage(
									context, "message", message -> {
										ServerCommandSource
												serverCommandSource =
												(ServerCommandSource) context.getSource();
										PlayerManager
												playerManager =
												serverCommandSource.getServer().getPlayerManager();
										playerManager.broadcast(
												message,
												serverCommandSource,
												MessageType.params(MessageType.SAY_COMMAND, serverCommandSource)
										);
									}
							);
							return 1;
						}))
		);
	}
}
