package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Запись отладочного HUD: локальная сложность в текущем чанке.
 */
@Environment(EnvType.CLIENT)
public class LocalDifficultyDebugHudEntry implements DebugHudEntry {

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		Entity cameraEntity = client.getCameraEntity();

		if (cameraEntity == null || chunk == null || !(world instanceof ServerWorld serverWorld)) {
			return;
		}

		BlockPos blockPos = cameraEntity.getBlockPos();

		if (!serverWorld.isInHeightLimit(blockPos.getY())) {
			return;
		}

		float moonSize = serverWorld.getMoonSize(blockPos);
		long inhabitedTime = chunk.getInhabitedTime();
		LocalDifficulty localDifficulty = new LocalDifficulty(
			serverWorld.getDifficulty(),
			serverWorld.getTimeOfDay(),
			inhabitedTime,
			moonSize
		);

		lines.addLine(
			String.format(
				Locale.ROOT,
				"Local Difficulty: %.2f // %.2f (Day %d)",
				localDifficulty.getLocalDifficulty(),
				localDifficulty.getClampedLocalDifficulty(),
				serverWorld.getDay()
			)
		);
	}
}
