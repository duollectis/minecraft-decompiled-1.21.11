package net.minecraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Рецепт трансмутации: преобразует предмет типа {@code input} в другой тип,
 * сохраняя компоненты оригинала. Требует ровно 2 предмета в сетке:
 * один — исходный предмет (input), второй — материал-катализатор (material).
 * Результат не должен совпадать с исходным предметом (защита от бесполезного крафта).
 */
public class TransmuteRecipe implements CraftingRecipe {

	final String group;
	final CraftingRecipeCategory category;
	final Ingredient input;
	final Ingredient material;
	final TransmuteRecipeResult result;
	private @Nullable IngredientPlacement ingredientPlacement;

	public TransmuteRecipe(
		String group,
		CraftingRecipeCategory category,
		Ingredient input,
		Ingredient material,
		TransmuteRecipeResult result
	) {
		this.group = group;
		this.category = category;
		this.input = input;
		this.material = material;
		this.result = result;
	}

	/**
	 * Проверяет совпадение: ровно 2 предмета, один соответствует {@code input},
	 * второй — {@code material}. Результат не должен быть идентичен исходному предмету.
	 */
	@Override
	public boolean matches(CraftingRecipeInput craftingInput, World world) {
		if (craftingInput.getStackCount() != 2) {
			return false;
		}

		boolean hasInput = false;
		boolean hasMaterial = false;

		for (int slotIndex = 0; slotIndex < craftingInput.size(); slotIndex++) {
			ItemStack stack = craftingInput.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (!hasInput && input.test(stack)) {
				if (result.isEqualToResult(stack)) {
					return false;
				}

				hasInput = true;
			} else {
				if (hasMaterial || !material.test(stack)) {
					return false;
				}

				hasMaterial = true;
			}
		}

		return hasInput && hasMaterial;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput craftingInput, RegistryWrapper.WrapperLookup wrapperLookup) {
		for (int slotIndex = 0; slotIndex < craftingInput.size(); slotIndex++) {
			ItemStack stack = craftingInput.getStackInSlot(slotIndex);

			if (!stack.isEmpty() && input.test(stack)) {
				return result.apply(stack);
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public List<RecipeDisplay> getDisplays() {
		return List.of(
			new ShapelessCraftingRecipeDisplay(
				List.of(input.toDisplay(), material.toDisplay()),
				result.createSlotDisplay(),
				new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
			)
		);
	}

	@Override
	public RecipeSerializer<TransmuteRecipe> getSerializer() {
		return RecipeSerializer.CRAFTING_TRANSMUTE;
	}

	@Override
	public String getGroup() {
		return group;
	}

	@Override
	public IngredientPlacement getIngredientPlacement() {
		if (ingredientPlacement == null) {
			ingredientPlacement = IngredientPlacement.forShapeless(List.of(input, material));
		}

		return ingredientPlacement;
	}

	@Override
	public CraftingRecipeCategory getCategory() {
		return category;
	}

	public static class Serializer implements RecipeSerializer<TransmuteRecipe> {

		private static final MapCodec<TransmuteRecipe> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Codec.STRING.optionalFieldOf("group", "").forGetter(recipe -> recipe.group),
				CraftingRecipeCategory.CODEC
					.fieldOf("category")
					.orElse(CraftingRecipeCategory.MISC)
					.forGetter(recipe -> recipe.category),
				Ingredient.CODEC.fieldOf("input").forGetter(recipe -> recipe.input),
				Ingredient.CODEC.fieldOf("material").forGetter(recipe -> recipe.material),
				TransmuteRecipeResult.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
			).apply(instance, TransmuteRecipe::new)
		);

		public static final PacketCodec<RegistryByteBuf, TransmuteRecipe> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.STRING,
			recipe -> recipe.group,
			CraftingRecipeCategory.PACKET_CODEC,
			recipe -> recipe.category,
			Ingredient.PACKET_CODEC,
			recipe -> recipe.input,
			Ingredient.PACKET_CODEC,
			recipe -> recipe.material,
			TransmuteRecipeResult.PACKET_CODEC,
			recipe -> recipe.result,
			TransmuteRecipe::new
		);

		@Override
		public MapCodec<TransmuteRecipe> codec() {
			return CODEC;
		}

		@Override
		public PacketCodec<RegistryByteBuf, TransmuteRecipe> packetCodec() {
			return PACKET_CODEC;
		}
	}
}
