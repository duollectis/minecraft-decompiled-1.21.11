package net.minecraft.server.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.*;
import net.minecraft.command.argument.*;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SlotRange;
import net.minecraft.inventory.StackReference;
import net.minecraft.inventory.StackReferenceGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.nbt.*;
import net.minecraft.predicate.NumberRange;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.*;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.MacroException;
import net.minecraft.server.function.Procedure;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.timer.stopwatch.Stopwatch;
import net.minecraft.world.timer.stopwatch.StopwatchPersistentState;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Команда {@code /execute}: выполнение команд с изменённым контекстом (позиция, сущность, условия).
 */
public class ExecuteCommand {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_BLOCKS = 32768;
	private static final Dynamic2CommandExceptionType BLOCKS_TOOBIG_EXCEPTION = new Dynamic2CommandExceptionType(
			(maxCount, count) -> Text.stringifiedTranslatable("commands.execute.blocks.toobig", maxCount, count)
	);
	private static final SimpleCommandExceptionType CONDITIONAL_FAIL_EXCEPTION = new SimpleCommandExceptionType(
			Text.translatable("commands.execute.conditional.fail")
	);
	private static final DynamicCommandExceptionType CONDITIONAL_FAIL_COUNT_EXCEPTION = new DynamicCommandExceptionType(
			count -> Text.stringifiedTranslatable("commands.execute.conditional.fail_count", count)
	);
	@VisibleForTesting
	public static final Dynamic2CommandExceptionType INSTANTIATION_FAILURE_EXCEPTION = new Dynamic2CommandExceptionType(
			(function, message) -> Text.stringifiedTranslatable(
					"commands.execute.function.instantiationFailure",
					function,
					message
			)
	);

	public static void register(
			CommandDispatcher<ServerCommandSource> dispatcher,
			CommandRegistryAccess commandRegistryAccess
	) {
		LiteralCommandNode<ServerCommandSource> literalCommandNode = dispatcher.register(
				(LiteralArgumentBuilder) CommandManager
						.literal("execute")
						.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
		);
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager
						.literal(
								"execute"
						)
						.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
				)
						.then(CommandManager.literal("run").redirect(dispatcher.getRoot()))
				)
						.then(addConditionArguments(
								literalCommandNode,
								CommandManager.literal("if"),
								true,
								commandRegistryAccess
						))
				)
						.then(addConditionArguments(
								literalCommandNode,
								CommandManager.literal("unless"),
								false,
								commandRegistryAccess
						))
				)
						.then(
								CommandManager.literal("as")
								              .then(CommandManager
										              .argument("targets", EntityArgumentType.entities())
										              .fork(
												              literalCommandNode, context -> {
													              List<ServerCommandSource> list = Lists.newArrayList();

													              for (Entity entity : EntityArgumentType.getOptionalEntities(
															              context,
															              "targets"
													              )) {
														              list.add(((ServerCommandSource) context.getSource()).withEntity(
																              entity));
													              }

													              return list;
												              }
										              ))
						)
				)
						.then(
								CommandManager.literal("at")
								              .then(
										              CommandManager.argument("targets", EntityArgumentType.entities())
										                            .fork(
												                            literalCommandNode,
												                            context -> {
													                            List<ServerCommandSource>
															                            list =
															                            Lists.newArrayList();

													                            for (Entity entity : EntityArgumentType.getOptionalEntities(
															                            context,
															                            "targets"
													                            )) {
														                            list.add(
																                            ((ServerCommandSource) context.getSource())
																		                            .withWorld((ServerWorld) entity.getEntityWorld())
																		                            .withPosition(entity.getEntityPos())
																		                            .withRotation(entity.getRotationClient())
														                            );
													                            }

													                            return list;
												                            }
										                            )
								              )
						)
				)
						.then(
								((LiteralArgumentBuilder) CommandManager.literal("store")
								                                        .then(addStoreArguments(
										                                        literalCommandNode,
										                                        CommandManager.literal("result"),
										                                        true
								                                        ))
								)
										.then(addStoreArguments(
												literalCommandNode,
												CommandManager.literal("success"),
												false
										))
						)
				)
						.then(
								((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager.literal("positioned")
								                                                                  .then(
										                                                                  CommandManager
												                                                                  .argument(
														                                                                  "pos",
														                                                                  Vec3ArgumentType.vec3()
												                                                                  )
												                                                                  .redirect(
														                                                                  literalCommandNode,
														                                                                  context -> ((ServerCommandSource) context.getSource())
																                                                                  .withPosition(
																		                                                                  Vec3ArgumentType.getVec3(
																				                                                                  context,
																				                                                                  "pos"
																		                                                                  ))
																                                                                  .withEntityAnchor(
																		                                                                  EntityAnchorArgumentType.EntityAnchor.FEET)
												                                                                  )
								                                                                  )
								)
										.then(
												CommandManager.literal("as")
												              .then(CommandManager
														              .argument(
																              "targets",
																              EntityArgumentType.entities()
														              )
														              .fork(
																              literalCommandNode, context -> {
																	              List<ServerCommandSource>
																			              list =
																			              Lists.newArrayList();

																	              for (Entity entity : EntityArgumentType.getOptionalEntities(
																			              context,
																			              "targets"
																	              )) {
																		              list.add(((ServerCommandSource) context.getSource()).withPosition(
																				              entity.getEntityPos()));
																	              }

																	              return list;
																              }
														              ))
										)
								)
										.then(
												CommandManager.literal("over")
												              .then(
														              CommandManager
																              .argument(
																		              "heightmap",
																		              HeightmapArgumentType.heightmap()
																              )
																              .redirect(
																		              literalCommandNode, context -> {
																			              Vec3d
																					              vec3d =
																					              ((ServerCommandSource) context.getSource()).getPosition();
																			              ServerWorld
																					              serverWorld =
																					              ((ServerCommandSource) context.getSource()).getWorld();
																			              double d = vec3d.getX();
																			              double e = vec3d.getZ();
																			              if (!serverWorld.isChunkLoaded(
																					              ChunkSectionPos.getSectionCoordFloored(
																							              d),
																					              ChunkSectionPos.getSectionCoordFloored(
																							              e)
																			              )) {
																				              throw BlockPosArgumentType.UNLOADED_EXCEPTION.create();
																			              }
																			              else {
																				              int
																						              i =
																						              serverWorld.getTopY(
																								              HeightmapArgumentType.getHeightmap(
																										              context,
																										              "heightmap"
																								              ),
																								              MathHelper.floor(
																										              d),
																								              MathHelper.floor(
																										              e)
																						              );
																				              return ((ServerCommandSource) context.getSource()).withPosition(
																						              new Vec3d(
																								              d,
																								              i,
																								              e
																						              ));
																			              }
																		              }
																              )
												              )
										)
						)
				)
						.then(
								((LiteralArgumentBuilder) CommandManager.literal("rotated")
								                                        .then(
										                                        CommandManager
												                                        .argument(
														                                        "rot",
														                                        RotationArgumentType.rotation()
												                                        )
												                                        .redirect(
														                                        literalCommandNode,
														                                        context -> ((ServerCommandSource) context.getSource())
																                                        .withRotation(
																		                                        RotationArgumentType
																				                                        .getRotation(
																						                                        context,
																						                                        "rot"
																				                                        )
																				                                        .getRotation(
																						                                        (ServerCommandSource) context.getSource())
																                                        )
												                                        )
								                                        )
								)
										.then(
												CommandManager.literal("as")
												              .then(CommandManager
														              .argument(
																              "targets",
																              EntityArgumentType.entities()
														              )
														              .fork(
																              literalCommandNode, context -> {
																	              List<ServerCommandSource>
																			              list =
																			              Lists.newArrayList();

																	              for (Entity entity : EntityArgumentType.getOptionalEntities(
																			              context,
																			              "targets"
																	              )) {
																		              list.add(((ServerCommandSource) context.getSource()).withRotation(
																				              entity.getRotationClient()));
																	              }

																	              return list;
																              }
														              ))
										)
						)
				)
						.then(
								((LiteralArgumentBuilder) CommandManager.literal("facing")
								                                        .then(
										                                        CommandManager.literal("entity")
										                                                      .then(
												                                                      CommandManager
														                                                      .argument(
																                                                      "targets",
																                                                      EntityArgumentType.entities()
														                                                      )
														                                                      .then(
																                                                      CommandManager
																		                                                      .argument(
																				                                                      "anchor",
																				                                                      EntityAnchorArgumentType.entityAnchor()
																		                                                      )
																		                                                      .fork(
																				                                                      literalCommandNode,
																				                                                      context -> {
																					                                                      List<ServerCommandSource>
																							                                                      list =
																							                                                      Lists.newArrayList();
																					                                                      EntityAnchorArgumentType.EntityAnchor
																							                                                      entityAnchor =
																							                                                      EntityAnchorArgumentType.getEntityAnchor(
																									                                                      context,
																									                                                      "anchor"
																							                                                      );

																					                                                      for (Entity entity : EntityArgumentType.getOptionalEntities(
																							                                                      context,
																							                                                      "targets"
																					                                                      )) {
																						                                                      list.add(
																								                                                      ((ServerCommandSource) context.getSource()).withLookingAt(
																										                                                      entity,
																										                                                      entityAnchor
																								                                                      ));
																					                                                      }

																					                                                      return list;
																				                                                      }
																		                                                      )
														                                                      )
										                                                      )
								                                        )
								)
										.then(
												CommandManager.argument("pos", Vec3ArgumentType.vec3())
												              .redirect(
														              literalCommandNode,
														              context -> ((ServerCommandSource) context.getSource()).withLookingAt(
																              Vec3ArgumentType.getVec3(context, "pos"))
												              )
										)
						)
				)
						.then(
								CommandManager.literal("align")
								              .then(
										              CommandManager.argument("axes", SwizzleArgumentType.swizzle())
										                            .redirect(
												                            literalCommandNode,
												                            context -> ((ServerCommandSource) context.getSource())
														                            .withPosition(
																                            ((ServerCommandSource) context.getSource())
																		                            .getPosition()
																		                            .floorAlongAxes(
																				                            SwizzleArgumentType.getSwizzle(
																						                            context,
																						                            "axes"
																				                            ))
														                            )
										                            )
								              )
						)
				)
						.then(
								CommandManager.literal("anchored")
								              .then(
										              CommandManager
												              .argument(
														              "anchor",
														              EntityAnchorArgumentType.entityAnchor()
												              )
												              .redirect(
														              literalCommandNode,
														              context -> ((ServerCommandSource) context.getSource())
																              .withEntityAnchor(EntityAnchorArgumentType.getEntityAnchor(
																		              context,
																		              "anchor"
																              ))
												              )
								              )
						)
				)
						.then(
								CommandManager.literal("in")
								              .then(
										              CommandManager
												              .argument("dimension", DimensionArgumentType.dimension())
												              .redirect(
														              literalCommandNode,
														              context -> ((ServerCommandSource) context.getSource())
																              .withWorld(DimensionArgumentType.getDimensionArgument(
																		              context,
																		              "dimension"
																              ))
												              )
								              )
						)
				)
						.then(
								CommandManager.literal("summon")
								              .then(
										              CommandManager
												              .argument(
														              "entity",
														              RegistryEntryReferenceArgumentType.registryEntry(
																              commandRegistryAccess,
																              RegistryKeys.ENTITY_TYPE
														              )
												              )
												              .suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES))
												              .redirect(
														              literalCommandNode,
														              context -> summon(
																              (ServerCommandSource) context.getSource(),
																              RegistryEntryReferenceArgumentType.getSummonableEntityType(
																		              context,
																		              "entity"
																              )
														              )
												              )
								              )
						)
				)
						.then(addOnArguments(literalCommandNode, CommandManager.literal("on")))
		);
	}

	private static ArgumentBuilder<ServerCommandSource, ?> addStoreArguments(
			LiteralCommandNode<ServerCommandSource> node,
			LiteralArgumentBuilder<ServerCommandSource> builder,
			boolean requestResult
	) {
		builder.then(
				CommandManager.literal("score")
				              .then(
						              CommandManager.argument("targets", ScoreHolderArgumentType.scoreHolders())
						                            .suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
						                            .then(
								                            CommandManager
										                            .argument(
												                            "objective",
												                            ScoreboardObjectiveArgumentType.scoreboardObjective()
										                            )
										                            .redirect(
												                            node,
												                            context -> executeStoreScore(
														                            (ServerCommandSource) context.getSource(),
														                            ScoreHolderArgumentType.getScoreboardScoreHolders(
																                            context,
																                            "targets"
														                            ),
														                            ScoreboardObjectiveArgumentType.getObjective(
																                            context,
																                            "objective"
														                            ),
														                            requestResult
												                            )
										                            )
						                            )
				              )
		);
		builder.then(
				CommandManager.literal("bossbar")
				              .then(
						              ((RequiredArgumentBuilder) CommandManager
								              .argument("id", IdentifierArgumentType.identifier())
								              .suggests(BossBarCommand.SUGGESTION_PROVIDER)
								              .then(
										              CommandManager.literal("value")
										                            .redirect(
												                            node,
												                            context -> executeStoreBossbar(
														                            (ServerCommandSource) context.getSource(),
														                            BossBarCommand.getBossBar(context),
														                            true,
														                            requestResult
												                            )
										                            )
								              )
						              )
								              .then(
										              CommandManager.literal("max")
										                            .redirect(
												                            node,
												                            context -> executeStoreBossbar(
														                            (ServerCommandSource) context.getSource(),
														                            BossBarCommand.getBossBar(context),
														                            false,
														                            requestResult
												                            )
										                            )
								              )
				              )
		);

		for (DataCommand.ObjectType objectType : DataCommand.TARGET_OBJECT_TYPES) {
			objectType.addArgumentsToBuilder(
					builder,
					builderx -> builderx.then(
							((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) CommandManager
									.argument(
											"path", NbtPathArgumentType.nbtPath()
									)
									.then(
											CommandManager.literal("int")
											              .then(
													              CommandManager
															              .argument(
																	              "scale",
																	              DoubleArgumentType.doubleArg()
															              )
															              .redirect(
																	              node,
																	              context -> executeStoreData(
																			              (ServerCommandSource) context.getSource(),
																			              objectType.getObject(context),
																			              NbtPathArgumentType.getNbtPath(
																					              context,
																					              "path"
																			              ),
																			              result -> NbtInt.of((int) (
																					              result
																							              * DoubleArgumentType.getDouble(
																							              context,
																							              "scale"
																					              )
																			              )),
																			              requestResult
																	              )
															              )
											              )
									)
							)
									.then(
											CommandManager.literal("float")
											              .then(
													              CommandManager
															              .argument(
																	              "scale",
																	              DoubleArgumentType.doubleArg()
															              )
															              .redirect(
																	              node,
																	              context -> executeStoreData(
																			              (ServerCommandSource) context.getSource(),
																			              objectType.getObject(context),
																			              NbtPathArgumentType.getNbtPath(
																					              context,
																					              "path"
																			              ),
																			              result -> NbtFloat.of((float) (
																					              result
																							              * DoubleArgumentType.getDouble(
																							              context,
																							              "scale"
																					              )
																			              )),
																			              requestResult
																	              )
															              )
											              )
									)
							)
									.then(
											CommandManager.literal("short")
											              .then(
													              CommandManager
															              .argument(
																	              "scale",
																	              DoubleArgumentType.doubleArg()
															              )
															              .redirect(
																	              node,
																	              context -> executeStoreData(
																			              (ServerCommandSource) context.getSource(),
																			              objectType.getObject(context),
																			              NbtPathArgumentType.getNbtPath(
																					              context,
																					              "path"
																			              ),
																			              result -> NbtShort.of((short) (
																					              result
																							              * DoubleArgumentType.getDouble(
																							              context,
																							              "scale"
																					              )
																			              )),
																			              requestResult
																	              )
															              )
											              )
									)
							)
									.then(
											CommandManager.literal("long")
											              .then(
													              CommandManager
															              .argument(
																	              "scale",
																	              DoubleArgumentType.doubleArg()
															              )
															              .redirect(
																	              node,
																	              context -> executeStoreData(
																			              (ServerCommandSource) context.getSource(),
																			              objectType.getObject(context),
																			              NbtPathArgumentType.getNbtPath(
																					              context,
																					              "path"
																			              ),
																			              result -> NbtLong.of((long) (
																					              result
																							              * DoubleArgumentType.getDouble(
																							              context,
																							              "scale"
																					              )
																			              )),
																			              requestResult
																	              )
															              )
											              )
									)
							)
									.then(
											CommandManager.literal("double")
											              .then(
													              CommandManager
															              .argument(
																	              "scale",
																	              DoubleArgumentType.doubleArg()
															              )
															              .redirect(
																	              node,
																	              context -> executeStoreData(
																			              (ServerCommandSource) context.getSource(),
																			              objectType.getObject(context),
																			              NbtPathArgumentType.getNbtPath(
																					              context,
																					              "path"
																			              ),
																			              result -> NbtDouble.of(result
																					              * DoubleArgumentType.getDouble(
																					              context,
																					              "scale"
																			              )),
																			              requestResult
																	              )
															              )
											              )
									)
							)
									.then(
											CommandManager.literal("byte")
											              .then(
													              CommandManager
															              .argument(
																	              "scale",
																	              DoubleArgumentType.doubleArg()
															              )
															              .redirect(
																	              node,
																	              context -> executeStoreData(
																			              (ServerCommandSource) context.getSource(),
																			              objectType.getObject(context),
																			              NbtPathArgumentType.getNbtPath(
																					              context,
																					              "path"
																			              ),
																			              result -> NbtByte.of((byte) (
																					              result
																							              * DoubleArgumentType.getDouble(
																							              context,
																							              "scale"
																					              )
																			              )),
																			              requestResult
																	              )
															              )
											              )
									)
					)
			);
		}

		return builder;
	}

	private static ServerCommandSource executeStoreScore(
			ServerCommandSource source,
			Collection<ScoreHolder> targets,
			ScoreboardObjective objective,
			boolean requestResult
	) {
		Scoreboard scoreboard = source.getServer().getScoreboard();
		return source.mergeReturnValueConsumers(
				(successful, returnValue) -> {
					for (ScoreHolder scoreHolder : targets) {
						ScoreAccess scoreAccess = scoreboard.getOrCreateScore(scoreHolder, objective);
						int i = requestResult ? returnValue : (successful ? 1 : 0);
						scoreAccess.setScore(i);
					}
				}, ReturnValueConsumer::chain
		);
	}

	private static ServerCommandSource executeStoreBossbar(
			ServerCommandSource source,
			CommandBossBar bossBar,
			boolean storeInValue,
			boolean requestResult
	) {
		return source.mergeReturnValueConsumers(
				(successful, returnValue) -> {
					int i = requestResult ? returnValue : (successful ? 1 : 0);
					if (storeInValue) {
						bossBar.setValue(i);
					}
					else {
						bossBar.setMaxValue(i);
					}
				}, ReturnValueConsumer::chain
		);
	}

	private static ServerCommandSource executeStoreData(
			ServerCommandSource source,
			DataCommandObject object,
			NbtPathArgumentType.NbtPath path,
			IntFunction<NbtElement> nbtSetter,
			boolean requestResult
	) {
		return source.mergeReturnValueConsumers(
				(successful, returnValue) -> {
					try {
						NbtCompound nbtCompound = object.getNbt();
						int i = requestResult ? returnValue : (successful ? 1 : 0);
						path.put(nbtCompound, nbtSetter.apply(i));
						object.setNbt(nbtCompound);
					}
					catch (CommandSyntaxException var8) {
					}
				}, ReturnValueConsumer::chain
		);
	}

	private static boolean isLoaded(ServerWorld world, BlockPos pos) {
		ChunkPos chunkPos = new ChunkPos(pos);
		WorldChunk worldChunk = world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z);
		return worldChunk == null ? false
		                          : worldChunk.getLevelType() == ChunkLevelType.ENTITY_TICKING && world.isChunkLoaded(
				                          chunkPos.toLong());
	}

	private static ArgumentBuilder<ServerCommandSource, ?> addConditionArguments(
			CommandNode<ServerCommandSource> root,
			LiteralArgumentBuilder<ServerCommandSource> argumentBuilder,
			boolean positive,
			CommandRegistryAccess commandRegistryAccess
	) {
		((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) argumentBuilder.then(
				CommandManager.literal("block")
				              .then(
						              CommandManager.argument("pos", BlockPosArgumentType.blockPos())
						                            .then(
								                            addConditionLogic(
										                            root,
										                            CommandManager.argument(
												                            "block",
												                            BlockPredicateArgumentType.blockPredicate(
														                            commandRegistryAccess)
										                            ),
										                            positive,
										                            context -> BlockPredicateArgumentType
												                            .getBlockPredicate(context, "block")
												                            .test(
														                            new CachedBlockPosition(
																                            ((ServerCommandSource) context.getSource()).getWorld(),
																                            BlockPosArgumentType.getLoadedBlockPos(
																		                            context,
																		                            "pos"
																                            ),
																                            true
														                            )
												                            )
								                            )
						                            )
				              )
		)
		)
				.then(
						CommandManager.literal("biome")
						              .then(
								              CommandManager.argument("pos", BlockPosArgumentType.blockPos())
								                            .then(
										                            addConditionLogic(
												                            root,
												                            CommandManager.argument(
														                            "biome",
														                            RegistryEntryPredicateArgumentType.registryEntryPredicate(
																                            commandRegistryAccess,
																                            RegistryKeys.BIOME
														                            )
												                            ),
												                            positive,
												                            context -> RegistryEntryPredicateArgumentType
														                            .getRegistryEntryPredicate(
																                            context,
																                            "biome",
																                            RegistryKeys.BIOME
														                            )
														                            .test(
																                            ((ServerCommandSource) context.getSource())
																		                            .getWorld()
																		                            .getBiome(
																				                            BlockPosArgumentType.getLoadedBlockPos(
																						                            context,
																						                            "pos"
																				                            ))
														                            )
										                            )
								                            )
						              )
				)
		)
				.then(
						CommandManager.literal("loaded")
						              .then(
								              addConditionLogic(
										              root,
										              CommandManager.argument("pos", BlockPosArgumentType.blockPos()),
										              positive,
										              context -> isLoaded(
												              ((ServerCommandSource) context.getSource()).getWorld(),
												              BlockPosArgumentType.getBlockPos(context, "pos")
										              )
								              )
						              )
				)
		)
				.then(
						CommandManager.literal("dimension")
						              .then(
								              addConditionLogic(
										              root,
										              CommandManager.argument(
												              "dimension",
												              DimensionArgumentType.dimension()
										              ),
										              positive,
										              context -> DimensionArgumentType.getDimensionArgument(
												              context,
												              "dimension"
										              )
												              == ((ServerCommandSource) context.getSource()).getWorld()
								              )
						              )
				)
		)
				.then(
						CommandManager.literal("score")
						              .then(
								              CommandManager.argument("target", ScoreHolderArgumentType.scoreHolder())
								                            .suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
								                            .then(
										                            ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) CommandManager
												                            .argument(
														                            "targetObjective",
														                            ScoreboardObjectiveArgumentType.scoreboardObjective()
												                            )
												                            .then(
														                            CommandManager.literal("=")
														                                          .then(
																                                          CommandManager
																		                                          .argument(
																				                                          "source",
																				                                          ScoreHolderArgumentType.scoreHolder()
																		                                          )
																		                                          .suggests(
																				                                          ScoreHolderArgumentType.SUGGESTION_PROVIDER)
																		                                          .then(
																				                                          addConditionLogic(
																						                                          root,
																						                                          CommandManager.argument(
																								                                          "sourceObjective",
																								                                          ScoreboardObjectiveArgumentType.scoreboardObjective()
																						                                          ),
																						                                          positive,
																						                                          context -> testScoreCondition(
																								                                          context,
																								                                          (targetScore, sourceScore) ->
																										                                          targetScore
																												                                          == sourceScore
																						                                          )
																				                                          )
																		                                          )
														                                          )
												                            )
										                            )
												                            .then(
														                            CommandManager.literal("<")
														                                          .then(
																                                          CommandManager
																		                                          .argument(
																				                                          "source",
																				                                          ScoreHolderArgumentType.scoreHolder()
																		                                          )
																		                                          .suggests(
																				                                          ScoreHolderArgumentType.SUGGESTION_PROVIDER)
																		                                          .then(
																				                                          addConditionLogic(
																						                                          root,
																						                                          CommandManager.argument(
																								                                          "sourceObjective",
																								                                          ScoreboardObjectiveArgumentType.scoreboardObjective()
																						                                          ),
																						                                          positive,
																						                                          context -> testScoreCondition(
																								                                          context,
																								                                          (targetScore, sourceScore) ->
																										                                          targetScore
																												                                          < sourceScore
																						                                          )
																				                                          )
																		                                          )
														                                          )
												                            )
										                            )
												                            .then(
														                            CommandManager.literal("<=")
														                                          .then(
																                                          CommandManager
																		                                          .argument(
																				                                          "source",
																				                                          ScoreHolderArgumentType.scoreHolder()
																		                                          )
																		                                          .suggests(
																				                                          ScoreHolderArgumentType.SUGGESTION_PROVIDER)
																		                                          .then(
																				                                          addConditionLogic(
																						                                          root,
																						                                          CommandManager.argument(
																								                                          "sourceObjective",
																								                                          ScoreboardObjectiveArgumentType.scoreboardObjective()
																						                                          ),
																						                                          positive,
																						                                          context -> testScoreCondition(
																								                                          context,
																								                                          (targetScore, sourceScore) ->
																										                                          targetScore
																												                                          <= sourceScore
																						                                          )
																				                                          )
																		                                          )
														                                          )
												                            )
										                            )
												                            .then(
														                            CommandManager.literal(">")
														                                          .then(
																                                          CommandManager
																		                                          .argument(
																				                                          "source",
																				                                          ScoreHolderArgumentType.scoreHolder()
																		                                          )
																		                                          .suggests(
																				                                          ScoreHolderArgumentType.SUGGESTION_PROVIDER)
																		                                          .then(
																				                                          addConditionLogic(
																						                                          root,
																						                                          CommandManager.argument(
																								                                          "sourceObjective",
																								                                          ScoreboardObjectiveArgumentType.scoreboardObjective()
																						                                          ),
																						                                          positive,
																						                                          context -> testScoreCondition(
																								                                          context,
																								                                          (targetScore, sourceScore) ->
																										                                          targetScore
																												                                          > sourceScore
																						                                          )
																				                                          )
																		                                          )
														                                          )
												                            )
										                            )
												                            .then(
														                            CommandManager.literal(">=")
														                                          .then(
																                                          CommandManager
																		                                          .argument(
																				                                          "source",
																				                                          ScoreHolderArgumentType.scoreHolder()
																		                                          )
																		                                          .suggests(
																				                                          ScoreHolderArgumentType.SUGGESTION_PROVIDER)
																		                                          .then(
																				                                          addConditionLogic(
																						                                          root,
																						                                          CommandManager.argument(
																								                                          "sourceObjective",
																								                                          ScoreboardObjectiveArgumentType.scoreboardObjective()
																						                                          ),
																						                                          positive,
																						                                          context -> testScoreCondition(
																								                                          context,
																								                                          (targetScore, sourceScore) ->
																										                                          targetScore
																												                                          >= sourceScore
																						                                          )
																				                                          )
																		                                          )
														                                          )
												                            )
										                            )
												                            .then(
														                            CommandManager.literal("matches")
														                                          .then(
																                                          addConditionLogic(
																		                                          root,
																		                                          CommandManager.argument(
																				                                          "range",
																				                                          NumberRangeArgumentType.intRange()
																		                                          ),
																		                                          positive,
																		                                          context -> testScoreMatch(
																				                                          context,
																				                                          NumberRangeArgumentType.IntRangeArgumentType.getRangeArgument(
																						                                          context,
																						                                          "range"
																				                                          )
																		                                          )
																                                          )
														                                          )
												                            )
								                            )
						              )
				)
		)
				.then(
						CommandManager.literal("blocks")
						              .then(
								              CommandManager.argument("start", BlockPosArgumentType.blockPos())
								                            .then(
										                            CommandManager
												                            .argument(
														                            "end",
														                            BlockPosArgumentType.blockPos()
												                            )
												                            .then(
														                            ((RequiredArgumentBuilder) CommandManager
																                            .argument(
																		                            "destination",
																		                            BlockPosArgumentType.blockPos()
																                            )
																                            .then(addBlocksConditionLogic(
																		                            root,
																		                            CommandManager.literal(
																				                            "all"),
																		                            positive,
																		                            false
																                            ))
														                            )
																                            .then(addBlocksConditionLogic(
																		                            root,
																		                            CommandManager.literal(
																				                            "masked"),
																		                            positive,
																		                            true
																                            ))
												                            )
								                            )
						              )
				)
		)
				.then(
						CommandManager.literal("entity")
						              .then(
								              ((RequiredArgumentBuilder) CommandManager
										              .argument("entities", EntityArgumentType.entities())
										              .fork(
												              root,
												              context -> getSourceOrEmptyForConditionFork(
														              context,
														              positive,
														              !EntityArgumentType
																              .getOptionalEntities(context, "entities")
																              .isEmpty()
												              )
										              )
								              )
										              .executes(getExistsConditionExecute(
												              positive,
												              context -> EntityArgumentType
														              .getOptionalEntities(context, "entities")
														              .size()
										              ))
						              )
				)
		)
				.then(
						CommandManager.literal("predicate")
						              .then(
								              addConditionLogic(
										              root,
										              CommandManager.argument(
												              "predicate",
												              RegistryEntryArgumentType.lootCondition(
														              commandRegistryAccess)
										              ),
										              positive,
										              context -> testLootCondition(
												              (ServerCommandSource) context.getSource(),
												              RegistryEntryArgumentType.getLootCondition(
														              context,
														              "predicate"
												              )
										              )
								              )
						              )
				)
		)
				.then(
						CommandManager.literal("function")
						              .then(
								              CommandManager
										              .argument("name", CommandFunctionArgumentType.commandFunction())
										              .suggests(FunctionCommand.SUGGESTION_PROVIDER)
										              .fork(root, new ExecuteCommand.IfUnlessRedirector(positive))
						              )
				)
		)
				.then(
						((LiteralArgumentBuilder) CommandManager.literal("items")
						                                        .then(
								                                        CommandManager.literal("entity")
								                                                      .then(
										                                                      CommandManager
												                                                      .argument(
														                                                      "entities",
														                                                      EntityArgumentType.entities()
												                                                      )
												                                                      .then(
														                                                      CommandManager
																                                                      .argument(
																		                                                      "slots",
																		                                                      SlotRangeArgumentType.slotRange()
																                                                      )
																                                                      .then(
																		                                                      ((RequiredArgumentBuilder) CommandManager
																				                                                      .argument(
																						                                                      "item_predicate",
																						                                                      ItemPredicateArgumentType.itemPredicate(
																								                                                      commandRegistryAccess)
																				                                                      )
																				                                                      .fork(
																						                                                      root,
																						                                                      context -> getSourceOrEmptyForConditionFork(
																								                                                      context,
																								                                                      positive,
																								                                                      countMatchingItems(
																										                                                      EntityArgumentType.getEntities(
																												                                                      context,
																												                                                      "entities"
																										                                                      ),
																										                                                      SlotRangeArgumentType.getSlotRange(
																												                                                      context,
																												                                                      "slots"
																										                                                      ),
																										                                                      ItemPredicateArgumentType.getItemStackPredicate(
																												                                                      context,
																												                                                      "item_predicate"
																										                                                      )
																								                                                      )
																										                                                      > 0
																						                                                      )
																				                                                      )
																		                                                      )
																				                                                      .executes(
																						                                                      getExistsConditionExecute(
																								                                                      positive,
																								                                                      context -> countMatchingItems(
																										                                                      EntityArgumentType.getEntities(
																												                                                      context,
																												                                                      "entities"
																										                                                      ),
																										                                                      SlotRangeArgumentType.getSlotRange(
																												                                                      context,
																												                                                      "slots"
																										                                                      ),
																										                                                      ItemPredicateArgumentType.getItemStackPredicate(
																												                                                      context,
																												                                                      "item_predicate"
																										                                                      )
																								                                                      )
																						                                                      )
																				                                                      )
																                                                      )
												                                                      )
								                                                      )
						                                        )
						)
								.then(
										CommandManager.literal("block")
										              .then(
												              CommandManager
														              .argument("pos", BlockPosArgumentType.blockPos())
														              .then(
																              CommandManager
																		              .argument(
																				              "slots",
																				              SlotRangeArgumentType.slotRange()
																		              )
																		              .then(
																				              ((RequiredArgumentBuilder) CommandManager
																						              .argument(
																								              "item_predicate",
																								              ItemPredicateArgumentType.itemPredicate(
																										              commandRegistryAccess)
																						              )
																						              .fork(
																								              root,
																								              context -> getSourceOrEmptyForConditionFork(
																										              context,
																										              positive,
																										              countMatchingItems(
																												              (ServerCommandSource) context.getSource(),
																												              BlockPosArgumentType.getLoadedBlockPos(
																														              context,
																														              "pos"
																												              ),
																												              SlotRangeArgumentType.getSlotRange(
																														              context,
																														              "slots"
																												              ),
																												              ItemPredicateArgumentType.getItemStackPredicate(
																														              context,
																														              "item_predicate"
																												              )
																										              )
																												              > 0
																								              )
																						              )
																				              )
																						              .executes(
																								              getExistsConditionExecute(
																										              positive,
																										              context -> countMatchingItems(
																												              (ServerCommandSource) context.getSource(),
																												              BlockPosArgumentType.getLoadedBlockPos(
																														              context,
																														              "pos"
																												              ),
																												              SlotRangeArgumentType.getSlotRange(
																														              context,
																														              "slots"
																												              ),
																												              ItemPredicateArgumentType.getItemStackPredicate(
																														              context,
																														              "item_predicate"
																												              )
																										              )
																								              )
																						              )
																		              )
														              )
										              )
								)
				)
		)
				.then(
						CommandManager.literal("stopwatch")
						              .then(
								              CommandManager.argument("id", IdentifierArgumentType.identifier())
								                            .suggests(StopwatchCommand.STOPWATCH_SUGGESTION_PROVIDER)
								                            .then(
										                            addConditionLogic(
												                            root,
												                            CommandManager.argument(
														                            "range",
														                            NumberRangeArgumentType.floatRange()
												                            ),
												                            positive,
												                            context -> testStopwatchRange(
														                            context,
														                            NumberRangeArgumentType.FloatRangeArgumentType.getRangeArgument(
																                            context,
																                            "range"
														                            )
												                            )
										                            )
								                            )
						              )
				);

		for (DataCommand.ObjectType objectType : DataCommand.SOURCE_OBJECT_TYPES) {
			argumentBuilder.then(
					objectType.addArgumentsToBuilder(
							CommandManager.literal("data"),
							builder -> builder.then(
									((RequiredArgumentBuilder) CommandManager
											.argument("path", NbtPathArgumentType.nbtPath())
											.fork(
													root,
													context -> getSourceOrEmptyForConditionFork(
															context,
															positive,
															countPathMatches(
																	objectType.getObject(context),
																	NbtPathArgumentType.getNbtPath(context, "path")
															) > 0
													)
											)
									)
											.executes(
													getExistsConditionExecute(
															positive,
															context -> countPathMatches(
																	objectType.getObject(context),
																	NbtPathArgumentType.getNbtPath(context, "path")
															)
													)
											)
							)
					)
			);
		}

		return argumentBuilder;
	}

	private static int countMatchingItems(
			Iterable<? extends StackReferenceGetter> entities,
			SlotRange slotRange,
			Predicate<ItemStack> predicate
	) {
		int i = 0;

		for (StackReferenceGetter stackReferenceGetter : entities) {
			IntList intList = slotRange.getSlotIds();

			for (int j = 0; j < intList.size(); j++) {
				int k = intList.getInt(j);
				StackReference stackReference = stackReferenceGetter.getStackReference(k);
				if (stackReference != null) {
					ItemStack itemStack = stackReference.get();
					if (predicate.test(itemStack)) {
						i += itemStack.getCount();
					}
				}
			}
		}

		return i;
	}

	private static int countMatchingItems(
			ServerCommandSource source,
			BlockPos pos,
			SlotRange slotRange,
			Predicate<ItemStack> predicate
	) throws CommandSyntaxException {
		int i = 0;
		Inventory inventory = ItemCommand.getInventoryAtPos(source, pos, ItemCommand.NOT_A_CONTAINER_SOURCE_EXCEPTION);
		int j = inventory.size();
		IntList intList = slotRange.getSlotIds();

		for (int k = 0; k < intList.size(); k++) {
			int l = intList.getInt(k);
			if (l >= 0 && l < j) {
				ItemStack itemStack = inventory.getStack(l);
				if (predicate.test(itemStack)) {
					i += itemStack.getCount();
				}
			}
		}

		return i;
	}

	private static Command<ServerCommandSource> getExistsConditionExecute(
			boolean positive,
			ExecuteCommand.ExistsCondition condition
	) {
		return positive ? context -> {
			int i = condition.test(context);
			if (i > 0) {
				((ServerCommandSource) context.getSource()).sendFeedback(
						() -> Text.translatable(
								"commands.execute.conditional.pass_count",
								i
						), false
				);
				return i;
			}
			else {
				throw CONDITIONAL_FAIL_EXCEPTION.create();
			}
		} : context -> {
			int i = condition.test(context);
			if (i == 0) {
				((ServerCommandSource) context.getSource()).sendFeedback(
						() -> Text.translatable("commands.execute.conditional.pass"), false);
				return 1;
			}
			else {
				throw CONDITIONAL_FAIL_COUNT_EXCEPTION.create(i);
			}
		};
	}

	private static int countPathMatches(DataCommandObject object, NbtPathArgumentType.NbtPath path)
	throws CommandSyntaxException {
		return path.count(object.getNbt());
	}

	private static boolean testScoreCondition(
			CommandContext<ServerCommandSource> context,
			ExecuteCommand.ScoreComparisonPredicate predicate
	) throws CommandSyntaxException {
		ScoreHolder scoreHolder = ScoreHolderArgumentType.getScoreHolder(context, "target");
		ScoreboardObjective
				scoreboardObjective =
				ScoreboardObjectiveArgumentType.getObjective(context, "targetObjective");
		ScoreHolder scoreHolder2 = ScoreHolderArgumentType.getScoreHolder(context, "source");
		ScoreboardObjective
				scoreboardObjective2 =
				ScoreboardObjectiveArgumentType.getObjective(context, "sourceObjective");
		Scoreboard scoreboard = ((ServerCommandSource) context.getSource()).getServer().getScoreboard();
		ReadableScoreboardScore readableScoreboardScore = scoreboard.getScore(scoreHolder, scoreboardObjective);
		ReadableScoreboardScore readableScoreboardScore2 = scoreboard.getScore(scoreHolder2, scoreboardObjective2);
		return readableScoreboardScore != null && readableScoreboardScore2 != null
		       ? predicate.test(readableScoreboardScore.getScore(), readableScoreboardScore2.getScore())
		       : false;
	}

	private static boolean testScoreMatch(CommandContext<ServerCommandSource> context, NumberRange.IntRange range)
	throws CommandSyntaxException {
		ScoreHolder scoreHolder = ScoreHolderArgumentType.getScoreHolder(context, "target");
		ScoreboardObjective
				scoreboardObjective =
				ScoreboardObjectiveArgumentType.getObjective(context, "targetObjective");
		Scoreboard scoreboard = ((ServerCommandSource) context.getSource()).getServer().getScoreboard();
		ReadableScoreboardScore readableScoreboardScore = scoreboard.getScore(scoreHolder, scoreboardObjective);
		return readableScoreboardScore == null ? false : range.test(readableScoreboardScore.getScore());
	}

	private static boolean testStopwatchRange(
			CommandContext<ServerCommandSource> context,
			NumberRange.DoubleRange range
	) throws CommandSyntaxException {
		Identifier identifier = IdentifierArgumentType.getIdentifier(context, "id");
		StopwatchPersistentState
				stopwatchPersistentState =
				((ServerCommandSource) context.getSource()).getServer().getStopwatchPersistentState();
		Stopwatch stopwatch = stopwatchPersistentState.get(identifier);
		if (stopwatch == null) {
			throw StopwatchCommand.DOES_NOT_EXIST_EXCEPTION.create(identifier);
		}
		else {
			long l = StopwatchPersistentState.getTimeMs();
			double d = stopwatch.getElapsedTimeSeconds(l);
			return range.test(d);
		}
	}

	private static boolean testLootCondition(ServerCommandSource source, RegistryEntry<LootCondition> lootCondition) {
		ServerWorld serverWorld = source.getWorld();
		LootWorldContext lootWorldContext = new LootWorldContext.Builder(serverWorld)
				.add(LootContextParameters.ORIGIN, source.getPosition())
				.addOptional(LootContextParameters.THIS_ENTITY, source.getEntity())
				.build(LootContextTypes.COMMAND);
		LootContext lootContext = new LootContext.Builder(lootWorldContext).build(Optional.empty());
		lootContext.markActive(LootContext.predicate(lootCondition.value()));
		return lootCondition.value().test(lootContext);
	}

	private static Collection<ServerCommandSource> getSourceOrEmptyForConditionFork(
			CommandContext<ServerCommandSource> context,
			boolean positive,
			boolean value
	) {
		return (Collection<ServerCommandSource>) (value == positive
		                                          ? Collections.singleton((ServerCommandSource) context.getSource())
		                                          : Collections.emptyList()
		);
	}

	private static ArgumentBuilder<ServerCommandSource, ?> addConditionLogic(
			CommandNode<ServerCommandSource> root,
			ArgumentBuilder<ServerCommandSource, ?> builder,
			boolean positive,
			ExecuteCommand.Condition condition
	) {
		return builder
				.fork(root, context -> getSourceOrEmptyForConditionFork(context, positive, condition.test(context)))
				.executes(context -> {
					if (positive == condition.test(context)) {
						((ServerCommandSource) context.getSource()).sendFeedback(
								() -> Text.translatable("commands.execute.conditional.pass"), false);
						return 1;
					}
					else {
						throw CONDITIONAL_FAIL_EXCEPTION.create();
					}
				});
	}

	private static ArgumentBuilder<ServerCommandSource, ?> addBlocksConditionLogic(
			CommandNode<ServerCommandSource> root,
			ArgumentBuilder<ServerCommandSource, ?> builder,
			boolean positive,
			boolean masked
	) {
		return builder
				.fork(
						root,
						context -> getSourceOrEmptyForConditionFork(
								context,
								positive,
								testBlocksCondition(context, masked).isPresent()
						)
				)
				.executes(positive ? context -> executePositiveBlockCondition(context, masked)
				                   : context -> executeNegativeBlockCondition(context, masked));
	}

	private static int executePositiveBlockCondition(CommandContext<ServerCommandSource> context, boolean masked)
	throws CommandSyntaxException {
		OptionalInt optionalInt = testBlocksCondition(context, masked);
		if (optionalInt.isPresent()) {
			((ServerCommandSource) context.getSource())
					.sendFeedback(
							() -> Text.translatable(
									"commands.execute.conditional.pass_count",
									optionalInt.getAsInt()
							), false
					);
			return optionalInt.getAsInt();
		}
		else {
			throw CONDITIONAL_FAIL_EXCEPTION.create();
		}
	}

	private static int executeNegativeBlockCondition(CommandContext<ServerCommandSource> context, boolean masked)
	throws CommandSyntaxException {
		OptionalInt optionalInt = testBlocksCondition(context, masked);
		if (optionalInt.isPresent()) {
			throw CONDITIONAL_FAIL_COUNT_EXCEPTION.create(optionalInt.getAsInt());
		}
		else {
			((ServerCommandSource) context.getSource()).sendFeedback(
					() -> Text.translatable("commands.execute.conditional.pass"), false);
			return 1;
		}
	}

	private static OptionalInt testBlocksCondition(CommandContext<ServerCommandSource> context, boolean masked)
	throws CommandSyntaxException {
		return testBlocksCondition(
				((ServerCommandSource) context.getSource()).getWorld(),
				BlockPosArgumentType.getLoadedBlockPos(context, "start"),
				BlockPosArgumentType.getLoadedBlockPos(context, "end"),
				BlockPosArgumentType.getLoadedBlockPos(context, "destination"),
				masked
		);
	}

	private static OptionalInt testBlocksCondition(
			ServerWorld world,
			BlockPos start,
			BlockPos end,
			BlockPos destination,
			boolean masked
	) throws CommandSyntaxException {
		BlockBox blockBox = BlockBox.create(start, end);
		BlockBox blockBox2 = BlockBox.create(destination, destination.add(blockBox.getDimensions()));
		BlockPos blockPos = new BlockPos(
				blockBox2.getMinX() - blockBox.getMinX(),
				blockBox2.getMinY() - blockBox.getMinY(),
				blockBox2.getMinZ() - blockBox.getMinZ()
		);
		int i = blockBox.getBlockCountX() * blockBox.getBlockCountY() * blockBox.getBlockCountZ();
		if (i > MAX_BLOCKS) {
			throw BLOCKS_TOOBIG_EXCEPTION.create(MAX_BLOCKS, i);
		}
		else {
			int j = 0;
			DynamicRegistryManager dynamicRegistryManager = world.getRegistryManager();

			try (ErrorReporter.Logging logging = new ErrorReporter.Logging(LOGGER)) {
				for (int k = blockBox.getMinZ(); k <= blockBox.getMaxZ(); k++) {
					for (int l = blockBox.getMinY(); l <= blockBox.getMaxY(); l++) {
						for (int m = blockBox.getMinX(); m <= blockBox.getMaxX(); m++) {
							BlockPos blockPos2 = new BlockPos(m, l, k);
							BlockPos blockPos3 = blockPos2.add(blockPos);
							BlockState blockState = world.getBlockState(blockPos2);
							if (!masked || !blockState.isOf(Blocks.AIR)) {
								if (blockState != world.getBlockState(blockPos3)) {
									return OptionalInt.empty();
								}

								BlockEntity blockEntity = world.getBlockEntity(blockPos2);
								BlockEntity blockEntity2 = world.getBlockEntity(blockPos3);
								if (blockEntity != null) {
									if (blockEntity2 == null) {
										return OptionalInt.empty();
									}

									if (blockEntity2.getType() != blockEntity.getType()) {
										return OptionalInt.empty();
									}

									if (!blockEntity.getComponents().equals(blockEntity2.getComponents())) {
										return OptionalInt.empty();
									}

									NbtWriteView
											nbtWriteView =
											NbtWriteView.create(
													logging.makeChild(blockEntity.getReporterContext()),
													dynamicRegistryManager
											);
									blockEntity.writeComponentlessData(nbtWriteView);
									NbtCompound nbtCompound = nbtWriteView.getNbt();
									NbtWriteView
											nbtWriteView2 =
											NbtWriteView.create(
													logging.makeChild(blockEntity2.getReporterContext()),
													dynamicRegistryManager
											);
									blockEntity2.writeComponentlessData(nbtWriteView2);
									NbtCompound nbtCompound2 = nbtWriteView2.getNbt();
									if (!nbtCompound.equals(nbtCompound2)) {
										return OptionalInt.empty();
									}
								}

								j++;
							}
						}
					}
				}
			}

			return OptionalInt.of(j);
		}
	}

	private static RedirectModifier<ServerCommandSource> createEntityModifier(Function<Entity, Optional<Entity>> function) {
		return context -> {
			ServerCommandSource serverCommandSource = (ServerCommandSource) context.getSource();
			Entity entity = serverCommandSource.getEntity();
			return (Collection) (entity == null
			                     ? List.of()
			                     : function
			                       .apply(entity)
			                       .filter(entityx -> !entityx.isRemoved())
			                       .map(entityx -> List.of(serverCommandSource.withEntity(entityx)))
			                       .orElse(List.of())
			);
		};
	}

	private static RedirectModifier<ServerCommandSource> createMultiEntityModifier(Function<Entity, Stream<Entity>> function) {
		return context -> {
			ServerCommandSource serverCommandSource = (ServerCommandSource) context.getSource();
			Entity entity = serverCommandSource.getEntity();
			return entity == null ? List.of() : function
			                                    .apply(entity)
			                                    .filter(entityx -> !entityx.isRemoved())
			                                    .map(serverCommandSource::withEntity)
			                                    .toList();
		};
	}

	private static LiteralArgumentBuilder<ServerCommandSource> addOnArguments(
			CommandNode<ServerCommandSource> node, LiteralArgumentBuilder<ServerCommandSource> builder
	) {
		return (LiteralArgumentBuilder<ServerCommandSource>) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) builder.then(
				CommandManager.literal("owner")
				              .fork(
						              node,
						              createEntityModifier(
								              entity -> entity instanceof Tameable tameable ? Optional.ofNullable(
										              tameable.getOwner()) : Optional.empty()
						              )
				              )
		)
		)
				.then(
						CommandManager.literal("leasher")
						              .fork(
								              node,
								              createEntityModifier(
										              entity -> entity instanceof Leashable leashable
										                        ? Optional.ofNullable(leashable.getLeashHolder())
										                        : Optional.empty()
								              )
						              )
				)
		)
				.then(
						CommandManager.literal("target")
						              .fork(
								              node,
								              createEntityModifier(
										              entity -> entity instanceof Targeter targeter
										                        ? Optional.ofNullable(targeter.getTarget())
										                        : Optional.empty()
								              )
						              )
				)
		)
				.then(
						CommandManager.literal("attacker")
						              .fork(
								              node,
								              createEntityModifier(
										              entity -> entity instanceof Attackable attackable
										                        ? Optional.ofNullable(attackable.getLastAttacker())
										                        : Optional.empty()
								              )
						              )
				)
		)
				.then(CommandManager
						.literal("vehicle")
						.fork(node, createEntityModifier(entity -> Optional.ofNullable(entity.getVehicle()))))
		)
				.then(CommandManager
						.literal("controller")
						.fork(
								node,
								createEntityModifier(entity -> Optional.ofNullable(entity.getControllingPassenger()))
						))
		)
				.then(
						CommandManager.literal("origin")
						              .fork(
								              node,
								              createEntityModifier(entity -> entity instanceof Ownable ownable
								                                             ? Optional.ofNullable(ownable.getOwner())
								                                             : Optional.empty())
						              )
				)
		)
				.then(CommandManager
						.literal("passengers")
						.fork(node, createMultiEntityModifier(entity -> entity.getPassengerList().stream())));
	}

	private static ServerCommandSource summon(
			ServerCommandSource source,
			RegistryEntry.Reference<EntityType<?>> entityType
	) throws CommandSyntaxException {
		Entity entity = SummonCommand.summon(source, entityType, source.getPosition(), new NbtCompound(), true);
		return source.withEntity(entity);
	}

	public static <T extends AbstractServerCommandSource<T>> void enqueueExecutions(
			T baseSource,
			List<T> sources,
			Function<T, T> functionSourceGetter,
			IntPredicate predicate,
			ContextChain<T> contextChain,
			@Nullable NbtCompound args,
			ExecutionControl<T> control,
			ArgumentGetter<CommandContext<T>, Collection<CommandFunction<T>>> functionNamesGetter,
			ExecutionFlags flags
	) {
		List<T> list = new ArrayList<>(sources.size());

		Collection<CommandFunction<T>> collection;
		try {
			collection = functionNamesGetter.apply(contextChain.getTopContext().copyFor(baseSource));
		}
		catch (CommandSyntaxException var18) {
			baseSource.handleException(var18, flags.isSilent(), control.getTracer());
			return;
		}

		int i = collection.size();
		if (i != 0) {
			List<Procedure<T>> list2 = new ArrayList<>(i);

			try {
				for (CommandFunction<T> commandFunction : collection) {
					try {
						list2.add(commandFunction.withMacroReplaced(args, baseSource.getDispatcher()));
					}
					catch (MacroException var17) {
						throw INSTANTIATION_FAILURE_EXCEPTION.create(commandFunction.id(), var17.getMessage());
					}
				}
			}
			catch (CommandSyntaxException var19) {
				baseSource.handleException(var19, flags.isSilent(), control.getTracer());
			}

			for (T abstractServerCommandSource : sources) {
				T
						abstractServerCommandSource2 =
						(T) functionSourceGetter.apply(abstractServerCommandSource.withDummyReturnValueConsumer());
				ReturnValueConsumer returnValueConsumer = (successful, returnValue) -> {
					if (predicate.test(returnValue)) {
						list.add(abstractServerCommandSource);
					}
				};
				control.enqueueAction(
						new IsolatedCommandAction<>(
								newControl -> {
									for (Procedure<T> procedure : list2) {
										newControl.enqueueAction(
												new CommandFunctionAction<>(
														procedure,
														newControl.getFrame().returnValueConsumer(),
														true
												).bind(abstractServerCommandSource2)
										);
									}

									newControl.enqueueAction(FallthroughCommandAction.getInstance());
								},
								returnValueConsumer
						)
				);
			}

			ContextChain<T> contextChain2 = contextChain.nextStage();
			String string = contextChain.getTopContext().getInput();
			control.enqueueAction(new SingleCommandAction.MultiSource<>(
					string,
					contextChain2,
					flags,
					baseSource,
					list
			));
		}
	}

	@FunctionalInterface
	interface Condition {

		boolean test(CommandContext<ServerCommandSource> context) throws CommandSyntaxException;
	}

	@FunctionalInterface
	interface ExistsCondition {

		int test(CommandContext<ServerCommandSource> context) throws CommandSyntaxException;
	}

	static class IfUnlessRedirector implements Forkable.RedirectModifier<ServerCommandSource> {

		private final IntPredicate predicate;

		IfUnlessRedirector(boolean success) {
			this.predicate = success ? result -> result != 0 : result -> result == 0;
		}

		public void execute(
				ServerCommandSource serverCommandSource,
				List<ServerCommandSource> list,
				ContextChain<ServerCommandSource> contextChain,
				ExecutionFlags executionFlags,
				ExecutionControl<ServerCommandSource> executionControl
		) {
			ExecuteCommand.enqueueExecutions(
					serverCommandSource,
					list,
					FunctionCommand::createFunctionCommandSource,
					this.predicate,
					contextChain,
					null,
					executionControl,
					context -> CommandFunctionArgumentType.getFunctions(context, "name"),
					executionFlags
			);
		}
	}

	@FunctionalInterface
	interface ScoreComparisonPredicate {

		boolean test(int targetScore, int sourceScore);
	}
}
