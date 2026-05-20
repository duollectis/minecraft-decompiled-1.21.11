package net.minecraft.client.gui.hud.debug;

import java.util.ArrayList;
import java.util.List;
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

@Environment(EnvType.CLIENT)
public class ChunkGenerationStatsDebugHudEntry implements DebugHudEntry {
   private static final Identifier SECTION_ID = Identifier.ofVanilla("chunk_generation");

   @Override
   public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      Entity entity = minecraftClient.getCameraEntity();
      ServerWorld serverWorld = world instanceof ServerWorld ? (ServerWorld)world : null;
      if (entity != null && serverWorld != null) {
         BlockPos blockPos = entity.getBlockPos();
         ServerChunkManager serverChunkManager = serverWorld.getChunkManager();
         List<String> list = new ArrayList<>();
         ChunkGenerator chunkGenerator = serverChunkManager.getChunkGenerator();
         NoiseConfig noiseConfig = serverChunkManager.getNoiseConfig();
         chunkGenerator.appendDebugHudText(list, noiseConfig, blockPos);
         MultiNoiseUtil.MultiNoiseSampler multiNoiseSampler = noiseConfig.getMultiNoiseSampler();
         BiomeSource biomeSource = chunkGenerator.getBiomeSource();
         biomeSource.addDebugInfo(list, blockPos, multiNoiseSampler);
         if (chunk != null && chunk.usesOldNoise()) {
            list.add("Blending: Old");
         }

         lines.addLinesToSection(SECTION_ID, list);
      }
   }
}
