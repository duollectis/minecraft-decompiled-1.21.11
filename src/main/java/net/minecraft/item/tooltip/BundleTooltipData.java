package net.minecraft.item.tooltip;

import net.minecraft.component.type.BundleContentsComponent;

/**
 * {@code BundleTooltipData}.
 */
public record BundleTooltipData(BundleContentsComponent contents) implements TooltipData {
}
