package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.entity.CollisionEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

public class PowderSnowBlock extends Block implements FluidDrainable {
   public static final MapCodec<PowderSnowBlock> CODEC = createCodec(PowderSnowBlock::new);
   private static final float field_31216 = 0.083333336F;
   private static final float HORIZONTAL_MOVEMENT_MULTIPLIER = 0.9F;
   private static final float VERTICAL_MOVEMENT_MULTIPLIER = 1.5F;
   private static final float field_31219 = 2.5F;
   private static final VoxelShape FALLING_SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 0.9F, 1.0);
   private static final double field_36189 = 4.0;
   private static final double SMALL_FALL_SOUND_MAX_DISTANCE = 7.0;

   @Override
   public MapCodec<PowderSnowBlock> getCodec() {
      return CODEC;
   }

   public PowderSnowBlock(AbstractBlock.Settings settings) {
      super(settings);
   }

   @Override
   protected boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
      return stateFrom.isOf(this) ? true : super.isSideInvisible(state, stateFrom, direction);
   }

   @Override
   protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, EntityCollisionHandler handler, boolean bl) {
      if (!(entity instanceof LivingEntity) || entity.getBlockStateAtPos().isOf(this)) {
         entity.slowMovement(state, new Vec3d(0.9F, 1.5, 0.9F));
         if (world.isClient()) {
            Random random = world.getRandom();
            boolean bl2 = entity.lastRenderX != entity.getX() || entity.lastRenderZ != entity.getZ();
            if (bl2 && random.nextBoolean()) {
               world.addParticleClient(
                  ParticleTypes.SNOWFLAKE,
                  entity.getX(),
                  pos.getY() + 1,
                  entity.getZ(),
                  MathHelper.nextBetween(random, -1.0F, 1.0F) * 0.083333336F,
                  0.05F,
                  MathHelper.nextBetween(random, -1.0F, 1.0F) * 0.083333336F
               );
            }
         }
      }

      BlockPos blockPos = pos.toImmutable();
      handler.addPreCallback(
         CollisionEvent.EXTINGUISH,
         entityx -> {
            if (world instanceof ServerWorld serverWorld
               && entityx.isOnFire()
               && (serverWorld.getGameRules().getValue(GameRules.DO_MOB_GRIEFING) || entityx instanceof PlayerEntity)
               && entityx.canModifyAt(serverWorld, blockPos)) {
               world.breakBlock(blockPos, false);
            }
         }
      );
      handler.addEvent(CollisionEvent.FREEZE);
      handler.addEvent(CollisionEvent.EXTINGUISH);
   }

   @Override
   public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
      if (!(fallDistance < 4.0) && entity instanceof LivingEntity livingEntity) {
         LivingEntity.FallSounds fallSounds = livingEntity.getFallSounds();
         SoundEvent soundEvent = fallDistance < 7.0 ? fallSounds.small() : fallSounds.big();
         entity.playSound(soundEvent, 1.0F, 1.0F);
      }
   }

   @Override
   protected VoxelShape getInsideCollisionShape(BlockState state, BlockView world, BlockPos pos, Entity entity) {
      VoxelShape voxelShape = this.getCollisionShape(state, world, pos, ShapeContext.of(entity));
      return voxelShape.isEmpty() ? VoxelShapes.fullCube() : voxelShape;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      if (!context.isPlacement() && context instanceof EntityShapeContext entityShapeContext) {
         Entity entity = entityShapeContext.getEntity();
         if (entity != null) {
            if (entity.fallDistance > 2.5) {
               return FALLING_SHAPE;
            }

            boolean bl = entity instanceof FallingBlockEntity;
            if (bl || canWalkOnPowderSnow(entity) && context.isAbove(VoxelShapes.fullCube(), pos, false) && !context.isDescending()) {
               return super.getCollisionShape(state, world, pos, context);
            }
         }
      }

      return VoxelShapes.empty();
   }

   @Override
   protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return VoxelShapes.empty();
   }

   public static boolean canWalkOnPowderSnow(Entity entity) {
      if (entity.getType().isIn(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)) {
         return true;
      } else {
         return entity instanceof LivingEntity ? ((LivingEntity)entity).getEquippedStack(EquipmentSlot.FEET).isOf(Items.LEATHER_BOOTS) : false;
      }
   }

   @Override
   public ItemStack tryDrainFluid(@Nullable LivingEntity drainer, WorldAccess world, BlockPos pos, BlockState state) {
      world.setBlockState(pos, Blocks.AIR.getDefaultState(), 11);
      if (!world.isClient()) {
         world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
      }

      return new ItemStack(Items.POWDER_SNOW_BUCKET);
   }

   @Override
   public Optional<SoundEvent> getBucketFillSound() {
      return Optional.of(SoundEvents.ITEM_BUCKET_FILL_POWDER_SNOW);
   }

   @Override
   protected boolean canPathfindThrough(BlockState state, NavigationType type) {
      return true;
   }
}
