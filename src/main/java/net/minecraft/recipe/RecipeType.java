package net.minecraft.recipe;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Реестр типов рецептов. Каждый тип соответствует определённому блоку-крафтеру
 * (верстак, печь, точильный камень и т.д.).
 *
 * @param <T> конкретный подтип рецепта
 */
public interface RecipeType<T extends Recipe<?>> {

	RecipeType<CraftingRecipe> CRAFTING = register("crafting");
	RecipeType<SmeltingRecipe> SMELTING = register("smelting");
	RecipeType<BlastingRecipe> BLASTING = register("blasting");
	RecipeType<SmokingRecipe> SMOKING = register("smoking");
	RecipeType<CampfireCookingRecipe> CAMPFIRE_COOKING = register("campfire_cooking");
	RecipeType<StonecuttingRecipe> STONECUTTING = register("stonecutting");
	RecipeType<SmithingRecipe> SMITHING = register("smithing");

	/**
	 * Регистрирует новый тип рецепта в реестре.
	 *
	 * @param id строковый идентификатор типа
	 * @param <T> тип рецепта
	 * @return зарегистрированный экземпляр типа
	 */
	static <T extends Recipe<?>> RecipeType<T> register(String id) {
		return Registry.register(
				Registries.RECIPE_TYPE,
				Identifier.ofVanilla(id),
				new RecipeType<T>() {
					@Override
					public String toString() {
						return id;
					}
				}
		);
	}
}
