package net.minecraft.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.book.CraftingRecipeCategory;

/**
 * Базовый класс для «особых» рецептов крафта, логика которых не может быть
 * выражена статическим JSON-шаблоном (покраска брони, клонирование книг и т.д.).
 * <p>
 * Такие рецепты скрыты из книги рецептов ({@link #isIgnoredInRecipeBook()} = true)
 * и не имеют фиксированного расположения ингредиентов ({@link IngredientPlacement#NONE}).
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
		return category;
	}

	@Override
	public IngredientPlacement getIngredientPlacement() {
		return IngredientPlacement.NONE;
	}

	@Override
	public abstract RecipeSerializer<? extends SpecialCraftingRecipe> getSerializer();

	/**
	 * Универсальный сериализатор для особых рецептов крафта.
	 * Кодирует только категорию рецепта — вся остальная логика хранится в коде.
	 *
	 * @param <T> конкретный подтип {@link CraftingRecipe}
	 */
	public static class SpecialRecipeSerializer<T extends CraftingRecipe> implements RecipeSerializer<T> {

		private final MapCodec<T> codec;
		private final PacketCodec<RegistryByteBuf, T> packetCodec;

		public SpecialRecipeSerializer(SpecialRecipeSerializer.Factory<T> factory) {
			this.codec = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
					CraftingRecipeCategory.CODEC
						.fieldOf("category")
						.orElse(CraftingRecipeCategory.MISC)
						.forGetter(CraftingRecipe::getCategory)
				).apply(instance, factory::create)
			);
			this.packetCodec = PacketCodec.tuple(
				CraftingRecipeCategory.PACKET_CODEC,
				CraftingRecipe::getCategory,
				factory::create
			);
		}

		@Override
		public MapCodec<T> codec() {
			return codec;
		}

		@Override
		public PacketCodec<RegistryByteBuf, T> packetCodec() {
			return packetCodec;
		}

		/**
		 * Фабричный интерфейс для создания особых рецептов крафта по категории.
		 *
		 * @param <T> конкретный подтип {@link CraftingRecipe}
		 */
		@FunctionalInterface
		public interface Factory<T extends CraftingRecipe> {

			T create(CraftingRecipeCategory category);
		}
	}
}
