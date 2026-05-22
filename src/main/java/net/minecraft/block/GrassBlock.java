package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.RandomPatchFeatureConfig;
import net.minecraft.world.gen.feature.VegetationPlacedFeatures;

import java.util.List;
import java.util.Optional;

/**
 * Блок травы. При использовании костяной муки случайным образом размещает траву и цветы
 * в радиусе 128 итераций вокруг блока, имитируя органический рост растительности.
 */
public class GrassBlock extends SpreadableBlock implements Fertilizable {

	public static final MapCodec<GrassBlock> CODEC = createCodec(GrassBlock::new);

	@Override
	public MapCodec<GrassBlock> getCodec() {
		return CODEC;
	}

	public GrassBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		return world.getBlockState(pos.up()).isAir();
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		BlockPos above = pos.up();
		BlockState shortGrass = Blocks.SHORT_GRASS.getDefaultState();
		Optional<RegistryEntry.Reference<PlacedFeature>> grassFeature = world.getRegistryManager()
				.getOrThrow(RegistryKeys.PLACED_FEATURE)
				.getOptional(VegetationPlacedFeatures.GRASS_BONEMEAL);

		for (int attempt = 0; attempt < 128; attempt++) {
			BlockPos candidate = above;
			boolean blocked = false;

			for (int step = 0; step < attempt / 16; step++) {
				candidate = candidate.add(
						random.nextInt(3) - 1,
						(random.nextInt(3) - 1) * random.nextInt(3) / 2,
						random.nextInt(3) - 1
				);
				if (world.getBlockState(candidate.down()).isOf(this) == false
						|| world.getBlockState(candidate).isFullCube(world, candidate)) {
					blocked = true;
					break;
				}
			}

			if (blocked) {
				continue;
			}

			BlockState candidateState = world.getBlockState(candidate);
			if (candidateState.isOf(shortGrass.getBlock()) && random.nextInt(10) == 0) {
				Fertilizable fertilizable = (Fertilizable) shortGrass.getBlock();
				if (fertilizable.isFertilizable(world, candidate, candidateState)) {
					fertilizable.grow(world, random, candidate, candidateState);
				}
			}

			if (candidateState.isAir() == false) {
				continue;
			}

			RegistryEntry<PlacedFeature> featureToPlace;
			if (random.nextInt(8) == 0) {
				List<ConfiguredFeature<?, ?>> flowers = world.getBiome(candidate)
						.value()
						.getGenerationSettings()
						.getFlowerFeatures();
				if (flowers.isEmpty()) {
					continue;
				}

				featureToPlace = ((RandomPatchFeatureConfig) flowers.get(random.nextInt(flowers.size())).config()).feature();
			} else {
				if (grassFeature.isEmpty()) {
					continue;
				}

				featureToPlace = grassFeature.get();
			}

			featureToPlace.value().generateUnregistered(world, world.getChunkManager().getChunkGenerator(), random, candidate);
		}
	}

	@Override
	public Fertilizable.FertilizableType getFertilizableType() {
		return Fertilizable.FertilizableType.NEIGHBOR_SPREADER;
	}
}
