package net.minecraft.world.biome.source;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.stream.Stream;

/**
 * Источник биомов для измерения Края (The End).
 * Распределяет биомы на основе расстояния от центра и значения шума эрозии:
 * центральный остров → хайленды → мидленды → баррены → малые острова.
 */
public class TheEndBiomeSource extends BiomeSource {

	// Пороговые значения шума эрозии для определения биома
	private static final double HIGHLANDS_EROSION_THRESHOLD = 0.25;
	private static final double MIDLANDS_EROSION_THRESHOLD = -0.0625;
	private static final double SMALL_ISLANDS_EROSION_THRESHOLD = -0.21875;

	// Радиус центрального острова в квадрате чанк-секций (64 секции = 64*16 = 1024 блока)
	private static final long CENTER_ISLAND_RADIUS_SQUARED = 4096L;

	public static final MapCodec<TheEndBiomeSource> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			RegistryOps.getEntryCodec(BiomeKeys.THE_END),
			RegistryOps.getEntryCodec(BiomeKeys.END_HIGHLANDS),
			RegistryOps.getEntryCodec(BiomeKeys.END_MIDLANDS),
			RegistryOps.getEntryCodec(BiomeKeys.SMALL_END_ISLANDS),
			RegistryOps.getEntryCodec(BiomeKeys.END_BARRENS)
		)
		.apply(instance, instance.stable(TheEndBiomeSource::new))
	);

	private final RegistryEntry<Biome> centerBiome;
	private final RegistryEntry<Biome> highlandsBiome;
	private final RegistryEntry<Biome> midlandsBiome;
	private final RegistryEntry<Biome> smallIslandsBiome;
	private final RegistryEntry<Biome> barrensBiome;

	public static TheEndBiomeSource createVanilla(RegistryEntryLookup<Biome> biomeLookup) {
		return new TheEndBiomeSource(
			biomeLookup.getOrThrow(BiomeKeys.THE_END),
			biomeLookup.getOrThrow(BiomeKeys.END_HIGHLANDS),
			biomeLookup.getOrThrow(BiomeKeys.END_MIDLANDS),
			biomeLookup.getOrThrow(BiomeKeys.SMALL_END_ISLANDS),
			biomeLookup.getOrThrow(BiomeKeys.END_BARRENS)
		);
	}

	private TheEndBiomeSource(
		RegistryEntry<Biome> centerBiome,
		RegistryEntry<Biome> highlandsBiome,
		RegistryEntry<Biome> midlandsBiome,
		RegistryEntry<Biome> smallIslandsBiome,
		RegistryEntry<Biome> barrensBiome
	) {
		this.centerBiome = centerBiome;
		this.highlandsBiome = highlandsBiome;
		this.midlandsBiome = midlandsBiome;
		this.smallIslandsBiome = smallIslandsBiome;
		this.barrensBiome = barrensBiome;
	}

	@Override
	protected Stream<RegistryEntry<Biome>> biomeStream() {
		return Stream.of(centerBiome, highlandsBiome, midlandsBiome, smallIslandsBiome, barrensBiome);
	}

	@Override
	protected MapCodec<? extends BiomeSource> getCodec() {
		return CODEC;
	}

	/**
	 * Определяет биом Края по координатам шума.
	 * Центральный остров определяется по расстоянию от начала координат в секциях чанков.
	 * Для остальных территорий биом выбирается по значению шума эрозии в центре секции.
	 */
	@Override
	public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
		int blockX = BiomeCoords.toBlock(x);
		int blockY = BiomeCoords.toBlock(y);
		int blockZ = BiomeCoords.toBlock(z);
		int sectionX = ChunkSectionPos.getSectionCoord(blockX);
		int sectionZ = ChunkSectionPos.getSectionCoord(blockZ);

		if ((long) sectionX * sectionX + (long) sectionZ * sectionZ <= CENTER_ISLAND_RADIUS_SQUARED) {
			return centerBiome;
		}

		// Сэмплируем шум в центре секции для более стабильного определения биома
		int noiseSampleX = (ChunkSectionPos.getSectionCoord(blockX) * 2 + 1) * 8;
		int noiseSampleZ = (ChunkSectionPos.getSectionCoord(blockZ) * 2 + 1) * 8;
		double erosion = noise.erosion().sample(new DensityFunction.UnblendedNoisePos(noiseSampleX, blockY, noiseSampleZ));

		if (erosion > HIGHLANDS_EROSION_THRESHOLD) {
			return highlandsBiome;
		}

		if (erosion >= MIDLANDS_EROSION_THRESHOLD) {
			return midlandsBiome;
		}

		return erosion < SMALL_ISLANDS_EROSION_THRESHOLD ? smallIslandsBiome : barrensBiome;
	}
}
