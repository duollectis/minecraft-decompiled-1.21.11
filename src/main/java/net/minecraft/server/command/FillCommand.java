package net.minecraft.server.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.ArgumentGetter;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Реализация команды {@code /fill} — заполнение прямоугольной области блоками.
 * <p>
 * Поддерживает режимы: {@code replace}, {@code outline}, {@code hollow}, {@code destroy}, {@code strict}.
 * Ограничение на максимальный объём задаётся правилом игры {@code maxCommandChainLength}.
 */
public class FillCommand {

	/**
	 * Флаг обновления блока без уведомления соседей (стандартный режим).
	 * Значение 2 соответствует {@code Block.NOTIFY_LISTENERS}.
	 */
	private static final int FLAG_NOTIFY = 2;

	/**
	 * Дополнительные флаги для строгого режима: подавление обновлений соседей и рендера.
	 * Значение 816 = NOTIFY_LISTENERS | SKIP_DROPS | FORCE_STATE | SKIP_LIGHTING_UPDATES.
	 */
	private static final int FLAG_STRICT_EXTRA = 816;

	/**
	 * Флаг для обычного режима: обновление соседей без лишних уведомлений.
	 * Значение 256 = SKIP_DROPS.
	 */
	private static final int FLAG_NORMAL_EXTRA = 256;

	private static final Dynamic2CommandExceptionType TOO_BIG_EXCEPTION = new Dynamic2CommandExceptionType(
			(maxCount, count) -> Text.stringifiedTranslatable("commands.fill.toobig", maxCount, count)
	);
	static final BlockStateArgument AIR_BLOCK_ARGUMENT =
			new BlockStateArgument(Blocks.AIR.getDefaultState(), Collections.emptySet(), null);
	private static final SimpleCommandExceptionType FAILED_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("commands.fill.failed"));

	public static void register(
			CommandDispatcher<ServerCommandSource> dispatcher,
			CommandRegistryAccess commandRegistryAccess
	) {
		dispatcher.register(
				(LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager.literal("fill")
				                                                                 .requires(CommandManager.requirePermissionLevel(
						                                                                 CommandManager.GAMEMASTERS_CHECK))
				)
						.then(
								CommandManager.argument("from", BlockPosArgumentType.blockPos())
								              .then(
										              CommandManager.argument("to", BlockPosArgumentType.blockPos())
										                            .then(
												                            buildModeTree(
														                            commandRegistryAccess,
														                            CommandManager.argument(
																                            "block",
																                            BlockStateArgumentType.blockState(
																		                            commandRegistryAccess)
														                            ),
														                            context -> BlockPosArgumentType.getLoadedBlockPos(
																                            context,
																                            "from"
														                            ),
														                            context -> BlockPosArgumentType.getLoadedBlockPos(
																                            context,
																                            "to"
														                            ),
														                            context -> BlockStateArgumentType.getBlockState(
																                            context,
																                            "block"
														                            ),
														                            context -> null
												                            )
														                            .then(
																                            ((LiteralArgumentBuilder) CommandManager
																		                            .literal("replace")
																		                            .executes(
																				                            context -> execute(
																						                            (ServerCommandSource) context.getSource(),
																						                            BlockBox.create(
																								                            BlockPosArgumentType.getLoadedBlockPos(
																										                            context,
																										                            "from"
																								                            ),
																								                            BlockPosArgumentType.getLoadedBlockPos(
																										                            context,
																										                            "to"
																								                            )
																						                            ),
																						                            BlockStateArgumentType.getBlockState(
																								                            context,
																								                            "block"
																						                            ),
																						                            FillCommand.Mode.REPLACE,
																						                            null,
																						                            false
																				                            )
																		                            )
																                            )
																		                            .then(
																				                            buildModeTree(
																						                            commandRegistryAccess,
																						                            CommandManager.argument(
																								                            "filter",
																								                            BlockPredicateArgumentType.blockPredicate(
																										                            commandRegistryAccess)
																						                            ),
																						                            context -> BlockPosArgumentType.getLoadedBlockPos(
																								                            context,
																								                            "from"
																						                            ),
																						                            context -> BlockPosArgumentType.getLoadedBlockPos(
																								                            context,
																								                            "to"
																						                            ),
																						                            context -> BlockStateArgumentType.getBlockState(
																								                            context,
																								                            "block"
																						                            ),
																						                            context -> BlockPredicateArgumentType.getBlockPredicate(
																								                            context,
																								                            "filter"
																						                            )
																				                            )
																		                            )
														                            )
														                            .then(
																                            CommandManager
																		                            .literal("keep")
																		                            .executes(
																				                            context -> execute(
																						                            (ServerCommandSource) context.getSource(),
																						                            BlockBox.create(
																								                            BlockPosArgumentType.getLoadedBlockPos(
																										                            context,
																										                            "from"
																								                            ),
																								                            BlockPosArgumentType.getLoadedBlockPos(
																										                            context,
																										                            "to"
																								                            )
																						                            ),
																						                            BlockStateArgumentType.getBlockState(
																								                            context,
																								                            "block"
																						                            ),
																						                            FillCommand.Mode.REPLACE,
																						                            pos -> pos
																								                            .getWorld()
																								                            .isAir(pos.getBlockPos()),
																						                            false
																				                            )
																		                            )
														                            )
										                            )
								              )
						)
		);
	}

	private static ArgumentBuilder<ServerCommandSource, ?> buildModeTree(
			CommandRegistryAccess registries,
			ArgumentBuilder<ServerCommandSource, ?> argumentBuilder,
			ArgumentGetter<CommandContext<ServerCommandSource>, BlockPos> from,
			ArgumentGetter<CommandContext<ServerCommandSource>, BlockPos> to,
			ArgumentGetter<CommandContext<ServerCommandSource>, BlockStateArgument> state,
			FillCommand.OptionalArgumentResolver<CommandContext<ServerCommandSource>, Predicate<CachedBlockPosition>> filter
	) {
		return argumentBuilder.executes(
				                      context -> execute(
						                      (ServerCommandSource) context.getSource(),
						                      BlockBox.create(from.apply(context), to.apply(context)),
						                      state.apply(context),
						                      FillCommand.Mode.REPLACE,
						                      filter.apply(context),
						                      false
				                      )
		                      )
		                      .then(
				                      CommandManager.literal("outline")
				                                    .executes(
						                                    context -> execute(
								                                    (ServerCommandSource) context.getSource(),
								                                    BlockBox.create(
										                                    from.apply(context),
										                                    to.apply(context)
								                                    ),
								                                    state.apply(context),
								                                    FillCommand.Mode.OUTLINE,
								                                    filter.apply(context),
								                                    false
						                                    )
				                                    )
		                      )
		                      .then(
				                      CommandManager.literal("hollow")
				                                    .executes(
						                                    context -> execute(
								                                    (ServerCommandSource) context.getSource(),
								                                    BlockBox.create(
										                                    from.apply(context),
										                                    to.apply(context)
								                                    ),
								                                    state.apply(context),
								                                    FillCommand.Mode.HOLLOW,
								                                    filter.apply(context),
								                                    false
						                                    )
				                                    )
		                      )
		                      .then(
				                      CommandManager.literal("destroy")
				                                    .executes(
						                                    context -> execute(
								                                    (ServerCommandSource) context.getSource(),
								                                    BlockBox.create(
										                                    from.apply(context),
										                                    to.apply(context)
								                                    ),
								                                    state.apply(context),
								                                    FillCommand.Mode.DESTROY,
								                                    filter.apply(context),
								                                    false
						                                    )
				                                    )
		                      )
		                      .then(
				                      CommandManager.literal("strict")
				                                    .executes(
						                                    context -> execute(
								                                    (ServerCommandSource) context.getSource(),
								                                    BlockBox.create(
										                                    from.apply(context),
										                                    to.apply(context)
								                                    ),
								                                    state.apply(context),
								                                    FillCommand.Mode.REPLACE,
								                                    filter.apply(context),
								                                    true
						                                    )
				                                    )
		                      );
	}

	/**
	 * Выполняет заполнение области блоками с учётом режима и фильтра.
	 * <p>
	 * После заполнения вызывает {@code onStateReplacedWithCommands} для каждого
	 * изменённого блока (кроме строгого режима, где обновления соседей подавлены).
	 *
	 * @param source  источник команды (игрок или командный блок)
	 * @param range   ограничивающий прямоугольник области заполнения
	 * @param block   целевое состояние блока
	 * @param mode    режим заполнения (replace, outline, hollow, destroy)
	 * @param filter  опциональный предикат фильтрации позиций
	 * @param strict  если {@code true}, подавляет обновления соседей и рендера
	 * @return количество изменённых блоков
	 */
	private static int execute(
			ServerCommandSource source,
			BlockBox range,
			BlockStateArgument block,
			FillCommand.Mode mode,
			@Nullable Predicate<CachedBlockPosition> filter,
			boolean strict
	) throws CommandSyntaxException {
		int volume = range.getBlockCountX() * range.getBlockCountY() * range.getBlockCountZ();
		int maxModifications = source.getWorld().getGameRules().getValue(GameRules.MAX_BLOCK_MODIFICATIONS);

		if (volume > maxModifications) {
			throw TOO_BIG_EXCEPTION.create(maxModifications, volume);
		}

		record Replaced(BlockPos pos, BlockState oldState) {}

		List<Replaced> replaced = Lists.newArrayList();
		ServerWorld world = source.getWorld();

		if (world.isDebugWorld()) {
			throw FAILED_EXCEPTION.create();
		}

		int changedCount = 0;

		for (BlockPos pos : BlockPos.iterate(
				range.getMinX(),
				range.getMinY(),
				range.getMinZ(),
				range.getMaxX(),
				range.getMaxY(),
				range.getMaxZ()
		)) {
			if (filter != null && !filter.test(new CachedBlockPosition(world, pos, true))) {
				continue;
			}

			BlockState oldState = world.getBlockState(pos);
			boolean postProcessed = mode.postProcessor.affect(world, pos);
			BlockStateArgument filtered = mode.filter.filter(range, pos, block, world);

			if (filtered == null) {
				if (postProcessed) {
					changedCount++;
				}

				continue;
			}

			int flags = FLAG_NOTIFY | (strict ? FLAG_STRICT_EXTRA : FLAG_NORMAL_EXTRA);

			if (!filtered.setBlockState(world, pos, flags)) {
				if (postProcessed) {
					changedCount++;
				}

				continue;
			}

			if (!strict) {
				replaced.add(new Replaced(pos.toImmutable(), oldState));
			}

			changedCount++;
		}

		for (Replaced entry : replaced) {
			world.onStateReplacedWithCommands(entry.pos(), entry.oldState());
		}

		if (changedCount == 0) {
			throw FAILED_EXCEPTION.create();
		}

		int finalCount = changedCount;
		source.sendFeedback(() -> Text.translatable("commands.fill.success", finalCount), true);
		return changedCount;
	}

	/**
	 * Фильтр блоков при заполнении — определяет, какой блок будет установлен в позицию.
	 * Возвращает {@code null}, если блок в данной позиции не должен быть изменён.
	 */
	@FunctionalInterface
	public interface Filter {

		Filter IDENTITY = (box, pos, block, world) -> block;

		@Nullable BlockStateArgument filter(BlockBox box, BlockPos pos, BlockStateArgument block, ServerWorld world);
	}

	/**
	 * Режим заполнения области командой {@code /fill}.
	 * Определяет, какие позиции затрагиваются и как обрабатываются блоки.
	 */
	enum Mode {
		/** Заменяет все блоки в области. */
		REPLACE(PostProcessor.EMPTY, Filter.IDENTITY),

		/** Заменяет только блоки на границе области. */
		OUTLINE(
				PostProcessor.EMPTY,
				(range, pos, block, world) -> pos.getX() != range.getMinX()
						&& pos.getX() != range.getMaxX()
						&& pos.getY() != range.getMinY()
						&& pos.getY() != range.getMaxY()
						&& pos.getZ() != range.getMinZ()
						&& pos.getZ() != range.getMaxZ()
						? null
						: block
		),

		/** Заменяет блоки на границе, внутренность заполняет воздухом. */
		HOLLOW(
				PostProcessor.EMPTY,
				(range, pos, block, world) -> pos.getX() != range.getMinX()
						&& pos.getX() != range.getMaxX()
						&& pos.getY() != range.getMinY()
						&& pos.getY() != range.getMaxY()
						&& pos.getZ() != range.getMinZ()
						&& pos.getZ() != range.getMaxZ()
						? AIR_BLOCK_ARGUMENT
						: block
		),

		/** Разрушает блоки с дропом предметов. */
		DESTROY((world, pos) -> world.breakBlock(pos, true), Filter.IDENTITY);

		public final Filter filter;
		public final PostProcessor postProcessor;

		Mode(PostProcessor postProcessor, Filter filter) {
			this.postProcessor = postProcessor;
			this.filter = filter;
		}
	}

	@FunctionalInterface
	interface OptionalArgumentResolver<T, R> {

		@Nullable R apply(T object) throws CommandSyntaxException;
	}

	/**
	 * Пост-процессор, вызываемый перед установкой блока.
	 * Используется, например, для разрушения блока с дропом в режиме {@code destroy}.
	 */
	@FunctionalInterface
	public interface PostProcessor {

		PostProcessor EMPTY = (world, pos) -> false;

		boolean affect(ServerWorld world, BlockPos pos);
	}
}
