package net.minecraft.inventory;

import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.jspecify.annotations.Nullable;

public class EnderChestInventory extends SimpleInventory {
   private @Nullable EnderChestBlockEntity activeBlockEntity;

   public EnderChestInventory() {
      super(27);
   }

   public void setActiveBlockEntity(EnderChestBlockEntity blockEntity) {
      this.activeBlockEntity = blockEntity;
   }

   public boolean isActiveBlockEntity(EnderChestBlockEntity blockEntity) {
      return this.activeBlockEntity == blockEntity;
   }

   public void readData(ReadView.TypedListReadView<StackWithSlot> list) {
      for (int i = 0; i < this.size(); i++) {
         this.setStack(i, ItemStack.EMPTY);
      }

      for (StackWithSlot stackWithSlot : list) {
         if (stackWithSlot.isValidSlot(this.size())) {
            this.setStack(stackWithSlot.slot(), stackWithSlot.stack());
         }
      }
   }

   public void writeData(WriteView.ListAppender<StackWithSlot> list) {
      for (int i = 0; i < this.size(); i++) {
         ItemStack itemStack = this.getStack(i);
         if (!itemStack.isEmpty()) {
            list.add(new StackWithSlot(i, itemStack));
         }
      }
   }

   @Override
   public boolean canPlayerUse(PlayerEntity player) {
      return this.activeBlockEntity != null && !this.activeBlockEntity.canPlayerUse(player) ? false : super.canPlayerUse(player);
   }

   @Override
   public void onOpen(ContainerUser user) {
      if (this.activeBlockEntity != null) {
         this.activeBlockEntity.onOpen(user);
      }

      super.onOpen(user);
   }

   @Override
   public void onClose(ContainerUser user) {
      if (this.activeBlockEntity != null) {
         this.activeBlockEntity.onClose(user);
      }

      super.onClose(user);
      this.activeBlockEntity = null;
   }
}
