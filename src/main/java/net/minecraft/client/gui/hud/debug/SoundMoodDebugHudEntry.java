package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Запись отладочного HUD: состояние звукового менеджера и процент настроения игрока.
 */
@Environment(EnvType.CLIENT)
public class SoundMoodDebugHudEntry implements DebugHudEntry {

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player == null) {
			return;
		}

		int moodPercent = Math.round(client.player.getMoodPercentage() * 100.0F);
		lines.addLine(
			client.getSoundManager().getDebugString()
				+ String.format(Locale.ROOT, " (Mood %d%%)", moodPercent)
		);
	}
}
