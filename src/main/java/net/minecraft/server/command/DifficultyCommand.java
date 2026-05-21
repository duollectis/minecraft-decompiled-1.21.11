package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.world.Difficulty;

/**
 * {@code DifficultyCommand}.
 */
public class DifficultyCommand {

	private static final DynamicCommandExceptionType FAILURE_EXCEPTION = new DynamicCommandExceptionType(
			difficulty -> Text.stringifiedTranslatable("commands.difficulty.failure", difficulty)
	);

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> literalArgumentBuilder = CommandManager.literal("difficulty");

		for (Difficulty difficulty : Difficulty.values()) {
			literalArgumentBuilder.then(
					CommandManager
							.literal(difficulty.getName())
							.executes(context -> execute((ServerCommandSource) context.getSource(), difficulty))
			);
		}

		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) literalArgumentBuilder.requires(
						CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK)
				)
				)
						.executes(
								context -> {
									Difficulty
											difficultyx =
											((ServerCommandSource) context.getSource()).getWorld().getDifficulty();
									((ServerCommandSource) context.getSource())
											.sendFeedback(
													() -> Text.translatable(
															"commands.difficulty.query",
															difficultyx.getTranslatableName()
													), false
											);
									return difficultyx.getId();
								}
						)
		);
	}

	/**
	 * Execute.
	 *
	 * @param source source
	 * @param difficulty difficulty
	 *
	 * @return int — результат операции
	 */
	public static int execute(ServerCommandSource source, Difficulty difficulty) throws CommandSyntaxException {
		MinecraftServer minecraftServer = source.getServer();
		if (minecraftServer.getSaveProperties().getDifficulty() == difficulty) {
			throw FAILURE_EXCEPTION.create(difficulty.getName());
		}
		else {
			minecraftServer.setDifficulty(difficulty, true);
			source.sendFeedback(
					() -> Text.translatable(
							"commands.difficulty.success",
							difficulty.getTranslatableName()
					), true
			);
			return 0;
		}
	}
}
