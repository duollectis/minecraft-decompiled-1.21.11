package net.minecraft.recipe;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

import java.util.Map;

/**
 * Рецепт крафта звезды фейерверка.
 * <p>
 * Обязательные ингредиенты: порох (ровно один) и хотя бы один краситель.
 * Опциональные модификаторы (каждый не более одного):
 * <ul>
 *   <li>Форма взрыва — предметы из {@link #SHAPE_MODIFIER_MAP}</li>
 *   <li>Эффект «след» — алмаз</li>
 *   <li>Эффект «мерцание» — светящийся камень</li>
 * </ul>
 */
public class FireworkStarRecipe extends SpecialCraftingRecipe {

	private static final Map<Item, FireworkExplosionComponent.Type> SHAPE_MODIFIER_MAP = Map.of(
		Items.FIRE_CHARGE, FireworkExplosionComponent.Type.LARGE_BALL,
		Items.FEATHER, FireworkExplosionComponent.Type.BURST,
		Items.GOLD_NUGGET, FireworkExplosionComponent.Type.STAR,
		Items.SKELETON_SKULL, FireworkExplosionComponent.Type.CREEPER,
		Items.WITHER_SKELETON_SKULL, FireworkExplosionComponent.Type.CREEPER,
		Items.CREEPER_HEAD, FireworkExplosionComponent.Type.CREEPER,
		Items.PLAYER_HEAD, FireworkExplosionComponent.Type.CREEPER,
		Items.DRAGON_HEAD, FireworkExplosionComponent.Type.CREEPER,
		Items.ZOMBIE_HEAD, FireworkExplosionComponent.Type.CREEPER,
		Items.PIGLIN_HEAD, FireworkExplosionComponent.Type.CREEPER
	);

	private static final Ingredient TRAIL_MODIFIER = Ingredient.ofItem(Items.DIAMOND);
	private static final Ingredient FLICKER_MODIFIER = Ingredient.ofItem(Items.GLOWSTONE_DUST);
	private static final Ingredient GUNPOWDER = Ingredient.ofItem(Items.GUNPOWDER);

	public FireworkStarRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (input.getStackCount() < 2) {
			return false;
		}

		boolean hasGunpowder = false;
		boolean hasDye = false;
		boolean hasShapeModifier = false;
		boolean hasTrail = false;
		boolean hasFlicker = false;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (SHAPE_MODIFIER_MAP.containsKey(stack.getItem())) {
				if (hasShapeModifier) {
					return false;
				}

				hasShapeModifier = true;
			} else if (FLICKER_MODIFIER.test(stack)) {
				if (hasFlicker) {
					return false;
				}

				hasFlicker = true;
			} else if (TRAIL_MODIFIER.test(stack)) {
				if (hasTrail) {
					return false;
				}

				hasTrail = true;
			} else if (GUNPOWDER.test(stack)) {
				if (hasGunpowder) {
					return false;
				}

				hasGunpowder = true;
			} else {
				if (!(stack.getItem() instanceof DyeItem)) {
					return false;
				}

				hasDye = true;
			}
		}

		return hasGunpowder && hasDye;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
		FireworkExplosionComponent.Type explosionShape = FireworkExplosionComponent.Type.SMALL_BALL;
		boolean hasTrail = false;
		boolean hasFlicker = false;
		IntList colors = new IntArrayList();

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			FireworkExplosionComponent.Type shapeOverride = SHAPE_MODIFIER_MAP.get(stack.getItem());

			if (shapeOverride != null) {
				explosionShape = shapeOverride;
			} else if (FLICKER_MODIFIER.test(stack)) {
				hasFlicker = true;
			} else if (TRAIL_MODIFIER.test(stack)) {
				hasTrail = true;
			} else if (stack.getItem() instanceof DyeItem dyeItem) {
				colors.add(dyeItem.getColor().getFireworkColor());
			}
		}

		ItemStack result = new ItemStack(Items.FIREWORK_STAR);
		result.set(
			DataComponentTypes.FIREWORK_EXPLOSION,
			new FireworkExplosionComponent(explosionShape, colors, IntList.of(), hasTrail, hasFlicker)
		);
		return result;
	}

	@Override
	public RecipeSerializer<FireworkStarRecipe> getSerializer() {
		return RecipeSerializer.FIREWORK_STAR;
	}
}
