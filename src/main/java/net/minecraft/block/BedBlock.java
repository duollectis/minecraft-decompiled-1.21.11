package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.Dismounting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.DirectionTransformation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.CollisionView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.tick.ScheduledTickView;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;

public class BedBlock extends HorizontalFacingBlock implements BlockEntityProvider {
   public static final MapCodec<BedBlock> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(DyeColor.CODEC.fieldOf("color").forGetter(BedBlock::getColor), createSettingsCodec()).apply(instance, BedBlock::new)
   );
   public static final EnumProperty<BedPart> PART = Properties.BED_PART;
   public static final BooleanProperty OCCUPIED = Properties.OCCUPIED;
   private static final Map<Direction, VoxelShape> SHAPES_BY_DIRECTION = Util.make(() -> {
      VoxelShape voxelShape = Block.createCuboidShape(0.0, 0.0, 0.0, 3.0, 3.0, 3.0);
      VoxelShape voxelShape2 = VoxelShapes.transform(voxelShape, DirectionTransformation.field_64511);
      return VoxelShapes.createHorizontalFacingShapeMap(VoxelShapes.union(Block.createColumnShape(16.0, 3.0, 9.0), voxelShape, voxelShape2));
   });
   private final DyeColor color;

   @Override
   public MapCodec<BedBlock> getCodec() {
      return CODEC;
   }

   public BedBlock(DyeColor color, AbstractBlock.Settings settings) {
      super(settings);
      this.color = color;
      this.setDefaultState(this.stateManager.getDefaultState().with(PART, BedPart.FOOT).with(OCCUPIED, false));
   }

   public static @Nullable Direction getDirection(BlockView world, BlockPos pos) {
      BlockState blockState = world.getBlockState(pos);
      return blockState.getBlock() instanceof BedBlock ? blockState.get(FACING) : null;
   }

   @Override
   protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
      if (world.isClient()) {
         return ActionResult.SUCCESS_SERVER;
      } else {
         if (state.get(PART) != BedPart.HEAD) {
            pos = pos.offset(state.get(FACING));
            state = world.getBlockState(pos);
            if (!state.isOf(this)) {
               return ActionResult.CONSUME;
            }
         }

         BedRule bedRule = world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.BED_RULE_GAMEPLAY, pos);
         if (bedRule.explodes()) {
            bedRule.errorMessage().ifPresent(message -> player.sendMessage(message, true));
            world.removeBlock(pos, false);
            BlockPos blockPos = pos.offset(state.get(FACING).getOpposite());
            if (world.getBlockState(blockPos).isOf(this)) {
               world.removeBlock(blockPos, false);
            }

            Vec3d vec3d = pos.toCenterPos();
            world.createExplosion(null, world.getDamageSources().badRespawnPoint(vec3d), null, vec3d, 5.0F, true, World.ExplosionSourceType.BLOCK);
            return ActionResult.SUCCESS_SERVER;
         } else if (state.get(OCCUPIED)) {
            if (!this.wakeVillager(world, pos)) {
               player.sendMessage(Text.translatable("block.minecraft.bed.occupied"), true);
            }

            return ActionResult.SUCCESS_SERVER;
         } else {
            player.trySleep(pos).ifLeft(reason -> {
               if (reason.message() != null) {
                  player.sendMessage(reason.message(), true);
               }
            });
            return ActionResult.SUCCESS_SERVER;
         }
      }
   }

   private boolean wakeVillager(World world, BlockPos pos) {
      List<VillagerEntity> list = world.getEntitiesByClass(VillagerEntity.class, new Box(pos), LivingEntity::isSleeping);
      if (list.isEmpty()) {
         return false;
      } else {
         list.get(0).wakeUp();
         return true;
      }
   }

   @Override
   public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
      super.onLandedUpon(world, state, pos, entity, fallDistance * 0.5);
   }

   @Override
   public void onEntityLand(BlockView world, Entity entity) {
      if (entity.bypassesLandingEffects()) {
         super.onEntityLand(world, entity);
      } else {
         this.bounceEntity(entity);
      }
   }

   private void bounceEntity(Entity entity) {
      Vec3d vec3d = entity.getVelocity();
      if (vec3d.y < 0.0) {
         double d = entity instanceof LivingEntity ? 1.0 : 0.8;
         entity.setVelocity(vec3d.x, -vec3d.y * 0.66F * d, vec3d.z);
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
      if (direction == getDirectionTowardsOtherPart(state.get(PART), state.get(FACING))) {
         return neighborState.isOf(this) && neighborState.get(PART) != state.get(PART)
            ? state.with(OCCUPIED, neighborState.get(OCCUPIED))
            : Blocks.AIR.getDefaultState();
      } else {
         return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
      }
   }

   private static Direction getDirectionTowardsOtherPart(BedPart part, Direction direction) {
      return part == BedPart.FOOT ? direction : direction.getOpposite();
   }

   @Override
   public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
      if (!world.isClient() && player.shouldSkipBlockDrops()) {
         BedPart bedPart = state.get(PART);
         if (bedPart == BedPart.FOOT) {
            BlockPos blockPos = pos.offset(getDirectionTowardsOtherPart(bedPart, state.get(FACING)));
            BlockState blockState = world.getBlockState(blockPos);
            if (blockState.isOf(this) && blockState.get(PART) == BedPart.HEAD) {
               world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 35);
               world.syncWorldEvent(player, 2001, blockPos, Block.getRawIdFromState(blockState));
            }
         }
      }

      return super.onBreak(world, pos, state, player);
   }

   @Override
   public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
      Direction direction = ctx.getHorizontalPlayerFacing();
      BlockPos blockPos = ctx.getBlockPos();
      BlockPos blockPos2 = blockPos.offset(direction);
      World world = ctx.getWorld();
      return world.getBlockState(blockPos2).canReplace(ctx) && world.getWorldBorder().contains(blockPos2)
         ? this.getDefaultState().with(FACING, direction)
         : null;
   }

   @Override
   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return SHAPES_BY_DIRECTION.get(getOppositePartDirection(state).getOpposite());
   }

   public static Direction getOppositePartDirection(BlockState state) {
      Direction direction = state.get(FACING);
      return state.get(PART) == BedPart.HEAD ? direction.getOpposite() : direction;
   }

   public static DoubleBlockProperties.Type getBedPart(BlockState state) {
      BedPart bedPart = state.get(PART);
      return bedPart == BedPart.HEAD ? DoubleBlockProperties.Type.FIRST : DoubleBlockProperties.Type.SECOND;
   }

   private static boolean isBedBelow(BlockView world, BlockPos pos) {
      return world.getBlockState(pos.down()).getBlock() instanceof BedBlock;
   }

   public static Optional<Vec3d> findWakeUpPosition(EntityType<?> type, CollisionView world, BlockPos pos, Direction bedDirection, float spawnAngle) {
      Direction direction = bedDirection.rotateYClockwise();
      Direction direction2 = direction.pointsTo(spawnAngle) ? direction.getOpposite() : direction;
      if (isBedBelow(world, pos)) {
         return findWakeUpPosition(type, world, pos, bedDirection, direction2);
      } else {
         int[][] is = getAroundAndOnBedOffsets(bedDirection, direction2);
         Optional<Vec3d> optional = findWakeUpPosition(type, world, pos, is, true);
         return optional.isPresent() ? optional : findWakeUpPosition(type, world, pos, is, false);
      }
   }

   private static Optional<Vec3d> findWakeUpPosition(EntityType<?> type, CollisionView world, BlockPos pos, Direction bedDirection, Direction respawnDirection) {
      int[][] is = getAroundBedOffsets(bedDirection, respawnDirection);
      Optional<Vec3d> optional = findWakeUpPosition(type, world, pos, is, true);
      if (optional.isPresent()) {
         return optional;
      } else {
         BlockPos blockPos = pos.down();
         Optional<Vec3d> optional2 = findWakeUpPosition(type, world, blockPos, is, true);
         if (optional2.isPresent()) {
            return optional2;
         } else {
            int[][] js = getOnBedOffsets(bedDirection);
            Optional<Vec3d> optional3 = findWakeUpPosition(type, world, pos, js, true);
            if (optional3.isPresent()) {
               return optional3;
            } else {
               Optional<Vec3d> optional4 = findWakeUpPosition(type, world, pos, is, false);
               if (optional4.isPresent()) {
                  return optional4;
               } else {
                  Optional<Vec3d> optional5 = findWakeUpPosition(type, world, blockPos, is, false);
                  return optional5.isPresent() ? optional5 : findWakeUpPosition(type, world, pos, js, false);
               }
            }
         }
      }
   }

   private static Optional<Vec3d> findWakeUpPosition(EntityType<?> type, CollisionView world, BlockPos pos, int[][] possibleOffsets, boolean ignoreInvalidPos) {
      BlockPos.Mutable mutable = new BlockPos.Mutable();

      for (int[] is : possibleOffsets) {
         mutable.set(pos.getX() + is[0], pos.getY(), pos.getZ() + is[1]);
         Vec3d vec3d = Dismounting.findRespawnPos(type, world, mutable, ignoreInvalidPos);
         if (vec3d != null) {
            return Optional.of(vec3d);
         }
      }

      return Optional.empty();
   }

   @Override
   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(FACING, PART, OCCUPIED);
   }

   @Override
   public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
      return new BedBlockEntity(pos, state, this.color);
   }

   @Override
   public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
      super.onPlaced(world, pos, state, placer, itemStack);
      if (!world.isClient()) {
         BlockPos blockPos = pos.offset(state.get(FACING));
         world.setBlockState(blockPos, state.with(PART, BedPart.HEAD), 3);
         world.updateNeighbors(pos, Blocks.AIR);
         state.updateNeighbors(world, pos, 3);
      }
   }

   public DyeColor getColor() {
      return this.color;
   }

   @Override
   protected long getRenderingSeed(BlockState state, BlockPos pos) {
      BlockPos blockPos = pos.offset(state.get(FACING), state.get(PART) == BedPart.HEAD ? 0 : 1);
      return MathHelper.hashCode(blockPos.getX(), pos.getY(), blockPos.getZ());
   }

   @Override
   protected boolean canPathfindThrough(BlockState state, NavigationType type) {
      return false;
   }

   private static int[][] getAroundAndOnBedOffsets(Direction bedDirection, Direction respawnDirection) {
      return (int[][])ArrayUtils.addAll(getAroundBedOffsets(bedDirection, respawnDirection), getOnBedOffsets(bedDirection));
   }

   private static int[][] getAroundBedOffsets(Direction bedDirection, Direction respawnDirection) {
      return new int[][]{
         {respawnDirection.getOffsetX(), respawnDirection.getOffsetZ()},
         {respawnDirection.getOffsetX() - bedDirection.getOffsetX(), respawnDirection.getOffsetZ() - bedDirection.getOffsetZ()},
         {respawnDirection.getOffsetX() - bedDirection.getOffsetX() * 2, respawnDirection.getOffsetZ() - bedDirection.getOffsetZ() * 2},
         {-bedDirection.getOffsetX() * 2, -bedDirection.getOffsetZ() * 2},
         {-respawnDirection.getOffsetX() - bedDirection.getOffsetX() * 2, -respawnDirection.getOffsetZ() - bedDirection.getOffsetZ() * 2},
         {-respawnDirection.getOffsetX() - bedDirection.getOffsetX(), -respawnDirection.getOffsetZ() - bedDirection.getOffsetZ()},
         {-respawnDirection.getOffsetX(), -respawnDirection.getOffsetZ()},
         {-respawnDirection.getOffsetX() + bedDirection.getOffsetX(), -respawnDirection.getOffsetZ() + bedDirection.getOffsetZ()},
         {bedDirection.getOffsetX(), bedDirection.getOffsetZ()},
         {respawnDirection.getOffsetX() + bedDirection.getOffsetX(), respawnDirection.getOffsetZ() + bedDirection.getOffsetZ()}
      };
   }

   private static int[][] getOnBedOffsets(Direction bedDirection) {
      return new int[][]{{0, 0}, {-bedDirection.getOffsetX(), -bedDirection.getOffsetZ()}};
   }
}
