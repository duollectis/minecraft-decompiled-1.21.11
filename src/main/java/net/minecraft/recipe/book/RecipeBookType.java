package net.minecraft.recipe.book;

/**
 * Тип книги рецептов, соответствующий конкретному типу станка.
 * Используется как ключ в {@link RecipeBookOptions} для хранения состояния UI.
 */
public enum RecipeBookType {
	CRAFTING,
	FURNACE,
	BLAST_FURNACE,
	SMOKER
}
