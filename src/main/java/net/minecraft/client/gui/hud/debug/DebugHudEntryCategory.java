package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
/**
 * {@code DebugHudEntryCategory}.
 */
public record DebugHudEntryCategory(Text label, float sortKey) {

	public static final DebugHudEntryCategory
			TEXT =
			new DebugHudEntryCategory(Text.translatable("debug.options.category.text"), 1.0F);
	public static final DebugHudEntryCategory
			RENDERER =
			new DebugHudEntryCategory(Text.translatable("debug.options.category.renderer"), 2.0F);
}
