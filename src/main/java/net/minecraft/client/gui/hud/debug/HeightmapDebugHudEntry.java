package net.minecraft.client.gui.hud.debug;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Запись отладочного HUD: высоты по всем типам карт высот
 * для клиентского и серверного чанков.
 */
@Environment(EnvType.CLIENT)
public class HeightmapDebugHudEntry implements DebugHudEntry {

	private static final Map<Heightmap.Type, String> HEIGHTMAP_ABBREVIATIONS = Maps.newEnumMap(
		Map.of(
			Heightmap.Type.WORLD_SURFACE_WG, "SW",
			Heightmap.Type.WORLD_SURFACE, "S",
			Heightmap.Type.OCEAN_FLOOR_WG, "OW",
			Heightmap.Type.OCEAN_FLOOR, "O",
			Heightmap.Type.MOTION_BLOCKING, "M",
			Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, "ML"
		)
	);
	private static final Identifier SECTION_ID = Identifier.ofVanilla("heightmaps");

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		Entity cameraEntity = client.getCameraEntity();

		if (cameraEntity == null || client.world == null || clientChunk == null) {
			return;
		}

		BlockPos blockPos = cameraEntity.getBlockPos();
		List<String> debugLines = new ArrayList<>();

		StringBuilder clientHeights = new StringBuilder("CH");
		for (Heightmap.Type type : Heightmap.Type.values()) {
			if (type.shouldSendToClient()) {
				clientHeights
					.append(" ")
					.append(HEIGHTMAP_ABBREVIATIONS.get(type))
					.append(": ")
					.append(clientChunk.sampleHeightmap(type, blockPos.getX(), blockPos.getZ()));
			}
		}
		debugLines.add(clientHeights.toString());

		StringBuilder serverHeights = new StringBuilder("SH");
		for (Heightmap.Type type : Heightmap.Type.values()) {
			if (type.isStoredServerSide()) {
				serverHeights.append(" ").append(HEIGHTMAP_ABBREVIATIONS.get(type)).append(": ");
				if (chunk != null) {
					serverHeights.append(chunk.sampleHeightmap(type, blockPos.getX(), blockPos.getZ()));
				} else {
					serverHeights.append("??");
				}
			}
		}
		debugLines.add(serverHeights.toString());

		lines.addLinesToSection(SECTION_ID, debugLines);
	}
}
