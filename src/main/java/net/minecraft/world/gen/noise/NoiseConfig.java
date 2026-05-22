package net.minecraft.world.gen.noise;

import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.noise.InterpolatedNoiseSampler;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Конфигурация шума для генерации мира: хранит все сэмплеры шума,
 * роутер плотности, мульти-шумовой сэмплер и строитель поверхности.
 * Создаётся один раз на мир и используется потокобезопасно через ConcurrentHashMap.
 */
public final class NoiseConfig {

	final RandomSplitter randomDeriver;
	private final RegistryEntryLookup<DoublePerlinNoiseSampler.NoiseParameters> noiseParametersRegistry;
	private final NoiseRouter noiseRouter;
	private final MultiNoiseUtil.MultiNoiseSampler multiNoiseSampler;
	private final SurfaceBuilder surfaceBuilder;
	private final RandomSplitter aquiferRandomDeriver;
	private final RandomSplitter oreRandomDeriver;
	private final Map<RegistryKey<DoublePerlinNoiseSampler.NoiseParameters>, DoublePerlinNoiseSampler> noises;
	private final Map<Identifier, RandomSplitter> randomDerivers;

	public static NoiseConfig create(
			RegistryEntryLookup.RegistryLookup registryLookup,
			RegistryKey<ChunkGeneratorSettings> chunkGeneratorSettingsKey,
			long legacyWorldSeed
	) {
		return create(
				registryLookup
						.getOrThrow(RegistryKeys.CHUNK_GENERATOR_SETTINGS)
						.getOrThrow(chunkGeneratorSettingsKey)
						.value(),
				registryLookup.getOrThrow(RegistryKeys.NOISE_PARAMETERS),
				legacyWorldSeed
		);
	}

	public static NoiseConfig create(
			ChunkGeneratorSettings chunkGeneratorSettings,
			RegistryEntryLookup<DoublePerlinNoiseSampler.NoiseParameters> noiseParametersLookup,
			long legacyWorldSeed
	) {
		return new NoiseConfig(chunkGeneratorSettings, noiseParametersLookup, legacyWorldSeed);
	}

	private NoiseConfig(
			ChunkGeneratorSettings chunkGeneratorSettings,
			RegistryEntryLookup<DoublePerlinNoiseSampler.NoiseParameters> noiseParametersLookup,
			long seed
	) {
		randomDeriver = chunkGeneratorSettings.getRandomProvider().create(seed).nextSplitter();
		noiseParametersRegistry = noiseParametersLookup;
		aquiferRandomDeriver = randomDeriver.split(Identifier.ofVanilla("aquifer")).nextSplitter();
		oreRandomDeriver = randomDeriver.split(Identifier.ofVanilla("ore")).nextSplitter();
		noises = new ConcurrentHashMap<>();
		randomDerivers = new ConcurrentHashMap<>();
		surfaceBuilder = new SurfaceBuilder(
				this,
				chunkGeneratorSettings.defaultBlock(),
				chunkGeneratorSettings.seaLevel(),
				randomDeriver
		);

		final boolean useLegacyRandom = chunkGeneratorSettings.usesLegacyRandom();

		// Визитор применяет legacy-шум для старых миров и инициализирует сэмплеры
		class LegacyNoiseDensityFunctionVisitor implements DensityFunction.DensityFunctionVisitor {

			private final Map<DensityFunction, DensityFunction> cache = new HashMap<>();

			private Random createRandom(long noiseSeed) {
				return new CheckedRandom(noiseSeed + noiseSeed);
			}

			@Override
			public DensityFunction.Noise apply(DensityFunction.Noise noiseDensityFunction) {
				RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> registryEntry = noiseDensityFunction.noiseData();

				if (useLegacyRandom) {
					if (registryEntry.matchesKey(NoiseParametersKeys.TEMPERATURE)) {
						DoublePerlinNoiseSampler sampler = DoublePerlinNoiseSampler.createLegacy(
								createRandom(0L), new DoublePerlinNoiseSampler.NoiseParameters(-7, 1.0, 1.0)
						);
						return new DensityFunction.Noise(registryEntry, sampler);
					}

					if (registryEntry.matchesKey(NoiseParametersKeys.VEGETATION)) {
						DoublePerlinNoiseSampler sampler = DoublePerlinNoiseSampler.createLegacy(
								createRandom(1L), new DoublePerlinNoiseSampler.NoiseParameters(-7, 1.0, 1.0)
						);
						return new DensityFunction.Noise(registryEntry, sampler);
					}

					if (registryEntry.matchesKey(NoiseParametersKeys.OFFSET)) {
						DoublePerlinNoiseSampler sampler = DoublePerlinNoiseSampler.create(
								NoiseConfig.this.randomDeriver.split(NoiseParametersKeys.OFFSET.getValue()),
								new DoublePerlinNoiseSampler.NoiseParameters(0, 0.0)
						);
						return new DensityFunction.Noise(registryEntry, sampler);
					}
				}

				DoublePerlinNoiseSampler sampler = NoiseConfig.this.getOrCreateSampler(
						registryEntry.getKey().orElseThrow()
				);
				return new DensityFunction.Noise(registryEntry, sampler);
			}

			private DensityFunction applyNotCached(DensityFunction densityFunction) {
				if (densityFunction instanceof InterpolatedNoiseSampler interpolatedNoiseSampler) {
					Random random = useLegacyRandom
							? createRandom(0L)
							: NoiseConfig.this.randomDeriver.split(Identifier.ofVanilla("terrain"));
					return interpolatedNoiseSampler.copyWithRandom(random);
				}

				return (DensityFunction) (densityFunction instanceof DensityFunctionTypes.EndIslands
						? new DensityFunctionTypes.EndIslands(seed)
						: densityFunction
				);
			}

			@Override
			public DensityFunction apply(DensityFunction densityFunction) {
				return cache.computeIfAbsent(densityFunction, this::applyNotCached);
			}
		}

		noiseRouter = chunkGeneratorSettings.noiseRouter().apply(new LegacyNoiseDensityFunctionVisitor());

		DensityFunction.DensityFunctionVisitor unwrapVisitor = new DensityFunction.DensityFunctionVisitor() {
			private final Map<DensityFunction, DensityFunction> unwrapped = new HashMap<>();

			private DensityFunction unwrap(DensityFunction densityFunction) {
				if (densityFunction instanceof DensityFunctionTypes.RegistryEntryHolder registryEntryHolder) {
					return registryEntryHolder.function().value();
				}

				return densityFunction instanceof DensityFunctionTypes.Wrapping wrapping
						? wrapping.wrapped()
						: densityFunction;
			}

			@Override
			public DensityFunction apply(DensityFunction densityFunction) {
				return unwrapped.computeIfAbsent(densityFunction, this::unwrap);
			}
		};

		multiNoiseSampler = new MultiNoiseUtil.MultiNoiseSampler(
				noiseRouter.temperature().apply(unwrapVisitor),
				noiseRouter.vegetation().apply(unwrapVisitor),
				noiseRouter.continents().apply(unwrapVisitor),
				noiseRouter.erosion().apply(unwrapVisitor),
				noiseRouter.depth().apply(unwrapVisitor),
				noiseRouter.ridges().apply(unwrapVisitor),
				chunkGeneratorSettings.spawnTarget()
		);
	}

	public DoublePerlinNoiseSampler getOrCreateSampler(RegistryKey<DoublePerlinNoiseSampler.NoiseParameters> noiseParametersKey) {
		return noises.computeIfAbsent(
				noiseParametersKey,
				key -> NoiseParametersKeys.createNoiseSampler(noiseParametersRegistry, randomDeriver, noiseParametersKey)
		);
	}

	public RandomSplitter getOrCreateRandomDeriver(Identifier id) {
		return randomDerivers.computeIfAbsent(id, ignored -> randomDeriver.split(id).nextSplitter());
	}

	public NoiseRouter getNoiseRouter() {
		return noiseRouter;
	}

	public MultiNoiseUtil.MultiNoiseSampler getMultiNoiseSampler() {
		return multiNoiseSampler;
	}

	public SurfaceBuilder getSurfaceBuilder() {
		return surfaceBuilder;
	}

	public RandomSplitter getAquiferRandomDeriver() {
		return aquiferRandomDeriver;
	}

	public RandomSplitter getOreRandomDeriver() {
		return oreRandomDeriver;
	}
}
