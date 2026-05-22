package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Запись отладочного HUD: позиция игрока внутри секции чанка (0–15 по каждой оси).
 */
@Environment(EnvType.CLIENT)
public class PlayerSectionPositionDebugHudEntry implements DebugHudEntry {

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		Entity cameraEntity = MinecraftClient.getInstance().getCameraEntity();

		if (cameraEntity == null) {
			return;
		}

		BlockPos blockPos = cameraEntity.getBlockPos();
		lines.addLineToSection(
			PlayerPositionDebugHudEntry.SECTION_ID,
			String.format(
				Locale.ROOT,
				"Section-relative: %02d %02d %02d",
				blockPos.getX() & 15,
				blockPos.getY() & 15,
				blockPos.getZ() & 15
			)
		);
	}

	@Override
	public boolean canShow(boolean reducedDebugInfo) {
		return true;
	}
}
