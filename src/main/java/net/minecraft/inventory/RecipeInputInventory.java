package net.minecraft.inventory;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeInputProvider;
import net.minecraft.recipe.input.CraftingRecipeInput;

public interface RecipeInputInventory extends Inventory, RecipeInputProvider {
   int getWidth();

   int getHeight();

   List<ItemStack> getHeldStacks();

   default CraftingRecipeInput createRecipeInput() {
      return this.createPositionedRecipeInput().input();
   }

   default CraftingRecipeInput.Positioned createPositionedRecipeInput() {
      return CraftingRecipeInput.createPositioned(this.getWidth(), this.getHeight(), this.getHeldStacks());
   }
}
