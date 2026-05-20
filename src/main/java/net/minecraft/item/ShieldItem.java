package net.minecraft.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;

public class ShieldItem extends Item {
   public ShieldItem(Item.Settings settings) {
      super(settings);
   }

   @Override
   public Text getName(ItemStack stack) {
      DyeColor dyeColor = stack.get(DataComponentTypes.BASE_COLOR);
      return (Text)(dyeColor != null ? Text.translatable(this.translationKey + "." + dyeColor.getId()) : super.getName(stack));
   }
}
