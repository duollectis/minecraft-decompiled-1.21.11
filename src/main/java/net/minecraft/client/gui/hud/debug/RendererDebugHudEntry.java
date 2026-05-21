package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code RendererDebugHudEntry}.
 */
public class RendererDebugHudEntry implements DebugHudEntry {

	private final boolean ignoreReducedDebugInfo;

	public RendererDebugHudEntry() {
		this(false);
	}

	public RendererDebugHudEntry(boolean ignoreReducedDebugInfo) {
		this.ignoreReducedDebugInfo = ignoreReducedDebugInfo;
	}

	@Override
	public void render(
			DebugHudLines lines,
			@Nullable World world,
			@Nullable WorldChunk clientChunk,
			@Nullable WorldChunk chunk
	) {
	}

	@Override
	public boolean canShow(boolean reducedDebugInfo) {
		return this.ignoreReducedDebugInfo || !reducedDebugInfo;
	}

	@Override
	public DebugHudEntryCategory getCategory() {
		return DebugHudEntryCategory.RENDERER;
	}
}
