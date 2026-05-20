package net.minecraft.server.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
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

public class FillCommand {
   private static final Dynamic2CommandExceptionType TOO_BIG_EXCEPTION = new Dynamic2CommandExceptionType(
      (maxCount, count) -> Text.stringifiedTranslatable("commands.fill.toobig", maxCount, count)
   );
   static final BlockStateArgument AIR_BLOCK_ARGUMENT = new BlockStateArgument(Blocks.AIR.getDefaultState(), Collections.emptySet(), null);
   private static final SimpleCommandExceptionType FAILED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.fill.failed"));

   public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("fill")
               .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK)))
            .then(
               CommandManager.argument("from", BlockPosArgumentType.blockPos())
                  .then(
                     CommandManager.argument("to", BlockPosArgumentType.blockPos())
                        .then(
                           buildModeTree(
                                 commandRegistryAccess,
                                 CommandManager.argument("block", BlockStateArgumentType.blockState(commandRegistryAccess)),
                                 context -> BlockPosArgumentType.getLoadedBlockPos(context, "from"),
                                 context -> BlockPosArgumentType.getLoadedBlockPos(context, "to"),
                                 context -> BlockStateArgumentType.getBlockState(context, "block"),
                                 context -> null
                              )
                              .then(
                                 ((LiteralArgumentBuilder)CommandManager.literal("replace")
                                       .executes(
                                          context -> execute(
                                             (ServerCommandSource)context.getSource(),
                                             BlockBox.create(
                                                BlockPosArgumentType.getLoadedBlockPos(context, "from"), BlockPosArgumentType.getLoadedBlockPos(context, "to")
                                             ),
                                             BlockStateArgumentType.getBlockState(context, "block"),
                                             FillCommand.Mode.REPLACE,
                                             null,
                                             false
                                          )
                                       ))
                                    .then(
                                       buildModeTree(
                                          commandRegistryAccess,
                                          CommandManager.argument("filter", BlockPredicateArgumentType.blockPredicate(commandRegistryAccess)),
                                          context -> BlockPosArgumentType.getLoadedBlockPos(context, "from"),
                                          context -> BlockPosArgumentType.getLoadedBlockPos(context, "to"),
                                          context -> BlockStateArgumentType.getBlockState(context, "block"),
                                          context -> BlockPredicateArgumentType.getBlockPredicate(context, "filter")
                                       )
                                    )
                              )
                              .then(
                                 CommandManager.literal("keep")
                                    .executes(
                                       context -> execute(
                                          (ServerCommandSource)context.getSource(),
                                          BlockBox.create(
                                             BlockPosArgumentType.getLoadedBlockPos(context, "from"), BlockPosArgumentType.getLoadedBlockPos(context, "to")
                                          ),
                                          BlockStateArgumentType.getBlockState(context, "block"),
                                          FillCommand.Mode.REPLACE,
                                          pos -> pos.getWorld().isAir(pos.getBlockPos()),
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
               (ServerCommandSource)context.getSource(),
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
                     (ServerCommandSource)context.getSource(),
                     BlockBox.create(from.apply(context), to.apply(context)),
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
                     (ServerCommandSource)context.getSource(),
                     BlockBox.create(from.apply(context), to.apply(context)),
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
                     (ServerCommandSource)context.getSource(),
                     BlockBox.create(from.apply(context), to.apply(context)),
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
                     (ServerCommandSource)context.getSource(),
                     BlockBox.create(from.apply(context), to.apply(context)),
                     state.apply(context),
                     FillCommand.Mode.REPLACE,
                     filter.apply(context),
                     true
                  )
               )
         );
   }

   private static int execute(
      ServerCommandSource source,
      BlockBox range,
      BlockStateArgument block,
      FillCommand.Mode mode,
      @Nullable Predicate<CachedBlockPosition> filter,
      boolean strict
   ) throws CommandSyntaxException {
      int i = range.getBlockCountX() * range.getBlockCountY() * range.getBlockCountZ();
      int j = source.getWorld().getGameRules().getValue(GameRules.MAX_BLOCK_MODIFICATIONS);
      if (i > j) {
         throw TOO_BIG_EXCEPTION.create(j, i);
      } else {
         record Replaced(BlockPos pos, BlockState oldState) {
         }

         List<Replaced> list = Lists.newArrayList();
         ServerWorld serverWorld = source.getWorld();
         if (serverWorld.isDebugWorld()) {
            throw FAILED_EXCEPTION.create();
         } else {
            int k = 0;

            for (BlockPos blockPos : BlockPos.iterate(range.getMinX(), range.getMinY(), range.getMinZ(), range.getMaxX(), range.getMaxY(), range.getMaxZ())) {
               if (filter == null || filter.test(new CachedBlockPosition(serverWorld, blockPos, true))) {
                  BlockState blockState = serverWorld.getBlockState(blockPos);
                  boolean bl = false;
                  if (mode.postProcessor.affect(serverWorld, blockPos)) {
                     bl = true;
                  }

                  BlockStateArgument blockStateArgument = mode.filter.filter(range, blockPos, block, serverWorld);
                  if (blockStateArgument == null) {
                     if (bl) {
                        k++;
                     }
                  } else if (!blockStateArgument.setBlockState(serverWorld, blockPos, 2 | (strict ? 816 : 256))) {
                     if (bl) {
                        k++;
                     }
                  } else {
                     if (!strict) {
                        list.add(new Replaced(blockPos.toImmutable(), blockState));
                     }

                     k++;
                  }
               }
            }

            for (Replaced replaced : list) {
               serverWorld.onStateReplacedWithCommands(replaced.pos, replaced.oldState);
            }

            if (k == 0) {
               throw FAILED_EXCEPTION.create();
            } else {
               int l = k;
               source.sendFeedback(() -> Text.translatable("commands.fill.success", l), true);
               return k;
            }
         }
      }
   }

   @FunctionalInterface
   public interface Filter {
      FillCommand.Filter IDENTITY = (box, pos, block, world) -> block;

      @Nullable BlockStateArgument filter(BlockBox box, BlockPos pos, BlockStateArgument block, ServerWorld world);
   }

   static enum Mode {
      REPLACE(FillCommand.PostProcessor.EMPTY, FillCommand.Filter.IDENTITY),
      OUTLINE(
         FillCommand.PostProcessor.EMPTY,
         (range, pos, block, world) -> pos.getX() != range.getMinX()
               && pos.getX() != range.getMaxX()
               && pos.getY() != range.getMinY()
               && pos.getY() != range.getMaxY()
               && pos.getZ() != range.getMinZ()
               && pos.getZ() != range.getMaxZ()
            ? null
            : block
      ),
      HOLLOW(
         FillCommand.PostProcessor.EMPTY,
         (range, pos, block, world) -> pos.getX() != range.getMinX()
               && pos.getX() != range.getMaxX()
               && pos.getY() != range.getMinY()
               && pos.getY() != range.getMaxY()
               && pos.getZ() != range.getMinZ()
               && pos.getZ() != range.getMaxZ()
            ? FillCommand.AIR_BLOCK_ARGUMENT
            : block
      ),
      DESTROY((world, pos) -> world.breakBlock(pos, true), FillCommand.Filter.IDENTITY);

      public final FillCommand.Filter filter;
      public final FillCommand.PostProcessor postProcessor;

      private Mode(final FillCommand.PostProcessor postProcessor, final FillCommand.Filter filter) {
         this.postProcessor = postProcessor;
         this.filter = filter;
      }
   }

   @FunctionalInterface
   interface OptionalArgumentResolver<T, R> {
      @Nullable R apply(T object) throws CommandSyntaxException;
   }

   @FunctionalInterface
   public interface PostProcessor {
      FillCommand.PostProcessor EMPTY = (world, pos) -> false;

      boolean affect(ServerWorld world, BlockPos pos);
   }
}
