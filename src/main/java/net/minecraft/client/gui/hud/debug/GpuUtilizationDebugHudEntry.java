package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

/**
 * Запись отладочного HUD: загрузка GPU в процентах.
 * При превышении 100% значение выделяется красным.
 */
@Environment(EnvType.CLIENT)
public class GpuUtilizationDebugHudEntry implements DebugHudEntry {

	private static final double GPU_OVERLOAD_THRESHOLD = 100.0;

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		double gpuUsage = MinecraftClient.getInstance().getGpuUtilizationPercentage();
		String usageLabel = gpuUsage > GPU_OVERLOAD_THRESHOLD
			? Formatting.RED + "100%"
			: Math.round(gpuUsage) + "%";

		lines.addLine("GPU: " + usageLabel);
	}

	@Override
	public boolean canShow(boolean reducedDebugInfo) {
		return true;
	}
}
