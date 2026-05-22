package net.minecraft.recipe;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Реализуется экранами крафта для отслеживания последнего использованного рецепта
 * и разблокировки его в книге рецептов игрока.
 */
public interface RecipeUnlocker {

	void setLastRecipe(@Nullable RecipeEntry<?> recipe);

	@Nullable RecipeEntry<?> getLastRecipe();

	default void unlockLastRecipe(PlayerEntity player, List<ItemStack> ingredients) {
		RecipeEntry<?> lastRecipe = getLastRecipe();

		if (lastRecipe == null) {
			return;
		}

		player.onRecipeCrafted(lastRecipe, ingredients);

		if (!lastRecipe.value().isIgnoredInRecipeBook()) {
			player.unlockRecipes(Collections.singleton(lastRecipe));
			setLastRecipe(null);
		}
	}

	/**
	 * Проверяет право игрока на крафт рецепта с учётом правила {@code LIMITED_CRAFTING}.
	 * Если рецепт разрешён — сохраняет его как последний использованный.
	 */
	default boolean shouldCraftRecipe(ServerPlayerEntity player, RecipeEntry<?> recipe) {
		if (!recipe.value().isIgnoredInRecipeBook()
			&& player.getEntityWorld().getGameRules().getValue(GameRules.LIMITED_CRAFTING)
			&& !player.getRecipeBook().isUnlocked(recipe.id())
		) {
			return false;
		}

		setLastRecipe(recipe);

		return true;
	}
}
