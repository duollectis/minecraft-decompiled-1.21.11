package net.minecraft.entity.projectile.thrown;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ExperienceBottleEntity extends ThrownItemEntity {
   public ExperienceBottleEntity(EntityType<? extends ExperienceBottleEntity> entityType, World world) {
      super(entityType, world);
   }

   public ExperienceBottleEntity(World world, LivingEntity owner, ItemStack stack) {
      super(EntityType.EXPERIENCE_BOTTLE, owner, world, stack);
   }

   public ExperienceBottleEntity(World world, double x, double y, double z, ItemStack stack) {
      super(EntityType.EXPERIENCE_BOTTLE, x, y, z, world, stack);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.EXPERIENCE_BOTTLE;
   }

   @Override
   protected double getGravity() {
      return 0.07;
   }

   @Override
   protected void onCollision(HitResult hitResult) {
      super.onCollision(hitResult);
      if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
         serverWorld.syncWorldEvent(2002, this.getBlockPos(), -13083194);
         int i = 3 + serverWorld.random.nextInt(5) + serverWorld.random.nextInt(5);
         if (hitResult instanceof BlockHitResult blockHitResult) {
            Vec3d vec3d = blockHitResult.getSide().getDoubleVector();
            ExperienceOrbEntity.spawn(serverWorld, hitResult.getPos(), vec3d, i);
         } else {
            ExperienceOrbEntity.spawn(serverWorld, hitResult.getPos(), this.getVelocity().multiply(-1.0), i);
         }

         this.discard();
      }
   }
}
