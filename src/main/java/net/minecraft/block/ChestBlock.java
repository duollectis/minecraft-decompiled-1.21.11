package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LidOpenable;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

public class ChestBlock extends AbstractChestBlock<ChestBlockEntity> implements Waterloggable {
   public static final MapCodec<ChestBlock> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Registries.SOUND_EVENT.getCodec().fieldOf("open_sound").forGetter(ChestBlock::getOpenSound),
            Registries.SOUND_EVENT.getCodec().fieldOf("close_sound").forGetter(ChestBlock::getCloseSound),
            createSettingsCodec()
         )
         .apply(instance, (openSound, closeSound, settings) -> new ChestBlock(() -> BlockEntityType.CHEST, openSound, closeSound, settings))
   );
   public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
   public static final EnumProperty<ChestType> CHEST_TYPE = Properties.CHEST_TYPE;
   public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
   public static final int field_31057 = 1;
   private static final VoxelShape SINGLE_SHAPE = Block.createColumnShape(14.0, 0.0, 14.0);
   private static final Map<Direction, VoxelShape> DOUBLE_SHAPES_BY_DIRECTION = VoxelShapes.createHorizontalFacingShapeMap(
      Block.createCuboidZShape(14.0, 0.0, 14.0, 0.0, 15.0)
   );
   private final SoundEvent openSound;
   private final SoundEvent closeSound;
   private static final DoubleBlockProperties.PropertyRetriever<ChestBlockEntity, Optional<Inventory>> INVENTORY_RETRIEVER = new DoubleBlockProperties.PropertyRetriever<ChestBlockEntity, Optional<Inventory>>() {
      public Optional<Inventory> getFromBoth(ChestBlockEntity chestBlockEntity, ChestBlockEntity chestBlockEntity2) {
         return Optional.of(new DoubleInventory(chestBlockEntity, chestBlockEntity2));
      }

      public Optional<Inventory> getFrom(ChestBlockEntity chestBlockEntity) {
         return Optional.of(chestBlockEntity);
      }

      public Optional<Inventory> getFallback() {
         return Optional.empty();
      }
   };
   private static final DoubleBlockProperties.PropertyRetriever<ChestBlockEntity, Optional<NamedScreenHandlerFactory>> NAME_RETRIEVER = new DoubleBlockProperties.PropertyRetriever<ChestBlockEntity, Optional<NamedScreenHandlerFactory>>() {
      public Optional<NamedScreenHandlerFactory> getFromBoth(ChestBlockEntity chestBlockEntity, ChestBlockEntity chestBlockEntity2) {
         final Inventory inventory = new DoubleInventory(chestBlockEntity, chestBlockEntity2);
         return Optional.of(new NamedScreenHandlerFactory() {
            @Override
            public @Nullable ScreenHandler createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
               if (chestBlockEntity.checkUnlocked(playerEntity) && chestBlockEntity2.checkUnlocked(playerEntity)) {
                  chestBlockEntity.generateLoot(playerInventory.player);
                  chestBlockEntity2.generateLoot(playerInventory.player);
                  return GenericContainerScreenHandler.createGeneric9x6(i, playerInventory, inventory);
               } else {
                  Direction direction = ChestBlock.getFacing(chestBlockEntity.getCachedState());
                  Vec3d vec3d = chestBlockEntity.getPos().toCenterPos();
                  Vec3d vec3d2 = vec3d.add(direction.getOffsetX() / 2.0, 0.0, direction.getOffsetZ() / 2.0);
                  LockableContainerBlockEntity.handleLocked(vec3d2, playerEntity, this.getDisplayName());
                  return null;
               }
            }

            @Override
            public Text getDisplayName() {
               if (chestBlockEntity.hasCustomName()) {
                  return chestBlockEntity.getDisplayName();
               } else {
                  return (Text)(chestBlockEntity2.hasCustomName() ? chestBlockEntity2.getDisplayName() : Text.translatable("container.chestDouble"));
               }
            }
         });
      }

      public Optional<NamedScreenHandlerFactory> getFrom(ChestBlockEntity chestBlockEntity) {
         return Optional.of(chestBlockEntity);
      }

      public Optional<NamedScreenHandlerFactory> getFallback() {
         return Optional.empty();
      }
   };

   @Override
   public MapCodec<? extends ChestBlock> getCodec() {
      return CODEC;
   }

   public ChestBlock(
      Supplier<BlockEntityType<? extends ChestBlockEntity>> blockEntityTypeSupplier,
      SoundEvent openSound,
      SoundEvent closeSound,
      AbstractBlock.Settings settings
   ) {
      super(settings, blockEntityTypeSupplier);
      this.openSound = openSound;
      this.closeSound = closeSound;
      this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(CHEST_TYPE, ChestType.SINGLE).with(WATERLOGGED, false));
   }

   public static DoubleBlockProperties.Type getDoubleBlockType(BlockState state) {
      ChestType chestType = state.get(CHEST_TYPE);
      if (chestType == ChestType.SINGLE) {
         return DoubleBlockProperties.Type.SINGLE;
      } else {
         return chestType == ChestType.RIGHT ? DoubleBlockProperties.Type.FIRST : DoubleBlockProperties.Type.SECOND;
      }
   }

   @Override
   protected BlockState getStateForNeighborUpdate(
      BlockState state,
      WorldView world,
      ScheduledTickView tickView,
      BlockPos pos,
      Direction direction,
      BlockPos neighborPos,
      BlockState neighborState,
      Random random
   ) {
      if (state.get(WATERLOGGED)) {
         tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
      }

      if (this.canMergeWith(neighborState) && direction.getAxis().isHorizontal()) {
         ChestType chestType = neighborState.get(CHEST_TYPE);
         if (state.get(CHEST_TYPE) == ChestType.SINGLE
            && chestType != ChestType.SINGLE
            && state.get(FACING) == neighborState.get(FACING)
            && getFacing(neighborState) == direction.getOpposite()) {
            return state.with(CHEST_TYPE, chestType.getOpposite());
         }
      } else if (getFacing(state) == direction) {
         return state.with(CHEST_TYPE, ChestType.SINGLE);
      }

      return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
   }

   public boolean canMergeWith(BlockState state) {
      return state.isOf(this);
   }

   @Override
   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return switch ((ChestType)state.get(CHEST_TYPE)) {
         case SINGLE -> SINGLE_SHAPE;
         case LEFT, RIGHT -> (VoxelShape)DOUBLE_SHAPES_BY_DIRECTION.get(getFacing(state));
      };
   }

   public static Direction getFacing(BlockState state) {
      Direction direction = state.get(FACING);
      return state.get(CHEST_TYPE) == ChestType.LEFT ? direction.rotateYClockwise() : direction.rotateYCounterclockwise();
   }

   public static BlockPos getPosInFrontOf(BlockPos pos, BlockState state) {
      Direction direction = getFacing(state);
      return pos.offset(direction);
   }

   @Override
   public BlockState getPlacementState(ItemPlacementContext ctx) {
      ChestType chestType = ChestType.SINGLE;
      Direction direction = ctx.getHorizontalPlayerFacing().getOpposite();
      FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
      boolean bl = ctx.shouldCancelInteraction();
      Direction direction2 = ctx.getSide();
      if (direction2.getAxis().isHorizontal() && bl) {
         Direction direction3 = this.getNeighborChestDirection(ctx.getWorld(), ctx.getBlockPos(), direction2.getOpposite());
         if (direction3 != null && direction3.getAxis() != direction2.getAxis()) {
            direction = direction3;
            chestType = direction3.rotateYCounterclockwise() == direction2.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
         }
      }

      if (chestType == ChestType.SINGLE && !bl) {
         chestType = this.getChestType(ctx.getWorld(), ctx.getBlockPos(), direction);
      }

      return this.getDefaultState().with(FACING, direction).with(CHEST_TYPE, chestType).with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
   }

   protected ChestType getChestType(World world, BlockPos pos, Direction facing) {
      if (facing == this.getNeighborChestDirection(world, pos, facing.rotateYClockwise())) {
         return ChestType.LEFT;
      } else {
         return facing == this.getNeighborChestDirection(world, pos, facing.rotateYCounterclockwise()) ? ChestType.RIGHT : ChestType.SINGLE;
      }
   }

   @Override
   protected FluidState getFluidState(BlockState state) {
      return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
   }

   private @Nullable Direction getNeighborChestDirection(World world, BlockPos pos, Direction neighborOffset) {
      BlockState blockState = world.getBlockState(pos.offset(neighborOffset));
      return this.canMergeWith(blockState) && blockState.get(CHEST_TYPE) == ChestType.SINGLE ? blockState.get(FACING) : null;
   }

   @Override
   protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
      ItemScatterer.onStateReplaced(state, world, pos);
   }

   @Override
   protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
      if (world instanceof ServerWorld serverWorld) {
         NamedScreenHandlerFactory namedScreenHandlerFactory = this.createScreenHandlerFactory(state, world, pos);
         if (namedScreenHandlerFactory != null) {
            player.openHandledScreen(namedScreenHandlerFactory);
            player.incrementStat(this.getOpenStat());
            PiglinBrain.onGuardedBlockInteracted(serverWorld, player, true);
         }
      }

      return ActionResult.SUCCESS;
   }

   protected Stat<Identifier> getOpenStat() {
      return Stats.CUSTOM.getOrCreateStat(Stats.OPEN_CHEST);
   }

   public BlockEntityType<? extends ChestBlockEntity> getExpectedEntityType() {
      return this.entityTypeRetriever.get();
   }

   public static @Nullable Inventory getInventory(ChestBlock block, BlockState state, World world, BlockPos pos, boolean ignoreBlocked) {
      return block.getBlockEntitySource(state, world, pos, ignoreBlocked).apply(INVENTORY_RETRIEVER).orElse(null);
   }

   @Override
   public DoubleBlockProperties.PropertySource<? extends ChestBlockEntity> getBlockEntitySource(
      BlockState state, World world, BlockPos pos, boolean ignoreBlocked
   ) {
      BiPredicate<WorldAccess, BlockPos> biPredicate;
      if (ignoreBlocked) {
         biPredicate = (worldx, posx) -> false;
      } else {
         biPredicate = ChestBlock::isChestBlocked;
      }

      return DoubleBlockProperties.toPropertySource(
         this.entityTypeRetriever.get(), ChestBlock::getDoubleBlockType, ChestBlock::getFacing, FACING, state, world, pos, biPredicate
      );
   }

   @Override
   protected @Nullable NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
      return this.getBlockEntitySource(state, world, pos, false).apply(NAME_RETRIEVER).orElse(null);
   }

   public static DoubleBlockProperties.PropertyRetriever<ChestBlockEntity, Float2FloatFunction> getAnimationProgressRetriever(LidOpenable progress) {
      return new DoubleBlockProperties.PropertyRetriever<ChestBlockEntity, Float2FloatFunction>() {
         public Float2FloatFunction getFromBoth(ChestBlockEntity chestBlockEntity, ChestBlockEntity chestBlockEntity2) {
            return tickProgress -> Math.max(chestBlockEntity.getAnimationProgress(tickProgress), chestBlockEntity2.getAnimationProgress(tickProgress));
         }

         public Float2FloatFunction getFrom(ChestBlockEntity chestBlockEntity) {
            return chestBlockEntity::getAnimationProgress;
         }

         public Float2FloatFunction getFallback() {
            return progress::getAnimationProgress;
         }
      };
   }

   @Override
   public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
      return new ChestBlockEntity(pos, state);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
      return world.isClient() ? validateTicker(type, this.getExpectedEntityType(), ChestBlockEntity::clientTick) : null;
   }

   public static boolean isChestBlocked(WorldAccess world, BlockPos pos) {
      return hasBlockOnTop(world, pos) || hasCatOnTop(world, pos);
   }

   private static boolean hasBlockOnTop(BlockView world, BlockPos pos) {
      BlockPos blockPos = pos.up();
      return world.getBlockState(blockPos).isSolidBlock(world, blockPos);
   }

   private static boolean hasCatOnTop(WorldAccess world, BlockPos pos) {
      List<CatEntity> list = world.getNonSpectatingEntities(
         CatEntity.class, new Box(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1)
      );
      if (!list.isEmpty()) {
         for (CatEntity catEntity : list) {
            if (catEntity.isInSittingPose()) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   protected boolean hasComparatorOutput(BlockState state) {
      return true;
   }

   @Override
   protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
      return ScreenHandler.calculateComparatorOutput(getInventory(this, state, world, pos, false));
   }

   @Override
   protected BlockState rotate(BlockState state, BlockRotation rotation) {
      return state.with(FACING, rotation.rotate(state.get(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState state, BlockMirror mirror) {
      return state.rotate(mirror.getRotation(state.get(FACING)));
   }

   @Override
   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(FACING, CHEST_TYPE, WATERLOGGED);
   }

   @Override
   protected boolean canPathfindThrough(BlockState state, NavigationType type) {
      return false;
   }

   @Override
   protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
      BlockEntity blockEntity = world.getBlockEntity(pos);
      if (blockEntity instanceof ChestBlockEntity) {
         ((ChestBlockEntity)blockEntity).onScheduledTick();
      }
   }

   public SoundEvent getOpenSound() {
      return this.openSound;
   }

   public SoundEvent getCloseSound() {
      return this.closeSound;
   }
}
