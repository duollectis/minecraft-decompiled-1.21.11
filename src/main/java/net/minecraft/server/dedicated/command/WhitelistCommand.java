package net.minecraft.server.dedicated.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;

/**
 * Команда {@code /whitelist}: управление белым списком игроков (только dedicated).
 */
public class WhitelistCommand {

	private static final SimpleCommandExceptionType
			ALREADY_ON_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("commands.whitelist.alreadyOn"));
	private static final SimpleCommandExceptionType
			ALREADY_OFF_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("commands.whitelist.alreadyOff"));
	private static final SimpleCommandExceptionType
			ADD_FAILED_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("commands.whitelist.add.failed"));
	private static final SimpleCommandExceptionType REMOVE_FAILED_EXCEPTION = new SimpleCommandExceptionType(
			Text.translatable("commands.whitelist.remove.failed")
	);

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager
						.literal(
								"whitelist"
						)
						.requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
				)
						.then(CommandManager
								.literal("on")
								.executes(context -> executeOn((ServerCommandSource) context.getSource())))
				)
						.then(CommandManager
								.literal("off")
								.executes(context -> executeOff((ServerCommandSource) context.getSource())))
				)
						.then(CommandManager
								.literal("list")
								.executes(context -> executeList((ServerCommandSource) context.getSource())))
				)
						.then(
								CommandManager.literal("add")
								              .then(
										              CommandManager
												              .argument(
														              "targets",
														              GameProfileArgumentType.gameProfile()
												              )
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
																	                           .map(PlayerEntity::getPlayerConfigEntry)
																	                           .filter(playerConfigEntry -> !playerManager
																			                           .getWhitelist()
																			                           .isAllowed(
																					                           playerConfigEntry))
																	                           .map(PlayerConfigEntry::name),
																	              builder
															              );
														              }
												              )
												              .executes(
														              context -> executeAdd(
																              (ServerCommandSource) context.getSource(),
																              GameProfileArgumentType.getProfileArgument(
																		              context,
																		              "targets"
																              )
														              )
												              )
								              )
						)
				)
						.then(
								CommandManager.literal("remove")
								              .then(
										              CommandManager
												              .argument(
														              "targets",
														              GameProfileArgumentType.gameProfile()
												              )
												              .suggests(
														              (context, builder) -> CommandSource.suggestMatching(
																              ((ServerCommandSource) context.getSource())
																		              .getServer()
																		              .getPlayerManager()
																		              .getWhitelistedNames(), builder
														              )
												              )
												              .executes(
														              context -> executeRemove(
																              (ServerCommandSource) context.getSource(),
																              GameProfileArgumentType.getProfileArgument(
																		              context,
																		              "targets"
																              )
														              )
												              )
								              )
						)
				)
						.then(CommandManager
								.literal("reload")
								.executes(context -> executeReload((ServerCommandSource) context.getSource())))
		);
	}

	private static int executeReload(ServerCommandSource source) {
		source.getServer().getPlayerManager().reloadWhitelist();
		source.sendFeedback(() -> Text.translatable("commands.whitelist.reloaded"), true);
		source.getServer().kickNonWhitelistedPlayers();
		return 1;
	}

	private static int executeAdd(ServerCommandSource source, Collection<PlayerConfigEntry> targets)
	throws CommandSyntaxException {
		Whitelist whitelist = source.getServer().getPlayerManager().getWhitelist();
		int i = 0;

		for (PlayerConfigEntry playerConfigEntry : targets) {
			if (!whitelist.isAllowed(playerConfigEntry)) {
				WhitelistEntry whitelistEntry = new WhitelistEntry(playerConfigEntry);
				whitelist.add(whitelistEntry);
				source.sendFeedback(
						() -> Text.translatable(
								"commands.whitelist.add.success",
								Text.literal(playerConfigEntry.name())
						), true
				);
				i++;
			}
		}

		if (i == 0) {
			throw ADD_FAILED_EXCEPTION.create();
		}
		else {
			return i;
		}
	}

	private static int executeRemove(ServerCommandSource source, Collection<PlayerConfigEntry> targets)
	throws CommandSyntaxException {
		Whitelist whitelist = source.getServer().getPlayerManager().getWhitelist();
		int i = 0;

		for (PlayerConfigEntry playerConfigEntry : targets) {
			if (whitelist.isAllowed(playerConfigEntry)) {
				WhitelistEntry whitelistEntry = new WhitelistEntry(playerConfigEntry);
				whitelist.remove(whitelistEntry);
				source.sendFeedback(
						() -> Text.translatable(
								"commands.whitelist.remove.success",
								Text.literal(playerConfigEntry.name())
						), true
				);
				i++;
			}
		}

		if (i == 0) {
			throw REMOVE_FAILED_EXCEPTION.create();
		}
		else {
			source.getServer().kickNonWhitelistedPlayers();
			return i;
		}
	}

	private static int executeOn(ServerCommandSource source) throws CommandSyntaxException {
		if (source.getServer().getUseAllowlist()) {
			throw ALREADY_ON_EXCEPTION.create();
		}
		else {
			source.getServer().setUseAllowlist(true);
			source.sendFeedback(() -> Text.translatable("commands.whitelist.enabled"), true);
			source.getServer().kickNonWhitelistedPlayers();
			return 1;
		}
	}

	private static int executeOff(ServerCommandSource source) throws CommandSyntaxException {
		if (!source.getServer().getUseAllowlist()) {
			throw ALREADY_OFF_EXCEPTION.create();
		}
		else {
			source.getServer().setUseAllowlist(false);
			source.sendFeedback(() -> Text.translatable("commands.whitelist.disabled"), true);
			return 1;
		}
	}

	private static int executeList(ServerCommandSource source) {
		String[] strings = source.getServer().getPlayerManager().getWhitelistedNames();
		if (strings.length == 0) {
			source.sendFeedback(() -> Text.translatable("commands.whitelist.none"), false);
		}
		else {
			source.sendFeedback(
					() -> Text.translatable(
							"commands.whitelist.list",
							strings.length,
							String.join(", ", strings)
					), false
			);
		}

		return strings.length;
	}
}
