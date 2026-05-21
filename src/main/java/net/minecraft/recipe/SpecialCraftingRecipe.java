package net.minecraft.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.book.CraftingRecipeCategory;

/**
 * {@code SpecialCraftingRecipe}.
 */
public abstract class SpecialCraftingRecipe implements CraftingRecipe {

	private final CraftingRecipeCategory category;

	public SpecialCraftingRecipe(CraftingRecipeCategory category) {
		this.category = category;
	}

	@Override
	public boolean isIgnoredInRecipeBook() {
		return true;
	}

	@Override
	public CraftingRecipeCategory getCategory() {
		return this.category;
	}

	@Override
	public IngredientPlacement getIngredientPlacement() {
		return IngredientPlacement.NONE;
	}

	@Override
	public abstract RecipeSerializer<? extends SpecialCraftingRecipe> getSerializer();

	/**
	 * {@code SpecialRecipeSerializer}.
	 */
	public static class SpecialRecipeSerializer<T extends CraftingRecipe> implements RecipeSerializer<T> {

		private final MapCodec<T> codec;
		private final PacketCodec<RegistryByteBuf, T> packetCodec;

		public SpecialRecipeSerializer(SpecialCraftingRecipe.SpecialRecipeSerializer.Factory<T> factory) {
			this.codec = RecordCodecBuilder.mapCodec(
					instance -> instance.group(
							                    CraftingRecipeCategory.CODEC
									                    .fieldOf("category")
									                    .orElse(CraftingRecipeCategory.MISC)
									                    .forGetter(CraftingRecipe::getCategory)
					                    )
					                    .apply(instance, factory::create)
			);
			this.packetCodec =
					PacketCodec.tuple(
							CraftingRecipeCategory.PACKET_CODEC,
							CraftingRecipe::getCategory,
							factory::create
					);
		}

		@Override
		public MapCodec<T> codec() {
			return this.codec;
		}

		@Override
		public PacketCodec<RegistryByteBuf, T> packetCodec() {
			return this.packetCodec;
		}

		@FunctionalInterface
		/**
		 * {@code Factory}.
		 */
		public interface Factory<T extends CraftingRecipe> {

			T create(CraftingRecipeCategory category);
		}
	}
}
