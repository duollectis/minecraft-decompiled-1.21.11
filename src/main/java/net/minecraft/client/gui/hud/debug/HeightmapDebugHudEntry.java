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

@Environment(EnvType.CLIENT)
/**
 * {@code HeightmapDebugHudEntry}.
 */
public class HeightmapDebugHudEntry implements DebugHudEntry {

	private static final Map<Heightmap.Type, String> HEIGHTMAP_TYPE_TO_STRING = Maps.newEnumMap(
			Map.of(
					Heightmap.Type.WORLD_SURFACE_WG,
					"SW",
					Heightmap.Type.WORLD_SURFACE,
					"S",
					Heightmap.Type.OCEAN_FLOOR_WG,
					"OW",
					Heightmap.Type.OCEAN_FLOOR,
					"O",
					Heightmap.Type.MOTION_BLOCKING,
					"M",
					Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
					"ML"
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
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		Entity entity = minecraftClient.getCameraEntity();
		if (entity != null && minecraftClient.world != null && clientChunk != null) {
			BlockPos blockPos = entity.getBlockPos();
			List<String> list = new ArrayList<>();
			StringBuilder stringBuilder = new StringBuilder("CH");

			for (Heightmap.Type type : Heightmap.Type.values()) {
				if (type.shouldSendToClient()) {
					stringBuilder.append(" ")
					             .append(HEIGHTMAP_TYPE_TO_STRING.get(type))
					             .append(": ")
					             .append(clientChunk.sampleHeightmap(type, blockPos.getX(), blockPos.getZ()));
				}
			}

			list.add(stringBuilder.toString());
			stringBuilder.setLength(0);
			stringBuilder.append("SH");

			for (Heightmap.Type typex : Heightmap.Type.values()) {
				if (typex.isStoredServerSide()) {
					stringBuilder.append(" ").append(HEIGHTMAP_TYPE_TO_STRING.get(typex)).append(": ");
					if (chunk != null) {
						stringBuilder.append(chunk.sampleHeightmap(typex, blockPos.getX(), blockPos.getZ()));
					}
					else {
						stringBuilder.append("??");
					}
				}
			}

			list.add(stringBuilder.toString());
			lines.addLinesToSection(SECTION_ID, list);
		}
	}
}
