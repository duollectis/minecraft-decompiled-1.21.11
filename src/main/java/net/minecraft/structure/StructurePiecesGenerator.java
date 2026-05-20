package net.minecraft.structure;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.FeatureConfig;

@FunctionalInterface
public interface StructurePiecesGenerator<C extends FeatureConfig> {
   void generatePieces(StructurePiecesCollector collector, StructurePiecesGenerator.Context<C> context);

   public record Context<C extends FeatureConfig>(
      C config,
      ChunkGenerator chunkGenerator,
      StructureTemplateManager structureTemplateManager,
      ChunkPos chunkPos,
      HeightLimitView world,
      ChunkRandom random,
      long seed
   ) {
   }
}
