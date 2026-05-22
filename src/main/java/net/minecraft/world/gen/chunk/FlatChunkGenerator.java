package net.minecraft.world.gen.chunk;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Генератор плоского мира. Заполняет чанки слоями блоков согласно {@link FlatChunkGeneratorConfig}.
 * Не использует шумовые функции — рельеф полностью детерминирован конфигурацией.
 */
public class FlatChunkGenerator extends ChunkGenerator {

	public static final MapCodec<FlatChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(FlatChunkGeneratorConfig.CODEC.fieldOf("settings").forGetter(FlatChunkGenerator::getConfig))
					.apply(instance, instance.stable(FlatChunkGenerator::new))
	);

	private final FlatChunkGeneratorConfig config;

	public FlatChunkGenerator(FlatChunkGeneratorConfig config) {
		super(new FixedBiomeSource(config.getBiome()), Util.memoize(config::createGenerationSettings));
		this.config = config;
	}

	@Override
	public StructurePlacementCalculator createStructurePlacementCalculator(
			RegistryWrapper<StructureSet> structureSetRegistry,
			NoiseConfig noiseConfig,
			long seed
	) {
		Stream<RegistryEntry<StructureSet>> structureStream = config.getStructureOverrides()
				.map(RegistryEntryList::stream)
				.orElseGet(() -> structureSetRegistry
						.streamEntries()
						.map(entry -> (RegistryEntry<StructureSet>) entry));
		return StructurePlacementCalculator.create(noiseConfig, seed, biomeSource, structureStream);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> getCodec() {
		return CODEC;
	}

	public FlatChunkGeneratorConfig getConfig() {
		return config;
	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
	}

	@Override
	public int getSpawnHeight(HeightLimitView world) {
		return world.getBottomY() + Math.min(world.getHeight(), config.getLayerBlocks().size());
	}

	@Override
	public CompletableFuture<Chunk> populateNoise(
			Blender blender,
			NoiseConfig noiseConfig,
			StructureAccessor structureAccessor,
			Chunk chunk
	) {
		List<BlockState> layerBlocks = config.getLayerBlocks();
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		Heightmap oceanFloor = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
		Heightmap worldSurface = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);

		for (int layerIndex = 0; layerIndex < Math.min(chunk.getHeight(), layerBlocks.size()); layerIndex++) {
			BlockState blockState = layerBlocks.get(layerIndex);

			if (blockState == null) {
				continue;
			}

			int worldY = chunk.getBottomY() + layerIndex;

			for (int localX = 0; localX < 16; localX++) {
				for (int localZ = 0; localZ < 16; localZ++) {
					chunk.setBlockState(mutable.set(localX, worldY, localZ), blockState);
					oceanFloor.trackUpdate(localX, worldY, localZ, blockState);
					worldSurface.trackUpdate(localX, worldY, localZ, blockState);
				}
			}
		}

		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		List<BlockState> layerBlocks = config.getLayerBlocks();

		for (int layerIndex = Math.min(layerBlocks.size() - 1, world.getTopYInclusive()); layerIndex >= 0; layerIndex--) {
			BlockState blockState = layerBlocks.get(layerIndex);

			if (blockState != null && heightmap.getBlockPredicate().test(blockState)) {
				return world.getBottomY() + layerIndex + 1;
			}
		}

		return world.getBottomY();
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		return new VerticalBlockSample(
				world.getBottomY(),
				config.getLayerBlocks()
				      .stream()
				      .limit(world.getHeight())
				      .map(state -> state == null ? Blocks.AIR.getDefaultState() : state)
				      .toArray(BlockState[]::new)
		);
	}

	@Override
	public void appendDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
	}

	@Override
	public void carve(
			ChunkRegion chunkRegion,
			long seed,
			NoiseConfig noiseConfig,
			BiomeAccess biomeAccess,
			StructureAccessor structureAccessor,
			Chunk chunk
	) {
	}

	@Override
	public void populateEntities(ChunkRegion region) {
	}

	@Override
	public int getMinimumY() {
		return 0;
	}

	@Override
	public int getWorldHeight() {
		return 384;
	}

	@Override
	public int getSeaLevel() {
		return -63;
	}
}
