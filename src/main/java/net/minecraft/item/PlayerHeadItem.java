package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;

public class PlayerHeadItem extends VerticallyAttachableBlockItem {
   public PlayerHeadItem(Block block, Block wallBlock, Item.Settings settings) {
      super(block, wallBlock, Direction.DOWN, settings);
   }

   @Override
   public Text getName(ItemStack stack) {
      ProfileComponent profileComponent = stack.get(DataComponentTypes.PROFILE);
      return (Text)(profileComponent != null && profileComponent.getName().isPresent()
         ? Text.translatable(this.translationKey + ".named", profileComponent.getName().get())
         : super.getName(stack));
   }
}
