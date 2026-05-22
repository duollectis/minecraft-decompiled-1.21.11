package net.minecraft.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.potion.Potions;
import net.minecraft.text.Text;

/**
 * Стрела с зельем, накладывающая эффекты зелья на поражённую цель.
 * Название предмета формируется динамически из компонента {@code POTION_CONTENTS}.
 */
public class TippedArrowItem extends ArrowItem {

	public TippedArrowItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ItemStack getDefaultStack() {
		ItemStack itemStack = super.getDefaultStack();
		itemStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.POISON));
		return itemStack;
	}

	@Override
	public Text getName(ItemStack stack) {
		PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
		return potionContents != null
			? potionContents.getName(translationKey + ".effect.")
			: super.getName(stack);
	}
}
