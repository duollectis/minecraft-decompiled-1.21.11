package net.minecraft.structure;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Фабрика генераторов структур. Принимает контекст генерации и возвращает
 * опциональный {@link StructurePiecesGenerator}, если структура может быть
 * размещена в данной позиции (биом, высота, условия).
 */
@FunctionalInterface
public interface StructureGeneratorFactory<C extends FeatureConfig> {

	Optional<StructurePiecesGenerator<C>> createGenerator(StructureGeneratorFactory.Context<C> context);

	static <C extends FeatureConfig> StructureGeneratorFactory<C> simple(
			Predicate<StructureGeneratorFactory.Context<C>> predicate, StructurePiecesGenerator<C> generator
	) {
		Optional<StructurePiecesGenerator<C>> optional = Optional.of(generator);
		return context -> predicate.test(context) ? optional : Optional.empty();
	}

	static <C extends FeatureConfig> Predicate<StructureGeneratorFactory.Context<C>> checkForBiomeOnTop(Heightmap.Type heightmapType) {
		return context -> context.isBiomeValid(heightmapType);
	}

	/**
	 * Контекст генерации структуры. Содержит все необходимые данные для принятия
	 * решения о размещении: генератор чанков, источник биомов, конфигурацию шума,
	 * позицию чанка и предикат допустимых биомов.
	 */
	public record Context<C extends FeatureConfig>(
			ChunkGenerator chunkGenerator,
			BiomeSource biomeSource,
			NoiseConfig noiseConfig,
			long seed,
			ChunkPos chunkPos,
			C config,
			HeightLimitView world,
			Predicate<RegistryEntry<Biome>> validBiome,
			StructureTemplateManager structureTemplateManager,
			DynamicRegistryManager registryManager
	) {

		public boolean isBiomeValid(Heightmap.Type heightmapType) {
			int centerX = this.chunkPos.getCenterX();
			int centerZ = this.chunkPos.getCenterZ();
			int groundY = this.chunkGenerator.getHeightInGround(centerX, centerZ, heightmapType, this.world, this.noiseConfig);
			RegistryEntry<Biome> biome = this.chunkGenerator
					.getBiomeSource()
					.getBiome(
							BiomeCoords.fromBlock(centerX),
							BiomeCoords.fromBlock(groundY),
							BiomeCoords.fromBlock(centerZ),
							this.noiseConfig.getMultiNoiseSampler()
					);
			return this.validBiome.test(biome);
		}
	}
}
