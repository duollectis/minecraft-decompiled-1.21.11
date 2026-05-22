package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

/**
 * Запись отладочного HUD: строковое представление источника чанков
 * клиентского и (при наличии) серверного мира.
 */
@Environment(EnvType.CLIENT)
public class ChunkSourceStatsDebugHudEntry implements DebugHudEntry {

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.world != null) {
			lines.addLine(client.world.asString());
		}

		if (world != null && world != client.world) {
			lines.addLine(world.asString());
		}
	}

	@Override
	public boolean canShow(boolean reducedDebugInfo) {
		return true;
	}
}
