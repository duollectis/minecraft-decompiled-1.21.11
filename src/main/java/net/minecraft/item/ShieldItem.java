package net.minecraft.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;

/**
 * Предмет «Щит». Если щит окрашен (компонент {@link DataComponentTypes#BASE_COLOR}),
 * отображает название с суффиксом цвета через ключ перевода {@code <translationKey>.<colorId>}.
 */
public class ShieldItem extends Item {

	public ShieldItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public Text getName(ItemStack stack) {
		DyeColor color = stack.get(DataComponentTypes.BASE_COLOR);

		return color != null
			? Text.translatable(translationKey + "." + color.getId())
			: super.getName(stack);
	}
}
