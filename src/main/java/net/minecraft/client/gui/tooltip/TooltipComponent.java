package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.tooltip.BundleTooltipData;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.text.OrderedText;

@Environment(EnvType.CLIENT)
/**
 * {@code TooltipComponent}.
 */
public interface TooltipComponent {

	static TooltipComponent of(OrderedText text) {
		return new OrderedTextTooltipComponent(text);
	}

	static TooltipComponent of(TooltipData tooltipData) {
		return (TooltipComponent) (switch (tooltipData) {
			case BundleTooltipData bundleTooltipData -> new BundleTooltipComponent(bundleTooltipData.contents());
			case ProfilesTooltipComponent.ProfilesData profilesData -> new ProfilesTooltipComponent(profilesData);
			default -> throw new IllegalArgumentException("Unknown TooltipComponent");
		}
		);
	}

	int getHeight(TextRenderer textRenderer);

	int getWidth(TextRenderer textRenderer);

	default boolean isSticky() {
		return false;
	}

	default void drawText(DrawContext context, TextRenderer textRenderer, int x, int y) {
	}

	default void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
	}
}
