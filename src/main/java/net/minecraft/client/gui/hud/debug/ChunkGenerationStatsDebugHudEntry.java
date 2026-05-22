package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Запись отладочного HUD: статистика генерации чанков (шум, биомы, смешивание).
 * Доступна только при наличии серверного мира.
 */
@Environment(EnvType.CLIENT)
public class ChunkGenerationStatsDebugHudEntry implements DebugHudEntry {

	private static final Identifier SECTION_ID = Identifier.ofVanilla("chunk_generation");

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		Entity cameraEntity = client.getCameraEntity();
		ServerWorld serverWorld = world instanceof ServerWorld sw ? sw : null;

		if (cameraEntity == null || serverWorld == null) {
			return;
		}

		BlockPos blockPos = cameraEntity.getBlockPos();
		ServerChunkManager chunkManager = serverWorld.getChunkManager();
		List<String> debugLines = new ArrayList<>();
		ChunkGenerator chunkGenerator = chunkManager.getChunkGenerator();
		NoiseConfig noiseConfig = chunkManager.getNoiseConfig();

		chunkGenerator.appendDebugHudText(debugLines, noiseConfig, blockPos);

		MultiNoiseUtil.MultiNoiseSampler noiseSampler = noiseConfig.getMultiNoiseSampler();
		BiomeSource biomeSource = chunkGenerator.getBiomeSource();
		biomeSource.addDebugInfo(debugLines, blockPos, noiseSampler);

		if (chunk != null && chunk.usesOldNoise()) {
			debugLines.add("Blending: Old");
		}

		lines.addLinesToSection(SECTION_ID, debugLines);
	}
}
