package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

/**
 * Категория записи отладочного HUD. Используется для группировки
 * и сортировки записей в меню настроек отладки.
 */
@Environment(EnvType.CLIENT)
public record DebugHudEntryCategory(Text label, float sortKey) {

	public static final DebugHudEntryCategory TEXT = new DebugHudEntryCategory(
		Text.translatable("debug.options.category.text"),
		1.0F
	);
	public static final DebugHudEntryCategory RENDERER = new DebugHudEntryCategory(
		Text.translatable("debug.options.category.renderer"),
		2.0F
	);
}
