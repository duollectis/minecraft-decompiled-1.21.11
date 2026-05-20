package net.minecraft.recipe;

import java.util.Collections;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

public interface RecipeUnlocker {
   void setLastRecipe(@Nullable RecipeEntry<?> recipe);

   @Nullable RecipeEntry<?> getLastRecipe();

   default void unlockLastRecipe(PlayerEntity player, List<ItemStack> ingredients) {
      RecipeEntry<?> recipeEntry = this.getLastRecipe();
      if (recipeEntry != null) {
         player.onRecipeCrafted(recipeEntry, ingredients);
         if (!recipeEntry.value().isIgnoredInRecipeBook()) {
            player.unlockRecipes(Collections.singleton(recipeEntry));
            this.setLastRecipe(null);
         }
      }
   }

   default boolean shouldCraftRecipe(ServerPlayerEntity player, RecipeEntry<?> recipe) {
      if (!recipe.value().isIgnoredInRecipeBook()
         && player.getEntityWorld().getGameRules().getValue(GameRules.LIMITED_CRAFTING)
         && !player.getRecipeBook().isUnlocked(recipe.id())) {
         return false;
      } else {
         this.setLastRecipe(recipe);
         return true;
      }
   }
}
