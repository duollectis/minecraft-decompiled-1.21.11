package net.minecraft.loot.function;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

/** Функция лута, переплавляющая предмет в печи, если для него существует рецепт плавки. */
public class FurnaceSmeltLootFunction extends ConditionalLootFunction {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<FurnaceSmeltLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance).apply(instance, FurnaceSmeltLootFunction::new)
	);

	private FurnaceSmeltLootFunction(List<LootCondition> conditions) {
		super(conditions);
	}

	@Override
	public LootFunctionType<FurnaceSmeltLootFunction> getType() {
		return LootFunctionTypes.FURNACE_SMELT;
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		if (stack.isEmpty()) {
			return stack;
		}

		SingleStackRecipeInput recipeInput = new SingleStackRecipeInput(stack);
		Optional<RecipeEntry<SmeltingRecipe>> recipe = context.getWorld()
			.getRecipeManager()
			.getFirstMatch(RecipeType.SMELTING, recipeInput, context.getWorld());

		if (recipe.isPresent()) {
			ItemStack result = recipe.get().value().craft(recipeInput, context.getWorld().getRegistryManager());

			if (!result.isEmpty()) {
				return result.copyWithCount(stack.getCount());
			}
		}

		LOGGER.warn("Couldn't smelt {} because there is no smelting recipe", stack);

		return stack;
	}

	public static ConditionalLootFunction.Builder<?> builder() {
		return builder(FurnaceSmeltLootFunction::new);
	}
}
