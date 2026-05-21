package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleVisitor;
import net.minecraft.world.rule.GameRules;

/**
 * {@code GameRuleCommand}.
 */
public class GameRuleCommand {

	public static void register(
			CommandDispatcher<ServerCommandSource> dispatcher,
			CommandRegistryAccess commandRegistryAccess
	) {
		final LiteralArgumentBuilder<ServerCommandSource>
				literalArgumentBuilder =
				(LiteralArgumentBuilder<ServerCommandSource>) CommandManager.literal("gamerule")
				                                                            .requires(CommandManager.requirePermissionLevel(
						                                                            CommandManager.GAMEMASTERS_CHECK));
		new GameRules(commandRegistryAccess.getEnabledFeatures())
				.accept(
						new GameRuleVisitor() {
							@Override
							public <T> void visit(GameRule<T> rule) {
								LiteralArgumentBuilder<ServerCommandSource>
										literalArgumentBuilderx =
										CommandManager.literal(rule.toShortString());
								LiteralArgumentBuilder<ServerCommandSource>
										literalArgumentBuilder2 =
										CommandManager.literal(rule.getId().toString());
								((LiteralArgumentBuilder) literalArgumentBuilder.then(GameRuleCommand.appendRule(
										rule,
										literalArgumentBuilderx
								))
								)
										.then(GameRuleCommand.appendRule(rule, literalArgumentBuilder2));
							}
						}
				);
		dispatcher.register(literalArgumentBuilder);
	}

	static <T> LiteralArgumentBuilder<ServerCommandSource> appendRule(
			GameRule<T> rule,
			LiteralArgumentBuilder<ServerCommandSource> builder
	) {
		return (LiteralArgumentBuilder<ServerCommandSource>) ((LiteralArgumentBuilder) builder.executes(
				context -> executeQuery((ServerCommandSource) context.getSource(), rule)
		)
		)
				.then(CommandManager
						.argument("value", rule.getArgumentType())
						.executes(context -> executeSet(context, rule)));
	}

	private static <T> int executeSet(CommandContext<ServerCommandSource> context, GameRule<T> key) {
		ServerCommandSource serverCommandSource = (ServerCommandSource) context.getSource();
		T object = (T) context.getArgument("value", key.getValueClass());
		serverCommandSource
				.getWorld()
				.getGameRules()
				.setValue(key, object, ((ServerCommandSource) context.getSource()).getServer());
		serverCommandSource.sendFeedback(
				() -> Text.translatable(
						"commands.gamerule.set",
						key.toShortString(),
						key.getValueName(object)
				), true
		);
		return key.getCommandResult(object);
	}

	private static <T> int executeQuery(ServerCommandSource source, GameRule<T> key) {
		T object = source.getWorld().getGameRules().getValue(key);
		source.sendFeedback(
				() -> Text.translatable(
						"commands.gamerule.query",
						key.toShortString(),
						key.getValueName(object)
				), false
		);
		return key.getCommandResult(object);
	}
}
