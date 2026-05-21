package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code EntityRenderStatsDebugHudEntry}.
 */
public class EntityRenderStatsDebugHudEntry implements DebugHudEntry {

	@Override
	public void render(
			DebugHudLines lines,
			@Nullable World world,
			@Nullable WorldChunk clientChunk,
			@Nullable WorldChunk chunk
	) {
		String string = MinecraftClient.getInstance().worldRenderer.getEntitiesDebugString();
		if (string != null) {
			lines.addLine(string);
		}
	}

	@Override
	public boolean canShow(boolean reducedDebugInfo) {
		return true;
	}
}
