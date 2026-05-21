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
 * {@code TransmuteRecipe}.
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
	 * Matches.
	 *
	 * @param craftingRecipeInput crafting recipe input
	 * @param world world
	 *
	 * @return boolean — результат операции
	 */
	public boolean matches(CraftingRecipeInput craftingRecipeInput, World world) {
		if (craftingRecipeInput.getStackCount() != 2) {
			return false;
		}
		else {
			boolean bl = false;
			boolean bl2 = false;

			for (int i = 0; i < craftingRecipeInput.size(); i++) {
				ItemStack itemStack = craftingRecipeInput.getStackInSlot(i);
				if (!itemStack.isEmpty()) {
					if (!bl && this.input.test(itemStack)) {
						if (this.result.isEqualToResult(itemStack)) {
							return false;
						}

						bl = true;
					}
					else {
						if (bl2 || !this.material.test(itemStack)) {
							return false;
						}

						bl2 = true;
					}
				}
			}

			return bl && bl2;
		}
	}

	/**
	 * Craft.
	 *
	 * @param craftingRecipeInput crafting recipe input
	 * @param wrapperLookup wrapper lookup
	 *
	 * @return ItemStack — результат операции
	 */
	public ItemStack craft(CraftingRecipeInput craftingRecipeInput, RegistryWrapper.WrapperLookup wrapperLookup) {
		for (int i = 0; i < craftingRecipeInput.size(); i++) {
			ItemStack itemStack = craftingRecipeInput.getStackInSlot(i);
			if (!itemStack.isEmpty() && this.input.test(itemStack)) {
				return this.result.apply(itemStack);
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public List<RecipeDisplay> getDisplays() {
		return List.of(
				new ShapelessCraftingRecipeDisplay(
						List.of(this.input.toDisplay(), this.material.toDisplay()),
						this.result.createSlotDisplay(),
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
		return this.group;
	}

	@Override
	public IngredientPlacement getIngredientPlacement() {
		if (this.ingredientPlacement == null) {
			this.ingredientPlacement = IngredientPlacement.forShapeless(List.of(this.input, this.material));
		}

		return this.ingredientPlacement;
	}

	@Override
	public CraftingRecipeCategory getCategory() {
		return this.category;
	}

	/**
	 * {@code Serializer}.
	 */
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
				                    )
				                    .apply(instance, TransmuteRecipe::new)
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
