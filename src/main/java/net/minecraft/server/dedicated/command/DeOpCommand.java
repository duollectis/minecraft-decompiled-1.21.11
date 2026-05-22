package net.minecraft.server.dedicated.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;

/**
 * Команда {@code /deop}: снятие прав оператора с игрока (только dedicated).
 */
public class DeOpCommand {

	private static final SimpleCommandExceptionType
			ALREADY_DEOPPED_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("commands.deop.failed"));

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager.literal("deop")
				                                                                 .requires(CommandManager.requirePermissionLevel(
						                                                                 CommandManager.ADMINS_CHECK))
				)
						.then(
								CommandManager.argument("targets", GameProfileArgumentType.gameProfile())
								              .suggests(
										              (context, builder) -> CommandSource.suggestMatching(
												              ((ServerCommandSource) context.getSource())
														              .getServer()
														              .getPlayerManager()
														              .getOpNames(), builder
										              )
								              )
								              .executes(context -> deop(
										              (ServerCommandSource) context.getSource(),
										              GameProfileArgumentType.getProfileArgument(context, "targets")
								              ))
						)
		);
	}

	private static int deop(ServerCommandSource source, Collection<PlayerConfigEntry> targets)
	throws CommandSyntaxException {
		PlayerManager playerManager = source.getServer().getPlayerManager();
		int i = 0;

		for (PlayerConfigEntry playerConfigEntry : targets) {
			if (playerManager.isOperator(playerConfigEntry)) {
				playerManager.removeFromOperators(playerConfigEntry);
				i++;
				source.sendFeedback(
						() -> Text.translatable("commands.deop.success", targets.iterator().next().name()),
						true
				);
			}
		}

		if (i == 0) {
			throw ALREADY_DEOPPED_EXCEPTION.create();
		}
		else {
			source.getServer().kickNonWhitelistedPlayers();
			return i;
		}
	}
}
