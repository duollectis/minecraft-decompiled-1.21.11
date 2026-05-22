package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

/**
 * Запись отладочного HUD: версия игры и название клиентского мода.
 */
@Environment(EnvType.CLIENT)
class GameVersionDebugHudEntry implements DebugHudEntry {

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		lines.addPriorityLine(
			"Minecraft "
				+ SharedConstants.getGameVersion().name()
				+ " ("
				+ MinecraftClient.getInstance().getGameVersion()
				+ "/"
				+ ClientBrandRetriever.getClientModName()
				+ ")"
		);
	}

	@Override
	public boolean canShow(boolean reducedDebugInfo) {
		return true;
	}
}
