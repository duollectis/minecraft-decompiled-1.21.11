package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code BiomeDebugHudEntry}.
 */
public class BiomeDebugHudEntry implements DebugHudEntry {

	private static final Identifier SECTION_ID = Identifier.ofVanilla("biome");

	@Override
	public void render(
			DebugHudLines lines,
			@Nullable World world,
			@Nullable WorldChunk clientChunk,
			@Nullable WorldChunk chunk
	) {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		Entity entity = minecraftClient.getCameraEntity();
		if (entity != null && minecraftClient.world != null) {
			BlockPos blockPos = entity.getBlockPos();
			if (minecraftClient.world.isInHeightLimit(blockPos.getY())) {
				if (SharedConstants.SHOW_SERVER_DEBUG_VALUES && world instanceof ServerWorld) {
					lines.addLinesToSection(
							SECTION_ID,
							List.of(
									"Biome: " + getBiomeAsString(minecraftClient.world.getBiome(blockPos)),
									"Server Biome: " + getBiomeAsString(world.getBiome(blockPos))
							)
					);
				}
				else {
					lines.addLine("Biome: " + getBiomeAsString(minecraftClient.world.getBiome(blockPos)));
				}
			}
		}
	}

	private static String getBiomeAsString(RegistryEntry<Biome> biome) {
		return (String) biome
				.getKeyOrValue()
				.map(key -> key.getValue().toString(), value -> "[unregistered " + value + "]");
	}
}
