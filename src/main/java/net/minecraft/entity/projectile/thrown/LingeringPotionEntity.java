package net.minecraft.entity.projectile.thrown;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class LingeringPotionEntity extends PotionEntity {
   public LingeringPotionEntity(EntityType<? extends LingeringPotionEntity> entityType, World world) {
      super(entityType, world);
   }

   public LingeringPotionEntity(World world, LivingEntity owner, ItemStack stack) {
      super(EntityType.LINGERING_POTION, world, owner, stack);
   }

   public LingeringPotionEntity(World world, double x, double y, double z, ItemStack stack) {
      super(EntityType.LINGERING_POTION, world, x, y, z, stack);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.LINGERING_POTION;
   }

   @Override
   public void spawnAreaEffectCloud(ServerWorld world, ItemStack stack, HitResult hitResult) {
      AreaEffectCloudEntity areaEffectCloudEntity = new AreaEffectCloudEntity(this.getEntityWorld(), this.getX(), this.getY(), this.getZ());
      if (this.getOwner() instanceof LivingEntity livingEntity) {
         areaEffectCloudEntity.setOwner(livingEntity);
      }

      areaEffectCloudEntity.setRadius(3.0F);
      areaEffectCloudEntity.setRadiusOnUse(-0.5F);
      areaEffectCloudEntity.setDuration(600);
      areaEffectCloudEntity.setWaitTime(10);
      areaEffectCloudEntity.setRadiusGrowth(-areaEffectCloudEntity.getRadius() / areaEffectCloudEntity.getDuration());
      areaEffectCloudEntity.copyComponentsFrom(stack);
      world.spawnEntity(areaEffectCloudEntity);
   }
}
