package net.minecraft.server.dedicated.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;

/**
 * {@code PardonCommand}.
 */
public class PardonCommand {

	private static final SimpleCommandExceptionType
			ALREADY_UNBANNED_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("commands.pardon.failed"));

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager.literal("pardon")
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
														              .getUserBanList()
														              .getNames(), builder
										              )
								              )
								              .executes(context -> pardon(
										              (ServerCommandSource) context.getSource(),
										              GameProfileArgumentType.getProfileArgument(context, "targets")
								              ))
						)
		);
	}

	private static int pardon(ServerCommandSource source, Collection<PlayerConfigEntry> targets)
	throws CommandSyntaxException {
		BannedPlayerList bannedPlayerList = source.getServer().getPlayerManager().getUserBanList();
		int i = 0;

		for (PlayerConfigEntry playerConfigEntry : targets) {
			if (bannedPlayerList.contains(playerConfigEntry)) {
				bannedPlayerList.remove(playerConfigEntry);
				i++;
				source.sendFeedback(
						() -> Text.translatable(
								"commands.pardon.success",
								Text.literal(playerConfigEntry.name())
						), true
				);
			}
		}

		if (i == 0) {
			throw ALREADY_UNBANNED_EXCEPTION.create();
		}
		else {
			return i;
		}
	}
}
