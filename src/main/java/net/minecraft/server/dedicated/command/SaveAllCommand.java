package net.minecraft.server.dedicated.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Команда {@code /save-all}: принудительное сохранение всех миров (только dedicated).
 */
public class SaveAllCommand {

	private static final SimpleCommandExceptionType
			FAILED_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("commands.save.failed"));

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager
						.literal("save-all")
						.requires(CommandManager.requirePermissionLevel(CommandManager.OWNERS_CHECK))
				)
						.executes(context -> saveAll((ServerCommandSource) context.getSource(), false))
				)
						.then(CommandManager
								.literal("flush")
								.executes(context -> saveAll((ServerCommandSource) context.getSource(), true)))
		);
	}

	private static int saveAll(ServerCommandSource source, boolean flush) throws CommandSyntaxException {
		source.sendFeedback(() -> Text.translatable("commands.save.saving"), false);
		MinecraftServer minecraftServer = source.getServer();
		boolean bl = minecraftServer.saveAll(true, flush, true);
		if (!bl) {
			throw FAILED_EXCEPTION.create();
		}
		else {
			source.sendFeedback(() -> Text.translatable("commands.save.success"), true);
			return 1;
		}
	}
}
