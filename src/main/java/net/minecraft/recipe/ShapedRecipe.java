package net.minecraft.recipe;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Рецепт крафта с фиксированным расположением ингредиентов (форменный крафт).
 * <p>
 * Паттерн хранится в {@link RawShapedRecipe}, который поддерживает зеркальное
 * отражение по горизонтали. Ленивая инициализация {@link IngredientPlacement}
 * откладывает вычисление слотов до первого обращения.
 */
public class ShapedRecipe implements CraftingRecipe {

	final RawShapedRecipe raw;
	final ItemStack result;
	final String group;
	final CraftingRecipeCategory category;
	final boolean showNotification;
	private @Nullable IngredientPlacement ingredientPlacement;

	public ShapedRecipe(
		String group,
		CraftingRecipeCategory category,
		RawShapedRecipe raw,
		ItemStack result,
		boolean showNotification
	) {
		this.group = group;
		this.category = category;
		this.raw = raw;
		this.result = result;
		this.showNotification = showNotification;
	}

	public ShapedRecipe(String group, CraftingRecipeCategory category, RawShapedRecipe raw, ItemStack result) {
		this(group, category, raw, result, true);
	}

	@Override
	public RecipeSerializer<? extends ShapedRecipe> getSerializer() {
		return RecipeSerializer.SHAPED;
	}

	@Override
	public String getGroup() {
		return group;
	}

	@Override
	public CraftingRecipeCategory getCategory() {
		return category;
	}

	@VisibleForTesting
	public List<Optional<Ingredient>> getIngredients() {
		return raw.getIngredients();
	}

	@Override
	public IngredientPlacement getIngredientPlacement() {
		if (ingredientPlacement == null) {
			ingredientPlacement = IngredientPlacement.forMultipleSlots(raw.getIngredients());
		}

		return ingredientPlacement;
	}

	@Override
	public boolean showNotification() {
		return showNotification;
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		return raw.matches(input);
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
		return result.copy();
	}

	public int getWidth() {
		return raw.getWidth();
	}

	public int getHeight() {
		return raw.getHeight();
	}

	@Override
	public List<RecipeDisplay> getDisplays() {
		return List.of(
			new ShapedCraftingRecipeDisplay(
				raw.getWidth(),
				raw.getHeight(),
				raw.getIngredients()
					.stream()
					.map(ingredient -> ingredient
						.<SlotDisplay>map(Ingredient::toDisplay)
						.orElse(SlotDisplay.EmptySlotDisplay.INSTANCE))
					.toList(),
				new SlotDisplay.StackSlotDisplay(result),
				new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
			)
		);
	}

	/**
	 * Сериализатор форменного рецепта крафта.
	 * Использует ручное чтение/запись пакета вместо {@code PacketCodec.tuple},
	 * так как {@link RawShapedRecipe} имеет собственный {@code PACKET_CODEC}.
	 */
	public static class Serializer implements RecipeSerializer<ShapedRecipe> {

		public static final MapCodec<ShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Codec.STRING.optionalFieldOf("group", "").forGetter(recipe -> recipe.group),
				CraftingRecipeCategory.CODEC
					.fieldOf("category")
					.orElse(CraftingRecipeCategory.MISC)
					.forGetter(recipe -> recipe.category),
				RawShapedRecipe.CODEC.forGetter(recipe -> recipe.raw),
				ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
				Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(recipe -> recipe.showNotification)
			).apply(instance, ShapedRecipe::new)
		);
		public static final PacketCodec<RegistryByteBuf, ShapedRecipe> PACKET_CODEC = PacketCodec.ofStatic(
			ShapedRecipe.Serializer::write,
			ShapedRecipe.Serializer::read
		);

		@Override
		public MapCodec<ShapedRecipe> codec() {
			return CODEC;
		}

		@Override
		public PacketCodec<RegistryByteBuf, ShapedRecipe> packetCodec() {
			return PACKET_CODEC;
		}

		private static ShapedRecipe read(RegistryByteBuf buf) {
			String group = buf.readString();
			CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
			RawShapedRecipe raw = RawShapedRecipe.PACKET_CODEC.decode(buf);
			ItemStack result = ItemStack.PACKET_CODEC.decode(buf);
			boolean showNotification = buf.readBoolean();
			return new ShapedRecipe(group, category, raw, result, showNotification);
		}

		private static void write(RegistryByteBuf buf, ShapedRecipe recipe) {
			buf.writeString(recipe.group);
			buf.writeEnumConstant(recipe.category);
			RawShapedRecipe.PACKET_CODEC.encode(buf, recipe.raw);
			ItemStack.PACKET_CODEC.encode(buf, recipe.result);
			buf.writeBoolean(recipe.showNotification);
		}
	}
}
