package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

/**
 * Контракт одной записи отладочного HUD. Каждая реализация отвечает
 * за формирование строк в своей тематической секции.
 */
@Environment(EnvType.CLIENT)
public interface DebugHudEntry {

	void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	);

	default boolean canShow(boolean reducedDebugInfo) {
		return !reducedDebugInfo;
	}

	default DebugHudEntryCategory getCategory() {
		return DebugHudEntryCategory.TEXT;
	}
}
