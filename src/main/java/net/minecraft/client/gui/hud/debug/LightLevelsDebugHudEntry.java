package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Запись отладочного HUD: уровни освещения (небо + блоки) в позиции камеры.
 * При включённых серверных значениях показывает также серверные данные.
 */
@Environment(EnvType.CLIENT)
public class LightLevelsDebugHudEntry implements DebugHudEntry {

	public static final Identifier SECTION_ID = Identifier.ofVanilla("light");

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		Entity cameraEntity = client.getCameraEntity();

		if (cameraEntity == null || client.world == null) {
			return;
		}

		BlockPos blockPos = cameraEntity.getBlockPos();
		int combinedLight = client.world.getChunkManager().getLightingProvider().getLight(blockPos, 0);
		int skyLight = client.world.getLightLevel(LightType.SKY, blockPos);
		int blockLight = client.world.getLightLevel(LightType.BLOCK, blockPos);
		String clientLightLine = "Client Light: " + combinedLight + " (" + skyLight + " sky, " + blockLight + " block)";

		if (SharedConstants.SHOW_SERVER_DEBUG_VALUES) {
			String serverLightLine;
			if (chunk != null) {
				LightingProvider lightingProvider = chunk.getWorld().getLightingProvider();
				serverLightLine = "Server Light: ("
					+ lightingProvider.get(LightType.SKY).getLightLevel(blockPos)
					+ " sky, "
					+ lightingProvider.get(LightType.BLOCK).getLightLevel(blockPos)
					+ " block)";
			} else {
				serverLightLine = "Server Light: (?? sky, ?? block)";
			}

			lines.addLinesToSection(SECTION_ID, List.of(clientLightLine, serverLightLine));
		} else {
			lines.addLineToSection(SECTION_ID, clientLightLine);
		}
	}
}
