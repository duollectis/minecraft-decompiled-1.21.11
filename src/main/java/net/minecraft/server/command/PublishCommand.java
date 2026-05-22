package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.NetworkUtils;
import net.minecraft.world.GameMode;
import org.jspecify.annotations.Nullable;

/**
 * Команда {@code /publish}: публикация одиночного мира в локальную сеть (только integrated).
 */
public class PublishCommand {

	private static final SimpleCommandExceptionType
			FAILED_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("commands.publish.failed"));
	private static final DynamicCommandExceptionType ALREADY_PUBLISHED_EXCEPTION = new DynamicCommandExceptionType(
			port -> Text.stringifiedTranslatable("commands.publish.alreadyPublished", port)
	);

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager
						.literal("publish")
						.requires(CommandManager.requirePermissionLevel(CommandManager.OWNERS_CHECK))
				)
						.executes(context -> execute(
								(ServerCommandSource) context.getSource(),
								NetworkUtils.findLocalPort(),
								false,
								null
						))
				)
						.then(
								((RequiredArgumentBuilder) CommandManager
										.argument("allowCommands", BoolArgumentType.bool())
										.executes(
												context -> execute(
														(ServerCommandSource) context.getSource(),
														NetworkUtils.findLocalPort(),
														BoolArgumentType.getBool(context, "allowCommands"),
														null
												)
										)
								)
										.then(
												((RequiredArgumentBuilder) CommandManager
														.argument("gamemode", GameModeArgumentType.gameMode())
														.executes(
																context -> execute(
																		(ServerCommandSource) context.getSource(),
																		NetworkUtils.findLocalPort(),
																		BoolArgumentType.getBool(
																				context,
																				"allowCommands"
																		),
																		GameModeArgumentType.getGameMode(
																				context,
																				"gamemode"
																		)
																)
														)
												)
														.then(
																CommandManager
																		.argument(
																				"port",
																				IntegerArgumentType.integer(0, 65535)
																		)
																		.executes(
																				context -> execute(
																						(ServerCommandSource) context.getSource(),
																						IntegerArgumentType.getInteger(
																								context,
																								"port"
																						),
																						BoolArgumentType.getBool(
																								context,
																								"allowCommands"
																						),
																						GameModeArgumentType.getGameMode(
																								context,
																								"gamemode"
																						)
																				)
																		)
														)
										)
						)
		);
	}

	private static int execute(ServerCommandSource source, int port, boolean allowCommands, @Nullable GameMode gameMode)
	throws CommandSyntaxException {
		if (source.getServer().isRemote()) {
			throw ALREADY_PUBLISHED_EXCEPTION.create(source.getServer().getServerPort());
		}
		else if (!source.getServer().openToLan(gameMode, allowCommands, port)) {
			throw FAILED_EXCEPTION.create();
		}
		else {
			source.sendFeedback(() -> getStartedText(port), true);
			return port;
		}
	}

	public static MutableText getStartedText(int port) {
		Text text = Texts.bracketedCopyable(String.valueOf(port));
		return Text.translatable("commands.publish.started", text);
	}
}
