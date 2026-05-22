package net.minecraft.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.item.equipment.trim.ArmorTrimMaterials;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.display.SmithingRecipeDisplay;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Рецепт нанесения отделки (trim) на броню через кузнечный стол.
 * Требует шаблон отделки, базовую броню и материал-добавку.
 * Если броня уже имеет идентичную отделку — результат пустой (защита от дублирования).
 */
public class SmithingTrimRecipe implements SmithingRecipe {

	final Ingredient template;
	final Ingredient base;
	final Ingredient addition;
	final RegistryEntry<ArmorTrimPattern> pattern;
	private @Nullable IngredientPlacement ingredientPlacement;

	public SmithingTrimRecipe(
		Ingredient template,
		Ingredient base,
		Ingredient addition,
		RegistryEntry<ArmorTrimPattern> pattern
	) {
		this.template = template;
		this.base = base;
		this.addition = addition;
		this.pattern = pattern;
	}

	@Override
	public ItemStack craft(SmithingRecipeInput input, RegistryWrapper.WrapperLookup wrapperLookup) {
		return craft(wrapperLookup, input.base(), input.addition(), pattern);
	}

	/**
	 * Применяет отделку к броне: определяет материал по добавке, создаёт {@link ArmorTrim}
	 * и устанавливает его на копию базового предмета. Возвращает пустой стек,
	 * если материал не найден или отделка уже идентична существующей.
	 */
	public static ItemStack craft(
		RegistryWrapper.WrapperLookup registries,
		ItemStack base,
		ItemStack addition,
		RegistryEntry<ArmorTrimPattern> pattern
	) {
		Optional<RegistryEntry<ArmorTrimMaterial>> material = ArmorTrimMaterials.get(registries, addition);

		if (material.isEmpty()) {
			return ItemStack.EMPTY;
		}

		ArmorTrim existingTrim = base.get(DataComponentTypes.TRIM);
		ArmorTrim newTrim = new ArmorTrim(material.get(), pattern);

		if (Objects.equals(existingTrim, newTrim)) {
			return ItemStack.EMPTY;
		}

		ItemStack result = base.copyWithCount(1);
		result.set(DataComponentTypes.TRIM, newTrim);

		return result;
	}

	@Override
	public Optional<Ingredient> template() {
		return Optional.of(template);
	}

	@Override
	public Ingredient base() {
		return base;
	}

	@Override
	public Optional<Ingredient> addition() {
		return Optional.of(addition);
	}

	@Override
	public RecipeSerializer<SmithingTrimRecipe> getSerializer() {
		return RecipeSerializer.SMITHING_TRIM;
	}

	@Override
	public IngredientPlacement getIngredientPlacement() {
		if (ingredientPlacement == null) {
			ingredientPlacement = IngredientPlacement.forShapeless(List.of(template, base, addition));
		}

		return ingredientPlacement;
	}

	@Override
	public List<RecipeDisplay> getDisplays() {
		SlotDisplay baseDisplay = base.toDisplay();
		SlotDisplay additionDisplay = addition.toDisplay();
		SlotDisplay templateDisplay = template.toDisplay();

		return List.of(
			new SmithingRecipeDisplay(
				templateDisplay,
				baseDisplay,
				additionDisplay,
				new SlotDisplay.SmithingTrimSlotDisplay(baseDisplay, additionDisplay, pattern),
				new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)
			)
		);
	}

	public static class Serializer implements RecipeSerializer<SmithingTrimRecipe> {

		private static final MapCodec<SmithingTrimRecipe> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Ingredient.CODEC.fieldOf("template").forGetter(recipe -> recipe.template),
				Ingredient.CODEC.fieldOf("base").forGetter(recipe -> recipe.base),
				Ingredient.CODEC.fieldOf("addition").forGetter(recipe -> recipe.addition),
				ArmorTrimPattern.ENTRY_CODEC.fieldOf("pattern").forGetter(recipe -> recipe.pattern)
			).apply(instance, SmithingTrimRecipe::new)
		);

		public static final PacketCodec<RegistryByteBuf, SmithingTrimRecipe> PACKET_CODEC = PacketCodec.tuple(
			Ingredient.PACKET_CODEC,
			recipe -> recipe.template,
			Ingredient.PACKET_CODEC,
			recipe -> recipe.base,
			Ingredient.PACKET_CODEC,
			recipe -> recipe.addition,
			ArmorTrimPattern.ENTRY_PACKET_CODEC,
			recipe -> recipe.pattern,
			SmithingTrimRecipe::new
		);

		@Override
		public MapCodec<SmithingTrimRecipe> codec() {
			return CODEC;
		}

		@Override
		public PacketCodec<RegistryByteBuf, SmithingTrimRecipe> packetCodec() {
			return PACKET_CODEC;
		}
	}
}
