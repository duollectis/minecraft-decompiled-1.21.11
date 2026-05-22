package net.minecraft.recipe.book;

/**
 * Хранит настройки книги рецептов игрока: состояние открытости и фильтрации
 * для каждого типа станка. Делегирует всю логику в {@link RecipeBookOptions}.
 */
public class RecipeBook {

	protected final RecipeBookOptions options = new RecipeBookOptions();

	public boolean isGuiOpen(RecipeBookType category) {
		return options.isGuiOpen(category);
	}

	public void setGuiOpen(RecipeBookType category, boolean open) {
		options.setGuiOpen(category, open);
	}

	public boolean isFilteringCraftable(RecipeBookType category) {
		return options.isFilteringCraftable(category);
	}

	public void setFilteringCraftable(RecipeBookType category, boolean filteringCraftable) {
		options.setFilteringCraftable(category, filteringCraftable);
	}

	public void setOptions(RecipeBookOptions newOptions) {
		options.copyFrom(newOptions);
	}

	public RecipeBookOptions getOptions() {
		return options;
	}

	public void setCategoryOptions(RecipeBookType category, boolean guiOpen, boolean filteringCraftable) {
		options.setGuiOpen(category, guiOpen);
		options.setFilteringCraftable(category, filteringCraftable);
	}
}
