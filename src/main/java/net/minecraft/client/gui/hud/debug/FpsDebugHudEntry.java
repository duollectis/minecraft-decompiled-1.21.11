package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Запись отладочного HUD: текущий FPS, лимит кадров и статус VSync.
 */
@Environment(EnvType.CLIENT)
public class FpsDebugHudEntry implements DebugHudEntry {

	/** Значение лимита FPS, соответствующее «без ограничений». */
	private static final int UNLIMITED_FPS = 260;

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		int fpsLimit = client.getInactivityFpsLimiter().update();
		GameOptions options = client.options;
		String fpsLimitLabel = fpsLimit == UNLIMITED_FPS ? "inf" : String.valueOf(fpsLimit);
		String vsyncSuffix = options.getEnableVsync().getValue() ? " vsync" : "";

		lines.addPriorityLine(
			String.format(Locale.ROOT, "%d fps T: %s%s", client.getCurrentFps(), fpsLimitLabel, vsyncSuffix)
		);
	}

	@Override
	public boolean canShow(boolean reducedDebugInfo) {
		return true;
	}
}
