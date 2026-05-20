package net.minecraft.block.entity;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public abstract class ViewerCountManager {
   private static final int SCHEDULE_TICK_DELAY = 5;
   private int viewerCount;
   private double maxBlockInteractionRange;

   protected abstract void onContainerOpen(World world, BlockPos pos, BlockState state);

   protected abstract void onContainerClose(World world, BlockPos pos, BlockState state);

   protected abstract void onViewerCountUpdate(World world, BlockPos pos, BlockState state, int oldViewerCount, int newViewerCount);

   public abstract boolean isPlayerViewing(PlayerEntity player);

   public void openContainer(LivingEntity user, World world, BlockPos pos, BlockState state, double userInteractionRange) {
      int i = this.viewerCount++;
      if (i == 0) {
         this.onContainerOpen(world, pos, state);
         world.emitGameEvent(user, GameEvent.CONTAINER_OPEN, pos);
         scheduleBlockTick(world, pos, state);
      }

      this.onViewerCountUpdate(world, pos, state, i, this.viewerCount);
      this.maxBlockInteractionRange = Math.max(userInteractionRange, this.maxBlockInteractionRange);
   }

   public void closeContainer(LivingEntity user, World world, BlockPos pos, BlockState state) {
      int i = this.viewerCount--;
      if (this.viewerCount == 0) {
         this.onContainerClose(world, pos, state);
         world.emitGameEvent(user, GameEvent.CONTAINER_CLOSE, pos);
         this.maxBlockInteractionRange = 0.0;
      }

      this.onViewerCountUpdate(world, pos, state, i, this.viewerCount);
   }

   public List<ContainerUser> getViewingUsers(World world, BlockPos pos) {
      double d = this.maxBlockInteractionRange + 4.0;
      Box box = new Box(pos).expand(d);
      return world.getOtherEntities((Entity)null, box, entity -> this.hasViewingUsers(entity, pos))
         .stream()
         .map(entity -> (ContainerUser)entity)
         .collect(Collectors.toList());
   }

   private boolean hasViewingUsers(Entity entity, BlockPos blockPos) {
      return entity instanceof ContainerUser containerUser && !containerUser.asLivingEntity().isSpectator()
         ? containerUser.isViewingContainerAt(this, blockPos)
         : false;
   }

   public void updateViewerCount(World world, BlockPos pos, BlockState state) {
      List<ContainerUser> list = this.getViewingUsers(world, pos);
      this.maxBlockInteractionRange = 0.0;

      for (ContainerUser containerUser : list) {
         this.maxBlockInteractionRange = Math.max(containerUser.getContainerInteractionRange(), this.maxBlockInteractionRange);
      }

      int i = list.size();
      int j = this.viewerCount;
      if (j != i) {
         boolean bl = i != 0;
         boolean bl2 = j != 0;
         if (bl && !bl2) {
            this.onContainerOpen(world, pos, state);
            world.emitGameEvent(null, GameEvent.CONTAINER_OPEN, pos);
         } else if (!bl) {
            this.onContainerClose(world, pos, state);
            world.emitGameEvent(null, GameEvent.CONTAINER_CLOSE, pos);
         }

         this.viewerCount = i;
      }

      this.onViewerCountUpdate(world, pos, state, j, i);
      if (i > 0) {
         scheduleBlockTick(world, pos, state);
      }
   }

   public int getViewerCount() {
      return this.viewerCount;
   }

   private static void scheduleBlockTick(World world, BlockPos pos, BlockState state) {
      world.scheduleBlockTick(pos, state.getBlock(), 5);
   }
}
