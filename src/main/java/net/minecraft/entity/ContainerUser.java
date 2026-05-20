package net.minecraft.entity;

import net.minecraft.block.entity.ViewerCountManager;
import net.minecraft.util.math.BlockPos;

public interface ContainerUser {
   boolean isViewingContainerAt(ViewerCountManager viewerCountManager, BlockPos pos);

   double getContainerInteractionRange();

   default LivingEntity asLivingEntity() {
      if (this instanceof LivingEntity) {
         return (LivingEntity)this;
      } else {
         throw new IllegalStateException("A container user must be a LivingEntity");
      }
   }
}
