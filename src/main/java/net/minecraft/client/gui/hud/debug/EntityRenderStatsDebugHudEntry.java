package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

/**
 * Запись отладочного HUD: статистика рендеринга сущностей.
 */
@Environment(EnvType.CLIENT)
public class EntityRenderStatsDebugHudEntry implements DebugHudEntry {

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		String debugString = MinecraftClient.getInstance().worldRenderer.getEntitiesDebugString();

		if (debugString != null) {
			lines.addLine(debugString);
		}
	}

	@Override
	public boolean canShow(boolean reducedDebugInfo) {
		return true;
	}
}
