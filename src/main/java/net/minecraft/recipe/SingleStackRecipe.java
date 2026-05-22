package net.minecraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для рецептов, принимающих ровно один стак предмета на входе
 * (плавка, обжиг, копчение, обработка на камнерезе и т.д.).
 * <p>
 * Хранит единственный {@link Ingredient}, результат и группу рецепта.
 * Ленивая инициализация {@link IngredientPlacement} позволяет избежать
 * лишних вычислений до первого обращения к слотам.
 */
public abstract class SingleStackRecipe implements Recipe<SingleStackRecipeInput> {

	private final Ingredient ingredient;
	private final ItemStack result;
	private final String group;
	private @Nullable IngredientPlacement ingredientPlacement;

	public SingleStackRecipe(String group, Ingredient ingredient, ItemStack result) {
		this.group = group;
		this.ingredient = ingredient;
		this.result = result;
	}

	@Override
	public abstract RecipeSerializer<? extends SingleStackRecipe> getSerializer();

	@Override
	public abstract RecipeType<? extends SingleStackRecipe> getType();

	@Override
	public boolean matches(SingleStackRecipeInput input, World world) {
		return ingredient.test(input.item());
	}

	@Override
	public String getGroup() {
		return group;
	}

	public Ingredient ingredient() {
		return ingredient;
	}

	protected ItemStack result() {
		return result;
	}

	@Override
	public IngredientPlacement getIngredientPlacement() {
		if (ingredientPlacement == null) {
			ingredientPlacement = IngredientPlacement.forSingleSlot(ingredient);
		}

		return ingredientPlacement;
	}

	@Override
	public ItemStack craft(SingleStackRecipeInput input, RegistryWrapper.WrapperLookup registries) {
		return result.copy();
	}

	/**
	 * Фабричный интерфейс для создания конкретных подтипов рецептов с одним стаком.
	 *
	 * @param <T> конкретный подтип {@link SingleStackRecipe}
	 */
	@FunctionalInterface
	public interface RecipeFactory<T extends SingleStackRecipe> {

		T create(String group, Ingredient ingredient, ItemStack result);
	}

	/**
	 * Универсальный сериализатор для рецептов с одним входным стаком.
	 *
	 * @param <T> конкретный подтип {@link SingleStackRecipe}
	 */
	public static class Serializer<T extends SingleStackRecipe> implements RecipeSerializer<T> {

		private final MapCodec<T> codec;
		private final PacketCodec<RegistryByteBuf, T> packetCodec;

		protected Serializer(SingleStackRecipe.RecipeFactory<T> recipeFactory) {
			this.codec = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
					Codec.STRING.optionalFieldOf("group", "").forGetter(SingleStackRecipe::getGroup),
					Ingredient.CODEC.fieldOf("ingredient").forGetter(SingleStackRecipe::ingredient),
					ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter(SingleStackRecipe::result)
				).apply(instance, recipeFactory::create)
			);
			this.packetCodec = PacketCodec.tuple(
				PacketCodecs.STRING, SingleStackRecipe::getGroup,
				Ingredient.PACKET_CODEC, SingleStackRecipe::ingredient,
				ItemStack.PACKET_CODEC, SingleStackRecipe::result,
				recipeFactory::create
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
	}
}
