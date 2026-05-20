package net.minecraft.block;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.CollisionView;
import org.jspecify.annotations.Nullable;

public interface ShapeContext {
   static ShapeContext absent() {
      return EntityShapeContext.Absent.INSTANCE;
   }

   static ShapeContext absentTreatingFluidAsCube() {
      return EntityShapeContext.Absent.TREAT_FLUID_AS_CUBE;
   }

   static ShapeContext of(Entity entity) {
      return (ShapeContext)(switch (entity) {
         case AbstractMinecartEntity abstractMinecartEntity -> AbstractMinecartEntity.areMinecartImprovementsEnabled(abstractMinecartEntity.getEntityWorld())
            ? new ExperimentalMinecartShapeContext(abstractMinecartEntity, false)
            : new EntityShapeContext(entity, false, false);
         default -> new EntityShapeContext(entity, false, false);
      });
   }

   static ShapeContext of(Entity entity, boolean shouldTreatFluidAsCube) {
      return new EntityShapeContext(entity, shouldTreatFluidAsCube, false);
   }

   static ShapeContext ofPlacement(@Nullable PlayerEntity player) {
      return new EntityShapeContext(
         player != null ? player.isDescending() : false,
         true,
         player != null ? player.getY() : -Double.MAX_VALUE,
         player instanceof LivingEntity ? player.getMainHandStack() : ItemStack.EMPTY,
         false,
         player
      );
   }

   static ShapeContext ofCollision(@Nullable Entity entity, double y) {
      return new EntityShapeContext(
         entity != null ? entity.isDescending() : false,
         true,
         entity != null ? y : -Double.MAX_VALUE,
         entity instanceof LivingEntity livingEntity ? livingEntity.getMainHandStack() : ItemStack.EMPTY,
         false,
         entity
      );
   }

   boolean isDescending();

   boolean isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue);

   boolean isHolding(Item item);

   boolean shouldTreatFluidAsCube();

   boolean canWalkOnFluid(FluidState stateAbove, FluidState state);

   VoxelShape getCollisionShape(BlockState state, CollisionView world, BlockPos pos);

   default boolean isPlacement() {
      return false;
   }
}
