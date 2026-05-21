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
 * {@code OpCommand}.
 */
public class OpCommand {

	private static final SimpleCommandExceptionType
			ALREADY_OPPED_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("commands.op.failed"));

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager.literal("op")
				                                                                 .requires(CommandManager.requirePermissionLevel(
						                                                                 CommandManager.ADMINS_CHECK))
				)
						.then(
								CommandManager.argument("targets", GameProfileArgumentType.gameProfile())
								              .suggests(
										              (context, builder) -> {
											              PlayerManager
													              playerManager =
													              ((ServerCommandSource) context.getSource())
															              .getServer()
															              .getPlayerManager();
											              return CommandSource.suggestMatching(
													              playerManager.getPlayerList()
													                           .stream()
													                           .filter(player -> !playerManager.isOperator(
															                           player.getPlayerConfigEntry()))
													                           .map(player -> player
															                           .getGameProfile()
															                           .name()),
													              builder
											              );
										              }
								              )
								              .executes(context -> op(
										              (ServerCommandSource) context.getSource(),
										              GameProfileArgumentType.getProfileArgument(context, "targets")
								              ))
						)
		);
	}

	private static int op(ServerCommandSource source, Collection<PlayerConfigEntry> targets)
	throws CommandSyntaxException {
		PlayerManager playerManager = source.getServer().getPlayerManager();
		int i = 0;

		for (PlayerConfigEntry playerConfigEntry : targets) {
			if (!playerManager.isOperator(playerConfigEntry)) {
				playerManager.addToOperators(playerConfigEntry);
				i++;
				source.sendFeedback(() -> Text.translatable("commands.op.success", playerConfigEntry.name()), true);
			}
		}

		if (i == 0) {
			throw ALREADY_OPPED_EXCEPTION.create();
		}
		else {
			return i;
		}
	}
}
