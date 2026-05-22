package net.minecraft.item.tooltip;

import net.minecraft.component.type.BundleContentsComponent;

/**
 * Данные тултипа для сумки: содержит компонент содержимого для отображения предметов внутри.
 */
public record BundleTooltipData(BundleContentsComponent contents) implements TooltipData {
}
