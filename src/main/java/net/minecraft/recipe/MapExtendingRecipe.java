package net.minecraft.recipe;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapPostProcessingComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

import java.util.Map;

/**
 * Рецепт расширения масштаба заполненной карты.
 * <p>
 * Паттерн 3×3: восемь листов бумаги вокруг заполненной карты.
 * Карта должна иметь масштаб менее 4 и не содержать маркеров исследования.
 * Результат помечается компонентом {@link MapPostProcessingComponent#SCALE}
 * для последующей обработки при следующем тике.
 */
public class MapExtendingRecipe extends ShapedRecipe {

	public MapExtendingRecipe(CraftingRecipeCategory category) {
		super(
			"",
			category,
			RawShapedRecipe.create(
				Map.of(
					'#', Ingredient.ofItem(Items.PAPER),
					'x', Ingredient.ofItem(Items.FILLED_MAP)
				),
				"###", "#x#", "###"
			),
			new ItemStack(Items.MAP)
		);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (!super.matches(input, world)) {
			return false;
		}

		ItemStack filledMap = findFilledMap(input);

		if (filledMap.isEmpty()) {
			return false;
		}

		MapState mapState = FilledMapItem.getMapState(filledMap, world);

		if (mapState == null) {
			return false;
		}

		return !mapState.hasExplorationMapDecoration() && mapState.scale < 4;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
		ItemStack result = findFilledMap(input).copyWithCount(1);
		result.set(DataComponentTypes.MAP_POST_PROCESSING, MapPostProcessingComponent.SCALE);
		return result;
	}

	private static ItemStack findFilledMap(CraftingRecipeInput input) {
		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.contains(DataComponentTypes.MAP_ID)) {
				return stack;
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public boolean isIgnoredInRecipeBook() {
		return true;
	}

	@Override
	public RecipeSerializer<MapExtendingRecipe> getSerializer() {
		return RecipeSerializer.MAP_EXTENDING;
	}
}
