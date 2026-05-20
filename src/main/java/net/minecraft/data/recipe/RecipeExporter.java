package net.minecraft.data.recipe;

import net.fabricmc.fabric.api.datagen.v1.recipe.FabricRecipeExporter;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;
import org.jspecify.annotations.Nullable;

public interface RecipeExporter extends FabricRecipeExporter {
   void accept(RegistryKey<Recipe<?>> key, Recipe<?> recipe, @Nullable AdvancementEntry advancement);

   Advancement.Builder getAdvancementBuilder();

   void addRootAdvancement();
}
