package net.minecraft.server.dedicated.command;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.BanEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;

/**
 * Команда {@code /banlist}: вывод списка заблокированных игроков и IP (только dedicated).
 */
public class BanListCommand {

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager
						.literal("banlist")
						.requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
				)
						.executes(
								context -> {
									PlayerManager
											playerManager =
											((ServerCommandSource) context.getSource()).getServer().getPlayerManager();
									return execute(
											(ServerCommandSource) context.getSource(),
											Lists.newArrayList(Iterables.concat(
													playerManager.getUserBanList().values(),
													playerManager.getIpBanList().values()
											))
									);
								}
						)
				)
						.then(
								CommandManager.literal("ips")
								              .executes(
										              context -> execute(
												              (ServerCommandSource) context.getSource(),
												              ((ServerCommandSource) context.getSource())
														              .getServer()
														              .getPlayerManager()
														              .getIpBanList()
														              .values()
										              )
								              )
						)
				)
						.then(
								CommandManager.literal("players")
								              .executes(
										              context -> execute(
												              (ServerCommandSource) context.getSource(),
												              ((ServerCommandSource) context.getSource())
														              .getServer()
														              .getPlayerManager()
														              .getUserBanList()
														              .values()
										              )
								              )
						)
		);
	}

	private static int execute(ServerCommandSource source, Collection<? extends BanEntry<?>> targets) {
		if (targets.isEmpty()) {
			source.sendFeedback(() -> Text.translatable("commands.banlist.none"), false);
		}
		else {
			source.sendFeedback(() -> Text.translatable("commands.banlist.list", targets.size()), false);

			for (BanEntry<?> banEntry : targets) {
				source.sendFeedback(
						() -> Text.translatable(
								"commands.banlist.entry",
								banEntry.toText(),
								banEntry.getSource(),
								banEntry.getReasonText()
						), false
				);
			}
		}

		return targets.size();
	}
}
