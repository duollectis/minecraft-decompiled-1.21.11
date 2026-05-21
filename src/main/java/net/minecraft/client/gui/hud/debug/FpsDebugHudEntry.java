package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

@Environment(EnvType.CLIENT)
/**
 * {@code FpsDebugHudEntry}.
 */
public class FpsDebugHudEntry implements DebugHudEntry {

	@Override
	public void render(
			DebugHudLines lines,
			@Nullable World world,
			@Nullable WorldChunk clientChunk,
			@Nullable WorldChunk chunk
	) {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		int i = minecraftClient.getInactivityFpsLimiter().update();
		GameOptions gameOptions = minecraftClient.options;
		lines.addPriorityLine(
				String.format(
						Locale.ROOT,
						"%d fps T: %s%s",
						minecraftClient.getCurrentFps(),
						i == 260 ? "inf" : i,
						gameOptions.getEnableVsync().getValue() ? " vsync" : ""
				)
		);
	}

	@Override
	public boolean canShow(boolean reducedDebugInfo) {
		return true;
	}
}
