package net.minecraft.item.tooltip;

import java.util.function.Consumer;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.text.Text;

public interface TooltipAppender {
   void appendTooltip(Item.TooltipContext context, Consumer<Text> textConsumer, TooltipType type, ComponentsAccess components);
}
