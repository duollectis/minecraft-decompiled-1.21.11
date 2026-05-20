package net.minecraft.block.dispenser;

import java.util.List;
import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class EquippableDispenserBehavior extends ItemDispenserBehavior {
   public static final EquippableDispenserBehavior INSTANCE = new EquippableDispenserBehavior();

   @Override
   protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
      return tryEquip(pointer, stack) ? stack : super.dispenseSilently(pointer, stack);
   }

   public static boolean tryEquip(BlockPointer pointer, ItemStack stack) {
      BlockPos blockPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));
      List<LivingEntity> list = pointer.world().getEntitiesByClass(LivingEntity.class, new Box(blockPos), entity -> entity.canEquipFromDispenser(stack));
      if (list.isEmpty()) {
         return false;
      } else {
         LivingEntity livingEntity = list.getFirst();
         EquipmentSlot equipmentSlot = livingEntity.getPreferredEquipmentSlot(stack);
         ItemStack itemStack = stack.split(1);
         livingEntity.equipStack(equipmentSlot, itemStack);
         if (livingEntity instanceof MobEntity mobEntity) {
            mobEntity.setDropGuaranteed(equipmentSlot);
            mobEntity.setPersistent();
         }

         return true;
      }
   }
}
