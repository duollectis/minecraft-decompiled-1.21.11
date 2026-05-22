package net.minecraft.world.biome.source;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Абстрактный источник биомов, определяющий, какой биом находится в заданных координатах.
 * Кэширует множество всех биомов через {@link Suppliers#memoize}.
 */
public abstract class BiomeSource implements BiomeSupplier {

	public static final Codec<BiomeSource> CODEC =
		Registries.BIOME_SOURCE.getCodec().dispatchStable(BiomeSource::getCodec, Function.identity());

	private final Supplier<Set<RegistryEntry<Biome>>> biomes =
		Suppliers.memoize(() -> biomeStream().distinct().collect(ImmutableSet.toImmutableSet()));

	protected BiomeSource() {
	}

	protected abstract MapCodec<? extends BiomeSource> getCodec();

	protected abstract Stream<RegistryEntry<Biome>> biomeStream();

	public Set<RegistryEntry<Biome>> getBiomes() {
		return biomes.get();
	}

	/**
	 * Возвращает множество уникальных биомов в кубической области вокруг заданной точки.
	 * Область задаётся в блочных координатах, но сэмплирование происходит в биомных.
	 */
	public Set<RegistryEntry<Biome>> getBiomesInArea(
		int x,
		int y,
		int z,
		int radius,
		MultiNoiseUtil.MultiNoiseSampler sampler
	) {
		int minBiomeX = BiomeCoords.fromBlock(x - radius);
		int minBiomeY = BiomeCoords.fromBlock(y - radius);
		int minBiomeZ = BiomeCoords.fromBlock(z - radius);
		int maxBiomeX = BiomeCoords.fromBlock(x + radius);
		int maxBiomeY = BiomeCoords.fromBlock(y + radius);
		int maxBiomeZ = BiomeCoords.fromBlock(z + radius);
		int rangeX = maxBiomeX - minBiomeX + 1;
		int rangeY = maxBiomeY - minBiomeY + 1;
		int rangeZ = maxBiomeZ - minBiomeZ + 1;
		Set<RegistryEntry<Biome>> result = Sets.newHashSet();

		for (int dz = 0; dz < rangeZ; dz++) {
			for (int dx = 0; dx < rangeX; dx++) {
				for (int dy = 0; dy < rangeY; dy++) {
					result.add(getBiome(minBiomeX + dx, minBiomeY + dy, minBiomeZ + dz, sampler));
				}
			}
		}

		return result;
	}

	public @Nullable Pair<BlockPos, RegistryEntry<Biome>> locateBiome(
		int x,
		int y,
		int z,
		int radius,
		Predicate<RegistryEntry<Biome>> predicate,
		Random random,
		MultiNoiseUtil.MultiNoiseSampler noiseSampler
	) {
		return locateBiome(x, y, z, radius, 1, predicate, random, false, noiseSampler);
	}

	/**
	 * Ищет ближайшую позицию с биомом, удовлетворяющим предикату, в вертикальном диапазоне мира.
	 * Перебирает позиции по спирали с заданным горизонтальным шагом и всеми допустимыми Y-уровнями.
	 */
	public @Nullable Pair<BlockPos, RegistryEntry<Biome>> locateBiome(
		BlockPos origin,
		int radius,
		int horizontalBlockCheckInterval,
		int verticalBlockCheckInterval,
		Predicate<RegistryEntry<Biome>> predicate,
		MultiNoiseUtil.MultiNoiseSampler noiseSampler,
		WorldView world
	) {
		Set<RegistryEntry<Biome>> matchingBiomes =
			getBiomes().stream().filter(predicate).collect(Collectors.toUnmodifiableSet());

		if (matchingBiomes.isEmpty()) {
			return null;
		}

		int spiralRadius = Math.floorDiv(radius, horizontalBlockCheckInterval);
		int[] yLevels = MathHelper
			.stream(
				origin.getY(),
				world.getBottomY() + 1,
				world.getTopYInclusive() + 1,
				verticalBlockCheckInterval
			)
			.toArray();

		for (BlockPos.Mutable mutable : BlockPos.iterateInSquare(
			BlockPos.ORIGIN,
			spiralRadius,
			Direction.EAST,
			Direction.SOUTH
		)) {
			int blockX = origin.getX() + mutable.getX() * horizontalBlockCheckInterval;
			int blockZ = origin.getZ() + mutable.getZ() * horizontalBlockCheckInterval;
			int biomeX = BiomeCoords.fromBlock(blockX);
			int biomeZ = BiomeCoords.fromBlock(blockZ);

			for (int yLevel : yLevels) {
				int biomeY = BiomeCoords.fromBlock(yLevel);
				RegistryEntry<Biome> biome = getBiome(biomeX, biomeY, biomeZ, noiseSampler);

				if (matchingBiomes.contains(biome)) {
					return Pair.of(new BlockPos(blockX, yLevel, blockZ), biome);
				}
			}
		}

		return null;
	}

	/**
	 * Ищет биом по спирали с опциональным режимом «первое совпадение» ({@code returnFirst=true})
	 * или случайного выбора среди всех найденных ({@code returnFirst=false}).
	 */
	public @Nullable Pair<BlockPos, RegistryEntry<Biome>> locateBiome(
		int x,
		int y,
		int z,
		int radius,
		int blockCheckInterval,
		Predicate<RegistryEntry<Biome>> predicate,
		Random random,
		boolean returnFirst,
		MultiNoiseUtil.MultiNoiseSampler noiseSampler
	) {
		int biomeX = BiomeCoords.fromBlock(x);
		int biomeZ = BiomeCoords.fromBlock(z);
		int biomeRadius = BiomeCoords.fromBlock(radius);
		int biomeY = BiomeCoords.fromBlock(y);
		Pair<BlockPos, RegistryEntry<Biome>> bestResult = null;
		int foundCount = 0;
		int startRing = returnFirst ? 0 : biomeRadius;
		int currentRing = startRing;

		while (currentRing <= biomeRadius) {
			for (int dz = !SharedConstants.ONLY_GENERATE_HALF_THE_WORLD && !SharedConstants.DEBUG_BIOME_SOURCE
				? -currentRing
				: 0;
			     dz <= currentRing;
			     dz += blockCheckInterval
			) {
				boolean onZEdge = Math.abs(dz) == currentRing;

				for (int dx = -currentRing; dx <= currentRing; dx += blockCheckInterval) {
					if (returnFirst) {
						boolean onXEdge = Math.abs(dx) == currentRing;

						if (!onXEdge && !onZEdge) {
							continue;
						}
					}

					int sampleX = biomeX + dx;
					int sampleZ = biomeZ + dz;
					RegistryEntry<Biome> biome = getBiome(sampleX, biomeY, sampleZ, noiseSampler);

					if (predicate.test(biome)) {
						if (bestResult == null || random.nextInt(foundCount + 1) == 0) {
							BlockPos blockPos = new BlockPos(
								BiomeCoords.toBlock(sampleX),
								y,
								BiomeCoords.toBlock(sampleZ)
							);

							if (returnFirst) {
								return Pair.of(blockPos, biome);
							}

							bestResult = Pair.of(blockPos, biome);
						}

						foundCount++;
					}
				}
			}

			currentRing += blockCheckInterval;
		}

		return bestResult;
	}

	@Override
	public abstract RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise);

	public void addDebugInfo(List<String> info, BlockPos pos, MultiNoiseUtil.MultiNoiseSampler noiseSampler) {
	}
}
