package net.minecraft.recipe;

import com.mojang.datafixers.util.Pair;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Рецепт починки предмета путём объединения двух одинаковых повреждённых предметов.
 * Итоговая прочность = сумма остаточных прочностей обоих предметов + 5% бонус от максимальной прочности.
 * Проклятия с обоих предметов переносятся на результат с максимальным уровнем.
 */
public class RepairItemRecipe extends SpecialCraftingRecipe {

	/** Бонус прочности при объединении: 5% от максимальной прочности предмета. */
	private static final int REPAIR_BONUS_PERCENT = 5;

	public RepairItemRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	private static @Nullable Pair<ItemStack, ItemStack> findPair(CraftingRecipeInput input) {
		if (input.getStackCount() != 2) {
			return null;
		}

		ItemStack first = null;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (first == null) {
				first = stack;
				continue;
			}

			return canCombineStacks(first, stack) ? Pair.of(first, stack) : null;
		}

		return null;
	}

	private static boolean canCombineStacks(ItemStack first, ItemStack second) {
		return second.isOf(first.getItem())
			&& first.getCount() == 1
			&& second.getCount() == 1
			&& first.contains(DataComponentTypes.MAX_DAMAGE)
			&& second.contains(DataComponentTypes.MAX_DAMAGE)
			&& first.contains(DataComponentTypes.DAMAGE)
			&& second.contains(DataComponentTypes.DAMAGE);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		return findPair(input) != null;
	}

	/**
	 * Объединяет два повреждённых предмета в один с восстановленной прочностью.
	 * Итоговый урон = max(maxDamage - combinedDurability, 0), где combinedDurability
	 * включает 5%-й бонус от максимальной прочности.
	 */
	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup wrapperLookup) {
		Pair<ItemStack, ItemStack> pair = findPair(input);

		if (pair == null) {
			return ItemStack.EMPTY;
		}

		ItemStack first = pair.getFirst();
		ItemStack second = pair.getSecond();
		int maxDamage = Math.max(first.getMaxDamage(), second.getMaxDamage());
		int firstDurability = first.getMaxDamage() - first.getDamage();
		int secondDurability = second.getMaxDamage() - second.getDamage();
		int combinedDurability = firstDurability + secondDurability + maxDamage * REPAIR_BONUS_PERCENT / 100;

		ItemStack result = new ItemStack(first.getItem());
		result.set(DataComponentTypes.MAX_DAMAGE, maxDamage);
		result.setDamage(Math.max(maxDamage - combinedDurability, 0));

		ItemEnchantmentsComponent firstEnchantments = EnchantmentHelper.getEnchantments(first);
		ItemEnchantmentsComponent secondEnchantments = EnchantmentHelper.getEnchantments(second);

		EnchantmentHelper.apply(
			result,
			builder -> wrapperLookup.getOrThrow(RegistryKeys.ENCHANTMENT)
				.streamEntries()
				.filter(enchantment -> enchantment.isIn(EnchantmentTags.CURSE))
				.forEach(enchantment -> {
					int curseLevel = Math.max(
						firstEnchantments.getLevel(enchantment),
						secondEnchantments.getLevel(enchantment)
					);

					if (curseLevel > 0) {
						builder.add(enchantment, curseLevel);
					}
				})
		);

		return result;
	}

	@Override
	public RecipeSerializer<RepairItemRecipe> getSerializer() {
		return RecipeSerializer.REPAIR_ITEM;
	}
}
