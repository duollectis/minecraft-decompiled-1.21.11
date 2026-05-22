package net.minecraft.world.biome.source;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Источник биомов, всегда возвращающий один и тот же биом для любых координат.
 * Используется в суперплоском мире и в тестовых сценариях.
 */
public class FixedBiomeSource extends BiomeSource implements BiomeAccess.Storage {

	public static final MapCodec<FixedBiomeSource> CODEC = Biome.REGISTRY_CODEC
		.fieldOf("biome")
		.xmap(FixedBiomeSource::new, source -> source.biome)
		.stable();

	private final RegistryEntry<Biome> biome;

	public FixedBiomeSource(RegistryEntry<Biome> biome) {
		this.biome = biome;
	}

	@Override
	protected Stream<RegistryEntry<Biome>> biomeStream() {
		return Stream.of(biome);
	}

	@Override
	protected MapCodec<? extends BiomeSource> getCodec() {
		return CODEC;
	}

	@Override
	public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
		return biome;
	}

	@Override
	public RegistryEntry<Biome> getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
		return biome;
	}

	@Override
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
		if (!predicate.test(biome)) {
			return null;
		}

		return returnFirst
			? Pair.of(new BlockPos(x, y, z), biome)
			: Pair.of(
				new BlockPos(
					x - radius + random.nextInt(radius * 2 + 1),
					y,
					z - radius + random.nextInt(radius * 2 + 1)
				),
				biome
			);
	}

	@Override
	public @Nullable Pair<BlockPos, RegistryEntry<Biome>> locateBiome(
		BlockPos origin,
		int radius,
		int horizontalBlockCheckInterval,
		int verticalBlockCheckInterval,
		Predicate<RegistryEntry<Biome>> predicate,
		MultiNoiseUtil.MultiNoiseSampler noiseSampler,
		WorldView world
	) {
		return predicate.test(biome)
			? Pair.of(
				origin.withY(MathHelper.clamp(
					origin.getY(),
					world.getBottomY() + 1,
					world.getTopYInclusive() + 1
				)),
				biome
			)
			: null;
	}

	@Override
	public Set<RegistryEntry<Biome>> getBiomesInArea(
		int x,
		int y,
		int z,
		int radius,
		MultiNoiseUtil.MultiNoiseSampler sampler
	) {
		return Sets.newHashSet(Set.of(biome));
	}
}
