package net.minecraft.recipe;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Рецепт крафта фейерверочной ракеты.
 * <p>
 * Требует ровно один лист бумаги и от 1 до {@link #MAX_GUNPOWDER_COUNT} пороха.
 * Опционально принимает любое количество звёзд фейерверка.
 * Результат — стак из {@link #ROCKET_OUTPUT_COUNT} ракет с заданной длительностью.
 */
public class FireworkRocketRecipe extends SpecialCraftingRecipe {

	private static final int MAX_GUNPOWDER_COUNT = 3;
	private static final int ROCKET_OUTPUT_COUNT = 3;

	private static final Ingredient PAPER = Ingredient.ofItem(Items.PAPER);
	private static final Ingredient GUNPOWDER = Ingredient.ofItem(Items.GUNPOWDER);
	private static final Ingredient FIREWORK_STAR = Ingredient.ofItem(Items.FIREWORK_STAR);

	public FireworkRocketRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (input.getStackCount() < 2) {
			return false;
		}

		boolean hasPaper = false;
		int gunpowderCount = 0;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (PAPER.test(stack)) {
				if (hasPaper) {
					return false;
				}

				hasPaper = true;
			} else if (GUNPOWDER.test(stack)) {
				if (++gunpowderCount > MAX_GUNPOWDER_COUNT) {
					return false;
				}
			} else if (!FIREWORK_STAR.test(stack)) {
				return false;
			}
		}

		return hasPaper && gunpowderCount >= 1;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
		List<FireworkExplosionComponent> explosions = new ArrayList<>();
		int gunpowderCount = 0;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (GUNPOWDER.test(stack)) {
				gunpowderCount++;
			} else if (FIREWORK_STAR.test(stack)) {
				FireworkExplosionComponent explosion = stack.get(DataComponentTypes.FIREWORK_EXPLOSION);

				if (explosion != null) {
					explosions.add(explosion);
				}
			}
		}

		ItemStack result = new ItemStack(Items.FIREWORK_ROCKET, ROCKET_OUTPUT_COUNT);
		result.set(DataComponentTypes.FIREWORKS, new FireworksComponent(gunpowderCount, explosions));
		return result;
	}

	@Override
	public RecipeSerializer<FireworkRocketRecipe> getSerializer() {
		return RecipeSerializer.FIREWORK_ROCKET;
	}
}
