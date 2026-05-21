package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.network.message.MessageType;
import net.minecraft.server.PlayerManager;

/**
 * {@code MeCommand}.
 */
public class MeCommand {

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) CommandManager
						.literal("me")
						.then(CommandManager.argument("action", MessageArgumentType.message()).executes(context -> {
							MessageArgumentType.getSignedMessage(
									context, "action", message -> {
										ServerCommandSource
												serverCommandSource =
												(ServerCommandSource) context.getSource();
										PlayerManager
												playerManager =
												serverCommandSource.getServer().getPlayerManager();
										playerManager.broadcast(
												message,
												serverCommandSource,
												MessageType.params(MessageType.EMOTE_COMMAND, serverCommandSource)
										);
									}
							);
							return 1;
						}))
		);
	}
}
