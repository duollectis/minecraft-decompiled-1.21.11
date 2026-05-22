package net.minecraft.item;

import com.mojang.logging.LogUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Предмет «Книга знаний». При использовании разблокирует игроку список рецептов,
 * хранящихся в компоненте {@link DataComponentTypes#RECIPES}. Предмет расходуется
 * при активации.
 */
public class KnowledgeBookItem extends Item {

	private static final Logger LOGGER = LogUtils.getLogger();

	public KnowledgeBookItem(Item.Settings settings) {
		super(settings);
	}

	/**
	 * Разблокирует все рецепты из компонента {@link DataComponentTypes#RECIPES}.
	 * Если хотя бы один ключ рецепта не найден в менеджере рецептов — логирует ошибку
	 * и возвращает {@link ActionResult#FAIL}, не разблокируя ничего.
	 */
	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		List<RegistryKey<Recipe<?>>> recipeKeys = stack.getOrDefault(DataComponentTypes.RECIPES, List.of());

		stack.decrementUnlessCreative(1, user);

		if (recipeKeys.isEmpty()) {
			return ActionResult.FAIL;
		}

		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}

		ServerRecipeManager recipeManager = world.getServer().getRecipeManager();
		List<RecipeEntry<?>> recipes = new ArrayList<>(recipeKeys.size());

		for (RegistryKey<Recipe<?>> recipeKey : recipeKeys) {
			Optional<RecipeEntry<?>> recipeEntry = recipeManager.get(recipeKey);

			if (recipeEntry.isEmpty()) {
				LOGGER.error("Invalid recipe: {}", recipeKey);
				return ActionResult.FAIL;
			}

			recipes.add(recipeEntry.get());
		}

		user.unlockRecipes(recipes);
		user.incrementStat(Stats.USED.getOrCreateStat(this));

		return ActionResult.SUCCESS;
	}
}
