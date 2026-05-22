package net.minecraft.world.gen.stateprovider;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.dynamic.Range;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Поставщик состояний блоков с двойным шумом Перлина.
 * Медленный шум определяет количество вариантов блоков (variety), быстрый — выбирает конкретный вариант.
 * Это создаёт плавные переходы между разными блоками в биомах.
 */
public class DualNoiseBlockStateProvider extends NoiseBlockStateProvider {

	public static final MapCodec<DualNoiseBlockStateProvider> DUAL_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Range
							.createRangedCodec(Codec.INT, 1, 64)
							.fieldOf("variety")
							.forGetter(provider -> provider.variety),
					DoublePerlinNoiseSampler.NoiseParameters.CODEC
							.fieldOf("slow_noise")
							.forGetter(provider -> provider.slowNoiseParameters),
					Codecs.POSITIVE_FLOAT
							.fieldOf("slow_scale")
							.forGetter(provider -> provider.slowScale)
			)
			.and(fillNoiseCodecFields(instance))
			.apply(instance, DualNoiseBlockStateProvider::new)
	);
	private final Range<Integer> variety;
	private final DoublePerlinNoiseSampler.NoiseParameters slowNoiseParameters;
	private final float slowScale;
	private final DoublePerlinNoiseSampler slowNoiseSampler;

	public DualNoiseBlockStateProvider(
			Range<Integer> variety,
			DoublePerlinNoiseSampler.NoiseParameters slowNoiseParameters,
			float slowScale,
			long seed,
			DoublePerlinNoiseSampler.NoiseParameters noiseParameters,
			float scale,
			List<BlockState> states
	) {
		super(seed, noiseParameters, scale, states);
		this.variety = variety;
		this.slowNoiseParameters = slowNoiseParameters;
		this.slowScale = slowScale;
		this.slowNoiseSampler = DoublePerlinNoiseSampler.create(new ChunkRandom(new CheckedRandom(seed)), slowNoiseParameters);
	}

	@Override
	protected BlockStateProviderType<?> getType() {
		return BlockStateProviderType.DUAL_NOISE_PROVIDER;
	}

	@Override
	public BlockState get(Random random, BlockPos pos) {
		double slowNoise = getSlowNoiseValue(pos);
		int variantCount = (int) MathHelper.clampedMap(
				slowNoise,
				-1.0,
				1.0,
				variety.minInclusive().intValue(),
				variety.maxInclusive() + 1
		);
		List<BlockState> variants = Lists.newArrayListWithCapacity(variantCount);

		for (int index = 0; index < variantCount; index++) {
			variants.add(getStateAtValue(states, getSlowNoiseValue(pos.add(index * 54545, 0, index * 34234))));
		}

		return getStateFromList(variants, pos, scale);
	}

	protected double getSlowNoiseValue(BlockPos pos) {
		return slowNoiseSampler.sample(
				pos.getX() * slowScale,
				pos.getY() * slowScale,
				pos.getZ() * slowScale
		);
	}
}
