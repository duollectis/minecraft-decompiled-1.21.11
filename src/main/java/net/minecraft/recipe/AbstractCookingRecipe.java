package net.minecraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.book.CookingRecipeCategory;
import net.minecraft.recipe.display.FurnaceRecipeDisplay;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;

import java.util.List;

/**
 * Базовый класс для всех рецептов приготовления (плавка, обжиг, копчение, костёр).
 * <p>
 * Хранит общие параметры: ингредиент, результат, опыт и время приготовления.
 * Конкретные подклассы определяют тип блока-плавильщика через {@link #getCookerItem()}.
 */
public abstract class AbstractCookingRecipe extends SingleStackRecipe {

	private final CookingRecipeCategory category;
	private final float experience;
	private final int cookingTime;

	public AbstractCookingRecipe(
		String group,
		CookingRecipeCategory category,
		Ingredient ingredient,
		ItemStack result,
		float experience,
		int cookingTime
	) {
		super(group, ingredient, result);
		this.category = category;
		this.experience = experience;
		this.cookingTime = cookingTime;
	}

	@Override
	public abstract RecipeSerializer<? extends AbstractCookingRecipe> getSerializer();

	@Override
	public abstract RecipeType<? extends AbstractCookingRecipe> getType();

	public float getExperience() {
		return experience;
	}

	public int getCookingTime() {
		return cookingTime;
	}

	public CookingRecipeCategory getCategory() {
		return category;
	}

	protected abstract Item getCookerItem();

	@Override
	public List<RecipeDisplay> getDisplays() {
		return List.of(
			new FurnaceRecipeDisplay(
				ingredient().toDisplay(),
				SlotDisplay.AnyFuelSlotDisplay.INSTANCE,
				new SlotDisplay.StackSlotDisplay(result()),
				new SlotDisplay.ItemSlotDisplay(getCookerItem()),
				cookingTime,
				experience
			)
		);
	}

	/**
	 * Фабричный интерфейс для создания конкретных подтипов рецептов приготовления.
	 * Используется сериализатором для инстанцирования рецепта из данных.
	 *
	 * @param <T> конкретный подтип {@link AbstractCookingRecipe}
	 */
	@FunctionalInterface
	public interface RecipeFactory<T extends AbstractCookingRecipe> {

		T create(
			String group,
			CookingRecipeCategory category,
			Ingredient ingredient,
			ItemStack result,
			float experience,
			int cookingTime
		);
	}

	/**
	 * Универсальный сериализатор для всех рецептов приготовления.
	 * Принимает фабрику и время приготовления по умолчанию, которое используется
	 * если поле {@code cookingtime} отсутствует в JSON.
	 *
	 * @param <T> конкретный подтип {@link AbstractCookingRecipe}
	 */
	public static class Serializer<T extends AbstractCookingRecipe> implements RecipeSerializer<T> {

		private final MapCodec<T> codec;
		private final PacketCodec<RegistryByteBuf, T> packetCodec;

		public Serializer(AbstractCookingRecipe.RecipeFactory<T> factory, int defaultCookingTime) {
			this.codec = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
					Codec.STRING.optionalFieldOf("group", "").forGetter(SingleStackRecipe::getGroup),
					CookingRecipeCategory.CODEC
						.fieldOf("category")
						.orElse(CookingRecipeCategory.MISC)
						.forGetter(AbstractCookingRecipe::getCategory),
					Ingredient.CODEC.fieldOf("ingredient").forGetter(SingleStackRecipe::ingredient),
					ItemStack.VALIDATED_UNCOUNTED_CODEC.fieldOf("result").forGetter(SingleStackRecipe::result),
					Codec.FLOAT.fieldOf("experience").orElse(0.0F).forGetter(AbstractCookingRecipe::getExperience),
					Codec.INT
						.fieldOf("cookingtime")
						.orElse(defaultCookingTime)
						.forGetter(AbstractCookingRecipe::getCookingTime)
				).apply(instance, factory::create)
			);
			this.packetCodec = PacketCodec.tuple(
				PacketCodecs.STRING, SingleStackRecipe::getGroup,
				CookingRecipeCategory.PACKET_CODEC, AbstractCookingRecipe::getCategory,
				Ingredient.PACKET_CODEC, SingleStackRecipe::ingredient,
				ItemStack.PACKET_CODEC, SingleStackRecipe::result,
				PacketCodecs.FLOAT, AbstractCookingRecipe::getExperience,
				PacketCodecs.INTEGER, AbstractCookingRecipe::getCookingTime,
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
	}
}
