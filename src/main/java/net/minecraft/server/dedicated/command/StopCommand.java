package net.minecraft.server.dedicated.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Команда {@code /stop}: корректная остановка сервера (только dedicated).
 */
public class StopCommand {

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager.literal("stop")
				                                                                 .requires(CommandManager.requirePermissionLevel(
						                                                                 CommandManager.OWNERS_CHECK))
				)
						.executes(context -> {
							((ServerCommandSource) context.getSource()).sendFeedback(
									() -> Text.translatable("commands.stop.stopping"), true);
							((ServerCommandSource) context.getSource()).getServer().stop(false);
							return 1;
						})
		);
	}
}
