package net.minecraft.screen;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.recipe.book.RecipeBookType;
import net.minecraft.server.world.ServerWorld;

/**
 * Базовый класс для обработчиков экранов, поддерживающих рецепты (крафт, плавка).
 * <p>
 * Предоставляет контракт для заполнения слотов ввода по рецепту и интеграции
 * с книгой рецептов. Конкретные реализации — {@link AbstractCraftingScreenHandler}
 * и {@link AbstractFurnaceScreenHandler}.
 */
public abstract class AbstractRecipeScreenHandler extends ScreenHandler {

	public AbstractRecipeScreenHandler(ScreenHandlerType<?> type, int syncId) {
		super(type, syncId);
	}

	/**
	 * Заполняет слоты ввода ингредиентами из указанного рецепта.
	 *
	 * @param craftAll    если {@code true} — заполнить максимально возможное количество
	 * @param creative    режим творчества (не расходует предметы из инвентаря)
	 * @param recipe      рецепт для заполнения
	 * @param world       серверный мир
	 * @param inventory   инвентарь игрока как источник ингредиентов
	 * @return действие после заполнения (ничего или показать призрачный рецепт)
	 */
	public abstract PostFillAction fillInputSlots(
			boolean craftAll,
			boolean creative,
			RecipeEntry<?> recipe,
			ServerWorld world,
			PlayerInventory inventory
	);

	public abstract void populateRecipeFinder(RecipeFinder finder);

	public abstract RecipeBookType getCategory();

	/**
	 * Действие, выполняемое после заполнения слотов ввода рецептом.
	 */
	public enum PostFillAction {
		NOTHING,
		PLACE_GHOST_RECIPE
	}
}
