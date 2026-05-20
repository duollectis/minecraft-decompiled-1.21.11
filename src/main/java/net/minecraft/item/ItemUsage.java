package net.minecraft.item;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class ItemUsage {
   public static ActionResult consumeHeldItem(World world, PlayerEntity player, Hand hand) {
      player.setCurrentHand(hand);
      return ActionResult.CONSUME;
   }

   public static ItemStack exchangeStack(ItemStack inputStack, PlayerEntity player, ItemStack outputStack, boolean creativeOverride) {
      boolean bl = player.isInCreativeMode();
      if (creativeOverride && bl) {
         if (!player.getInventory().contains(outputStack)) {
            player.getInventory().insertStack(outputStack);
         }

         return inputStack;
      } else {
         inputStack.decrementUnlessCreative(1, player);
         if (inputStack.isEmpty()) {
            return outputStack;
         } else {
            if (!player.getInventory().insertStack(outputStack)) {
               player.dropItem(outputStack, false);
            }

            return inputStack;
         }
      }
   }

   public static ItemStack exchangeStack(ItemStack inputStack, PlayerEntity player, ItemStack outputStack) {
      return exchangeStack(inputStack, player, outputStack, true);
   }

   public static void spawnItemContents(ItemEntity itemEntity, Iterable<ItemStack> contents) {
      World world = itemEntity.getEntityWorld();
      if (!world.isClient()) {
         contents.forEach(stack -> world.spawnEntity(new ItemEntity(world, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), stack)));
      }
   }
}
