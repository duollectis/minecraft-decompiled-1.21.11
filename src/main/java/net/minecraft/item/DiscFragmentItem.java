package net.minecraft.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * Предмет — фрагмент музыкального диска.
 * При наведении отображает описание, из которого можно собрать полный диск.
 */
public class DiscFragmentItem extends Item {

	public DiscFragmentItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public void appendTooltip(
			ItemStack stack,
			Item.TooltipContext context,
			TooltipDisplayComponent displayComponent,
			Consumer<Text> textConsumer,
			TooltipType type
	) {
		textConsumer.accept(getDescription().formatted(Formatting.GRAY));
	}

	public MutableText getDescription() {
		return Text.translatable(translationKey + ".desc");
	}
}
