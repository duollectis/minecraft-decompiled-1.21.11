package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

@Environment(EnvType.CLIENT)
/**
 * {@code SoundMoodDebugHudEntry}.
 */
public class SoundMoodDebugHudEntry implements DebugHudEntry {

	@Override
	public void render(
			DebugHudLines lines,
			@Nullable World world,
			@Nullable WorldChunk clientChunk,
			@Nullable WorldChunk chunk
	) {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		if (minecraftClient.player != null) {
			lines.addLine(
					minecraftClient.getSoundManager().getDebugString()
							+ String.format(
							Locale.ROOT,
							" (Mood %d%%)",
							Math.round(minecraftClient.player.getMoodPercentage() * 100.0F)
					)
			);
		}
	}
}
