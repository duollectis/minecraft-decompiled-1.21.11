package net.minecraft.server.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.ArgumentGetter;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.component.ComponentMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class CloneCommand {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final SimpleCommandExceptionType OVERLAP_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.clone.overlap"));
   private static final Dynamic2CommandExceptionType TOO_BIG_EXCEPTION = new Dynamic2CommandExceptionType(
      (maxCount, count) -> Text.stringifiedTranslatable("commands.clone.toobig", maxCount, count)
   );
   private static final SimpleCommandExceptionType FAILED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.clone.failed"));
   public static final Predicate<CachedBlockPosition> IS_AIR_PREDICATE = pos -> !pos.getBlockState().isAir();

   public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("clone")
                  .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK)))
               .then(createSourceArgs(commandRegistryAccess, context -> ((ServerCommandSource)context.getSource()).getWorld())))
            .then(
               CommandManager.literal("from")
                  .then(
                     CommandManager.argument("sourceDimension", DimensionArgumentType.dimension())
                        .then(createSourceArgs(commandRegistryAccess, context -> DimensionArgumentType.getDimensionArgument(context, "sourceDimension")))
                  )
            )
      );
   }

   private static ArgumentBuilder<ServerCommandSource, ?> createSourceArgs(
      CommandRegistryAccess commandRegistryAccess, ArgumentGetter<CommandContext<ServerCommandSource>, ServerWorld> worldGetter
   ) {
      return CommandManager.argument("begin", BlockPosArgumentType.blockPos())
         .then(
            ((RequiredArgumentBuilder)CommandManager.argument("end", BlockPosArgumentType.blockPos())
                  .then(createDestinationArgs(commandRegistryAccess, worldGetter, context -> ((ServerCommandSource)context.getSource()).getWorld())))
               .then(
                  CommandManager.literal("to")
                     .then(
                        CommandManager.argument("targetDimension", DimensionArgumentType.dimension())
                           .then(
                              createDestinationArgs(
                                 commandRegistryAccess, worldGetter, context -> DimensionArgumentType.getDimensionArgument(context, "targetDimension")
                              )
                           )
                     )
               )
         );
   }

   private static CloneCommand.DimensionalPos createDimensionalPos(CommandContext<ServerCommandSource> context, ServerWorld world, String name) throws CommandSyntaxException {
      BlockPos blockPos = BlockPosArgumentType.getLoadedBlockPos(context, world, name);
      return new CloneCommand.DimensionalPos(world, blockPos);
   }

   private static ArgumentBuilder<ServerCommandSource, ?> createDestinationArgs(
      CommandRegistryAccess registries,
      ArgumentGetter<CommandContext<ServerCommandSource>, ServerWorld> currentWorldGetter,
      ArgumentGetter<CommandContext<ServerCommandSource>, ServerWorld> targetWorldGetter
   ) {
      ArgumentGetter<CommandContext<ServerCommandSource>, CloneCommand.DimensionalPos> argumentGetter = context -> createDimensionalPos(
         context, currentWorldGetter.apply(context), "begin"
      );
      ArgumentGetter<CommandContext<ServerCommandSource>, CloneCommand.DimensionalPos> argumentGetter2 = context -> createDimensionalPos(
         context, currentWorldGetter.apply(context), "end"
      );
      ArgumentGetter<CommandContext<ServerCommandSource>, CloneCommand.DimensionalPos> argumentGetter3 = context -> createDimensionalPos(
         context, targetWorldGetter.apply(context), "destination"
      );
      return appendMode(
            registries, argumentGetter, argumentGetter2, argumentGetter3, false, CommandManager.argument("destination", BlockPosArgumentType.blockPos())
         )
         .then(appendMode(registries, argumentGetter, argumentGetter2, argumentGetter3, true, CommandManager.literal("strict")));
   }

   private static ArgumentBuilder<ServerCommandSource, ?> appendMode(
      CommandRegistryAccess registries,
      ArgumentGetter<CommandContext<ServerCommandSource>, CloneCommand.DimensionalPos> beginPosGetter,
      ArgumentGetter<CommandContext<ServerCommandSource>, CloneCommand.DimensionalPos> endPosGetter,
      ArgumentGetter<CommandContext<ServerCommandSource>, CloneCommand.DimensionalPos> destinationPosGetter,
      boolean strict,
      ArgumentBuilder<ServerCommandSource, ?> builder
   ) {
      return builder.executes(
            context -> execute(
               (ServerCommandSource)context.getSource(),
               beginPosGetter.apply(context),
               endPosGetter.apply(context),
               destinationPosGetter.apply(context),
               pos -> true,
               CloneCommand.Mode.NORMAL,
               strict
            )
         )
         .then(createModeArgs(beginPosGetter, endPosGetter, destinationPosGetter, context -> pos -> true, strict, CommandManager.literal("replace")))
         .then(createModeArgs(beginPosGetter, endPosGetter, destinationPosGetter, context -> IS_AIR_PREDICATE, strict, CommandManager.literal("masked")))
         .then(
            CommandManager.literal("filtered")
               .then(
                  createModeArgs(
                     beginPosGetter,
                     endPosGetter,
                     destinationPosGetter,
                     context -> BlockPredicateArgumentType.getBlockPredicate(context, "filter"),
                     strict,
                     CommandManager.argument("filter", BlockPredicateArgumentType.blockPredicate(registries))
                  )
               )
         );
   }

   private static ArgumentBuilder<ServerCommandSource, ?> createModeArgs(
      ArgumentGetter<CommandContext<ServerCommandSource>, CloneCommand.DimensionalPos> beginPosGetter,
      ArgumentGetter<CommandContext<ServerCommandSource>, CloneCommand.DimensionalPos> endPosGetter,
      ArgumentGetter<CommandContext<ServerCommandSource>, CloneCommand.DimensionalPos> destinationPosGetter,
      ArgumentGetter<CommandContext<ServerCommandSource>, Predicate<CachedBlockPosition>> filterGetter,
      boolean strict,
      ArgumentBuilder<ServerCommandSource, ?> builder
   ) {
      return builder.executes(
            context -> execute(
               (ServerCommandSource)context.getSource(),
               beginPosGetter.apply(context),
               endPosGetter.apply(context),
               destinationPosGetter.apply(context),
               filterGetter.apply(context),
               CloneCommand.Mode.NORMAL,
               strict
            )
         )
         .then(
            CommandManager.literal("force")
               .executes(
                  context -> execute(
                     (ServerCommandSource)context.getSource(),
                     beginPosGetter.apply(context),
                     endPosGetter.apply(context),
                     destinationPosGetter.apply(context),
                     filterGetter.apply(context),
                     CloneCommand.Mode.FORCE,
                     strict
                  )
               )
         )
         .then(
            CommandManager.literal("move")
               .executes(
                  context -> execute(
                     (ServerCommandSource)context.getSource(),
                     beginPosGetter.apply(context),
                     endPosGetter.apply(context),
                     destinationPosGetter.apply(context),
                     filterGetter.apply(context),
                     CloneCommand.Mode.MOVE,
                     strict
                  )
               )
         )
         .then(
            CommandManager.literal("normal")
               .executes(
                  context -> execute(
                     (ServerCommandSource)context.getSource(),
                     beginPosGetter.apply(context),
                     endPosGetter.apply(context),
                     destinationPosGetter.apply(context),
                     filterGetter.apply(context),
                     CloneCommand.Mode.NORMAL,
                     strict
                  )
               )
         );
   }

   private static int execute(
      ServerCommandSource source,
      CloneCommand.DimensionalPos begin,
      CloneCommand.DimensionalPos end,
      CloneCommand.DimensionalPos destination,
      Predicate<CachedBlockPosition> filter,
      CloneCommand.Mode mode,
      boolean strict
   ) throws CommandSyntaxException {
      BlockPos blockPos = begin.position();
      BlockPos blockPos2 = end.position();
      BlockBox blockBox = BlockBox.create(blockPos, blockPos2);
      BlockPos blockPos3 = destination.position();
      BlockPos blockPos4 = blockPos3.add(blockBox.getDimensions());
      BlockBox blockBox2 = BlockBox.create(blockPos3, blockPos4);
      ServerWorld serverWorld = begin.dimension();
      ServerWorld serverWorld2 = destination.dimension();
      if (!mode.allowsOverlap() && serverWorld == serverWorld2 && blockBox2.intersects(blockBox)) {
         throw OVERLAP_EXCEPTION.create();
      } else {
         int i = blockBox.getBlockCountX() * blockBox.getBlockCountY() * blockBox.getBlockCountZ();
         int j = source.getWorld().getGameRules().getValue(GameRules.MAX_BLOCK_MODIFICATIONS);
         if (i > j) {
            throw TOO_BIG_EXCEPTION.create(j, i);
         } else if (!serverWorld.isRegionLoaded(blockPos, blockPos2) || !serverWorld2.isRegionLoaded(blockPos3, blockPos4)) {
            throw BlockPosArgumentType.UNLOADED_EXCEPTION.create();
         } else if (serverWorld2.isDebugWorld()) {
            throw FAILED_EXCEPTION.create();
         } else {
            List<CloneCommand.BlockInfo> list = Lists.newArrayList();
            List<CloneCommand.BlockInfo> list2 = Lists.newArrayList();
            List<CloneCommand.BlockInfo> list3 = Lists.newArrayList();
            Deque<BlockPos> deque = Lists.newLinkedList();
            int k = 0;
            ErrorReporter.Logging logging = new ErrorReporter.Logging(LOGGER);

            try {
               BlockPos blockPos5 = new BlockPos(
                  blockBox2.getMinX() - blockBox.getMinX(), blockBox2.getMinY() - blockBox.getMinY(), blockBox2.getMinZ() - blockBox.getMinZ()
               );

               for (int l = blockBox.getMinZ(); l <= blockBox.getMaxZ(); l++) {
                  for (int m = blockBox.getMinY(); m <= blockBox.getMaxY(); m++) {
                     for (int n = blockBox.getMinX(); n <= blockBox.getMaxX(); n++) {
                        BlockPos blockPos6 = new BlockPos(n, m, l);
                        BlockPos blockPos7 = blockPos6.add(blockPos5);
                        CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(serverWorld, blockPos6, false);
                        BlockState blockState = cachedBlockPosition.getBlockState();
                        if (filter.test(cachedBlockPosition)) {
                           BlockEntity blockEntity = serverWorld.getBlockEntity(blockPos6);
                           if (blockEntity != null) {
                              NbtWriteView nbtWriteView = NbtWriteView.create(logging.makeChild(blockEntity.getReporterContext()), source.getRegistryManager());
                              blockEntity.writeComponentlessData(nbtWriteView);
                              CloneCommand.BlockEntityInfo blockEntityInfo = new CloneCommand.BlockEntityInfo(
                                 nbtWriteView.getNbt(), blockEntity.getComponents()
                              );
                              list2.add(new CloneCommand.BlockInfo(blockPos7, blockState, blockEntityInfo, serverWorld2.getBlockState(blockPos7)));
                              deque.addLast(blockPos6);
                           } else if (!blockState.isOpaqueFullCube() && !blockState.isFullCube(serverWorld, blockPos6)) {
                              list3.add(new CloneCommand.BlockInfo(blockPos7, blockState, null, serverWorld2.getBlockState(blockPos7)));
                              deque.addFirst(blockPos6);
                           } else {
                              list.add(new CloneCommand.BlockInfo(blockPos7, blockState, null, serverWorld2.getBlockState(blockPos7)));
                              deque.addLast(blockPos6);
                           }
                        }
                     }
                  }
               }

               int l = 2 | (strict ? 816 : 0);
               if (mode == CloneCommand.Mode.MOVE) {
                  for (BlockPos blockPos8 : deque) {
                     serverWorld.setBlockState(blockPos8, Blocks.BARRIER.getDefaultState(), l | 816);
                  }

                  int m = strict ? l : 3;

                  for (BlockPos blockPos6 : deque) {
                     serverWorld.setBlockState(blockPos6, Blocks.AIR.getDefaultState(), m);
                  }
               }

               List<CloneCommand.BlockInfo> list4 = Lists.newArrayList();
               list4.addAll(list);
               list4.addAll(list2);
               list4.addAll(list3);
               List<CloneCommand.BlockInfo> list5 = Lists.reverse(list4);

               for (CloneCommand.BlockInfo blockInfo : list5) {
                  serverWorld2.setBlockState(blockInfo.pos, Blocks.BARRIER.getDefaultState(), l | 816);
               }

               for (CloneCommand.BlockInfo blockInfo : list4) {
                  if (serverWorld2.setBlockState(blockInfo.pos, blockInfo.state, l)) {
                     k++;
                  }
               }

               for (CloneCommand.BlockInfo blockInfox : list2) {
                  BlockEntity blockEntity2 = serverWorld2.getBlockEntity(blockInfox.pos);
                  if (blockInfox.blockEntityInfo != null && blockEntity2 != null) {
                     blockEntity2.readComponentlessData(
                        NbtReadView.create(
                           logging.makeChild(blockEntity2.getReporterContext()), serverWorld2.getRegistryManager(), blockInfox.blockEntityInfo.nbt
                        )
                     );
                     blockEntity2.setComponents(blockInfox.blockEntityInfo.components);
                     blockEntity2.markDirty();
                  }

                  serverWorld2.setBlockState(blockInfox.pos, blockInfox.state, l);
               }

               if (!strict) {
                  for (CloneCommand.BlockInfo blockInfox : list5) {
                     serverWorld2.onStateReplacedWithCommands(blockInfox.pos, blockInfox.previousStateAtDestination);
                  }
               }

               serverWorld2.getBlockTickScheduler().scheduleTicks(serverWorld.getBlockTickScheduler(), blockBox, blockPos5);
            } catch (Throwable var35) {
               try {
                  logging.close();
               } catch (Throwable var34) {
                  var35.addSuppressed(var34);
               }

               throw var35;
            }

            logging.close();
            if (k == 0) {
               throw FAILED_EXCEPTION.create();
            } else {
               int o = k;
               source.sendFeedback(() -> Text.translatable("commands.clone.success", o), true);
               return k;
            }
         }
      }
   }

   record BlockEntityInfo(NbtCompound nbt, ComponentMap components) {
   }

   record BlockInfo(BlockPos pos, BlockState state, CloneCommand.@Nullable BlockEntityInfo blockEntityInfo, BlockState previousStateAtDestination) {
   }

   record DimensionalPos(ServerWorld dimension, BlockPos position) {
   }

   static enum Mode {
      FORCE(true),
      MOVE(true),
      NORMAL(false);

      private final boolean allowsOverlap;

      private Mode(final boolean allowsOverlap) {
         this.allowsOverlap = allowsOverlap;
      }

      public boolean allowsOverlap() {
         return this.allowsOverlap;
      }
   }
}
