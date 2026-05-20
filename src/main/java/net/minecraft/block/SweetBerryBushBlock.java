package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;

public class SweetBerryBushBlock extends PlantBlock implements Fertilizable {
   public static final MapCodec<SweetBerryBushBlock> CODEC = createCodec(SweetBerryBushBlock::new);
   private static final float MIN_MOVEMENT_FOR_DAMAGE = 0.003F;
   public static final int MAX_AGE = 3;
   public static final IntProperty AGE = Properties.AGE_3;
   private static final VoxelShape SMALL_SHAPE = Block.createColumnShape(10.0, 0.0, 8.0);
   private static final VoxelShape LARGE_SHAPE = Block.createColumnShape(14.0, 0.0, 16.0);

   @Override
   public MapCodec<SweetBerryBushBlock> getCodec() {
      return CODEC;
   }

   public SweetBerryBushBlock(AbstractBlock.Settings settings) {
      super(settings);
      this.setDefaultState(this.stateManager.getDefaultState().with(AGE, 0));
   }

   @Override
   protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
      return new ItemStack(Items.SWEET_BERRIES);
   }

   @Override
   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return switch (state.get(AGE)) {
         case 0 -> SMALL_SHAPE;
         case 3 -> VoxelShapes.fullCube();
         default -> LARGE_SHAPE;
      };
   }

   @Override
   protected boolean hasRandomTicks(BlockState state) {
      return state.get(AGE) < 3;
   }

   @Override
   protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
      int i = state.get(AGE);
      if (i < 3 && random.nextInt(5) == 0 && world.getBaseLightLevel(pos.up(), 0) >= 9) {
         BlockState blockState = state.with(AGE, i + 1);
         world.setBlockState(pos, blockState, 2);
         world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(blockState));
      }
   }

   @Override
   protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, EntityCollisionHandler handler, boolean bl) {
      if (entity instanceof LivingEntity && entity.getType() != EntityType.FOX && entity.getType() != EntityType.BEE) {
         entity.slowMovement(state, new Vec3d(0.8F, 0.75, 0.8F));
         if (world instanceof ServerWorld serverWorld && state.get(AGE) != 0) {
            Vec3d vec3d = entity.isControlledByPlayer() ? entity.getMovement() : entity.getLastRenderPos().subtract(entity.getEntityPos());
            if (vec3d.horizontalLengthSquared() > 0.0) {
               double d = Math.abs(vec3d.getX());
               double e = Math.abs(vec3d.getZ());
               if (d >= 0.003F || e >= 0.003F) {
                  entity.damage(serverWorld, world.getDamageSources().sweetBerryBush(), 1.0F);
               }
            }
         }
      }
   }

   @Override
   protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
      int i = state.get(AGE);
      boolean bl = i == 3;
      return (ActionResult)(!bl && stack.isOf(Items.BONE_MEAL) ? ActionResult.PASS : super.onUseWithItem(stack, state, world, pos, player, hand, hit));
   }

   @Override
   protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
      if (state.get(AGE) > 1) {
         if (world instanceof ServerWorld serverWorld) {
            Block.generateBlockInteractLoot(
               serverWorld,
               LootTables.SWEET_BERRY_BUSH_HARVEST,
               state,
               world.getBlockEntity(pos),
               null,
               player,
               (worldx, stack) -> Block.dropStack(worldx, pos, stack)
            );
            serverWorld.playSound(
               null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1.0F, 0.8F + serverWorld.random.nextFloat() * 0.4F
            );
            BlockState blockState = state.with(AGE, 1);
            serverWorld.setBlockState(pos, blockState, 2);
            serverWorld.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, blockState));
         }

         return ActionResult.SUCCESS;
      } else {
         return super.onUse(state, world, pos, player, hit);
      }
   }

   @Override
   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(AGE);
   }

   @Override
   public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
      return state.get(AGE) < 3;
   }

   @Override
   public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
      return true;
   }

   @Override
   public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
      int i = Math.min(3, state.get(AGE) + 1);
      world.setBlockState(pos, state.with(AGE, i), 2);
   }
}
