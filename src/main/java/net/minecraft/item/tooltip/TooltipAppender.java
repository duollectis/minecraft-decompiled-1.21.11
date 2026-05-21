package net.minecraft.item.tooltip;

import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * {@code TooltipAppender}.
 */
public interface TooltipAppender {

	void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	);
}
