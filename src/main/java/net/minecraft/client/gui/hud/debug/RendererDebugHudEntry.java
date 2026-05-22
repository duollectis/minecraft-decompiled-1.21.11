package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

/**
 * Запись отладочного HUD для визуальных отладочных режимов рендерера
 * (хитбоксы, границы чанков, 3D-прицел и т.п.).
 * Сама по себе не добавляет строк — управляет только видимостью.
 */
@Environment(EnvType.CLIENT)
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
		return ignoreReducedDebugInfo || !reducedDebugInfo;
	}

	@Override
	public DebugHudEntryCategory getCategory() {
		return DebugHudEntryCategory.RENDERER;
	}
}
