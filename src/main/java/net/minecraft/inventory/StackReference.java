package net.minecraft.inventory;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface StackReference {
   ItemStack get();

   boolean set(ItemStack stack);

   static StackReference of(Supplier<ItemStack> getter, Consumer<ItemStack> setter) {
      return new StackReference() {
         @Override
         public ItemStack get() {
            return getter.get();
         }

         @Override
         public boolean set(ItemStack stack) {
            setter.accept(stack);
            return true;
         }
      };
   }

   static StackReference of(LivingEntity entity, EquipmentSlot slot, Predicate<ItemStack> filter) {
      return new StackReference() {
         @Override
         public ItemStack get() {
            return entity.getEquippedStack(slot);
         }

         @Override
         public boolean set(ItemStack stack) {
            if (!filter.test(stack)) {
               return false;
            } else {
               entity.equipStack(slot, stack);
               return true;
            }
         }
      };
   }

   static StackReference of(LivingEntity entity, EquipmentSlot slot) {
      return of(entity, slot, stack -> true);
   }

   static StackReference of(List<ItemStack> stacks, int index) {
      return new StackReference() {
         @Override
         public ItemStack get() {
            return stacks.get(index);
         }

         @Override
         public boolean set(ItemStack stack) {
            stacks.set(index, stack);
            return true;
         }
      };
   }
}
