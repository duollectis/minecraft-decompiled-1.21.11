package net.minecraft.server.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.NumberRangeArgumentType;
import net.minecraft.predicate.NumberRange;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSequencesState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code RandomCommand}.
 */
public class RandomCommand {

	private static final SimpleCommandExceptionType RANGE_TOO_LARGE_EXCEPTION = new SimpleCommandExceptionType(
			Text.translatable("commands.random.error.range_too_large")
	);
	private static final SimpleCommandExceptionType RANGE_TOO_SMALL_EXCEPTION = new SimpleCommandExceptionType(
			Text.translatable("commands.random.error.range_too_small")
	);

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager
						.literal("random")
						.then(random("value", false))
				)
						.then(random("roll", true))
				)
						.then(
								((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager.literal("reset")
								                                                                  .requires(
										                                                                  CommandManager.requirePermissionLevel(
												                                                                  CommandManager.GAMEMASTERS_CHECK))
								)
										.then(
												((LiteralArgumentBuilder) CommandManager
														.literal("*")
														.executes(context -> executeReset((ServerCommandSource) context.getSource()))
												)
														.then(
																((RequiredArgumentBuilder) CommandManager
																		.argument("seed", IntegerArgumentType.integer())
																		.executes(
																				context -> executeReset(
																						(ServerCommandSource) context.getSource(),
																						IntegerArgumentType.getInteger(
																								context,
																								"seed"
																						),
																						true,
																						true
																				)
																		)
																)
																		.then(
																				((RequiredArgumentBuilder) CommandManager
																						.argument(
																								"includeWorldSeed",
																								BoolArgumentType.bool()
																						)
																						.executes(
																								context -> executeReset(
																										(ServerCommandSource) context.getSource(),
																										IntegerArgumentType.getInteger(
																												context,
																												"seed"
																										),
																										BoolArgumentType.getBool(
																												context,
																												"includeWorldSeed"
																										),
																										true
																								)
																						)
																				)
																						.then(
																								CommandManager
																										.argument(
																												"includeSequenceId",
																												BoolArgumentType.bool()
																										)
																										.executes(
																												context -> executeReset(
																														(ServerCommandSource) context.getSource(),
																														IntegerArgumentType.getInteger(
																																context,
																																"seed"
																														),
																														BoolArgumentType.getBool(
																																context,
																																"includeWorldSeed"
																														),
																														BoolArgumentType.getBool(
																																context,
																																"includeSequenceId"
																														)
																												)
																										)
																						)
																		)
														)
										)
								)
										.then(
												((RequiredArgumentBuilder) CommandManager
														.argument("sequence", IdentifierArgumentType.identifier())
														.suggests(RandomCommand::suggestSequences)
														.executes(
																context -> executeReset(
																		(ServerCommandSource) context.getSource(),
																		IdentifierArgumentType.getIdentifier(
																				context,
																				"sequence"
																		)
																)
														)
												)
														.then(
																((RequiredArgumentBuilder) CommandManager
																		.argument("seed", IntegerArgumentType.integer())
																		.executes(
																				context -> executeReset(
																						(ServerCommandSource) context.getSource(),
																						IdentifierArgumentType.getIdentifier(
																								context,
																								"sequence"
																						),
																						IntegerArgumentType.getInteger(
																								context,
																								"seed"
																						),
																						true,
																						true
																				)
																		)
																)
																		.then(
																				((RequiredArgumentBuilder) CommandManager
																						.argument(
																								"includeWorldSeed",
																								BoolArgumentType.bool()
																						)
																						.executes(
																								context -> executeReset(
																										(ServerCommandSource) context.getSource(),
																										IdentifierArgumentType.getIdentifier(
																												context,
																												"sequence"
																										),
																										IntegerArgumentType.getInteger(
																												context,
																												"seed"
																										),
																										BoolArgumentType.getBool(
																												context,
																												"includeWorldSeed"
																										),
																										true
																								)
																						)
																				)
																						.then(
																								CommandManager
																										.argument(
																												"includeSequenceId",
																												BoolArgumentType.bool()
																										)
																										.executes(
																												context -> executeReset(
																														(ServerCommandSource) context.getSource(),
																														IdentifierArgumentType.getIdentifier(
																																context,
																																"sequence"
																														),
																														IntegerArgumentType.getInteger(
																																context,
																																"seed"
																														),
																														BoolArgumentType.getBool(
																																context,
																																"includeWorldSeed"
																														),
																														BoolArgumentType.getBool(
																																context,
																																"includeSequenceId"
																														)
																												)
																										)
																						)
																		)
														)
										)
						)
		);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> random(String argumentName, boolean roll) {
		return (LiteralArgumentBuilder<ServerCommandSource>) CommandManager.literal(argumentName)
		                                                                   .then(
				                                                                   ((RequiredArgumentBuilder) CommandManager
						                                                                   .argument(
								                                                                   "range",
								                                                                   NumberRangeArgumentType.intRange()
						                                                                   )
						                                                                   .executes(
								                                                                   context -> execute(
										                                                                   (ServerCommandSource) context.getSource(),
										                                                                   NumberRangeArgumentType.IntRangeArgumentType.getRangeArgument(
												                                                                   context,
												                                                                   "range"
										                                                                   ),
										                                                                   null,
										                                                                   roll
								                                                                   )
						                                                                   )
				                                                                   )
						                                                                   .then(
								                                                                   ((RequiredArgumentBuilder) CommandManager
										                                                                   .argument(
												                                                                   "sequence",
												                                                                   IdentifierArgumentType.identifier()
										                                                                   )
										                                                                   .suggests(
												                                                                   RandomCommand::suggestSequences)
										                                                                   .requires(
												                                                                   CommandManager.requirePermissionLevel(
														                                                                   CommandManager.GAMEMASTERS_CHECK))
								                                                                   )
										                                                                   .executes(
												                                                                   context -> execute(
														                                                                   (ServerCommandSource) context.getSource(),
														                                                                   NumberRangeArgumentType.IntRangeArgumentType.getRangeArgument(
																                                                                   context,
																                                                                   "range"
														                                                                   ),
														                                                                   IdentifierArgumentType.getIdentifier(
																                                                                   context,
																                                                                   "sequence"
														                                                                   ),
														                                                                   roll
												                                                                   )
										                                                                   )
						                                                                   )
		                                                                   );
	}

	private static CompletableFuture<Suggestions> suggestSequences(
			CommandContext<ServerCommandSource> context,
			SuggestionsBuilder suggestionsBuilder
	) {
		List<String> list = Lists.newArrayList();
		((ServerCommandSource) context.getSource())
				.getWorld()
				.getRandomSequences()
				.forEachSequence((id, sequence) -> list.add(id.toString()));
		return CommandSource.suggestMatching(list, suggestionsBuilder);
	}

	private static int execute(
			ServerCommandSource source,
			NumberRange.IntRange range,
			@Nullable Identifier sequenceId,
			boolean roll
	) throws CommandSyntaxException {
		Random random;
		if (sequenceId != null) {
			random = source.getWorld().getOrCreateRandom(sequenceId);
		}
		else {
			random = source.getWorld().getRandom();
		}

		int i = range.getMin().orElse(Integer.MIN_VALUE);
		int j = range.getMax().orElse(Integer.MAX_VALUE);
		long l = (long) j - i;
		if (l == 0L) {
			throw RANGE_TOO_SMALL_EXCEPTION.create();
		}
		else if (l >= 2147483647L) {
			throw RANGE_TOO_LARGE_EXCEPTION.create();
		}
		else {
			int k = MathHelper.nextBetween(random, i, j);
			if (roll) {
				source
						.getServer()
						.getPlayerManager()
						.broadcast(Text.translatable("commands.random.roll", source.getDisplayName(), k, i, j), false);
			}
			else {
				source.sendFeedback(() -> Text.translatable("commands.random.sample.success", k), false);
			}

			return k;
		}
	}

	private static int executeReset(ServerCommandSource source, Identifier sequenceId) throws CommandSyntaxException {
		ServerWorld serverWorld = source.getWorld();
		serverWorld.getRandomSequences().reset(sequenceId, serverWorld.getSeed());
		source.sendFeedback(() -> Text.translatable("commands.random.reset.success", Text.of(sequenceId)), false);
		return 1;
	}

	private static int executeReset(
			ServerCommandSource source,
			Identifier sequenceId,
			int salt,
			boolean includeWorldSeed,
			boolean includeSequenceId
	) throws CommandSyntaxException {
		ServerWorld serverWorld = source.getWorld();
		serverWorld
				.getRandomSequences()
				.reset(sequenceId, serverWorld.getSeed(), salt, includeWorldSeed, includeSequenceId);
		source.sendFeedback(() -> Text.translatable("commands.random.reset.success", Text.of(sequenceId)), false);
		return 1;
	}

	private static int executeReset(ServerCommandSource source) {
		int i = source.getWorld().getRandomSequences().resetAll();
		source.sendFeedback(() -> Text.translatable("commands.random.reset.all.success", i), false);
		return i;
	}

	private static int executeReset(
			ServerCommandSource source,
			int salt,
			boolean includeWorldSeed,
			boolean includeSequenceId
	) {
		RandomSequencesState randomSequencesState = source.getWorld().getRandomSequences();
		randomSequencesState.setDefaultParameters(salt, includeWorldSeed, includeSequenceId);
		int i = randomSequencesState.resetAll();
		source.sendFeedback(() -> Text.translatable("commands.random.reset.all.success", i), false);
		return i;
	}
}
