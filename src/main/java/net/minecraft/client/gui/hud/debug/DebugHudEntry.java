package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code DebugHudEntry}.
 */
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
