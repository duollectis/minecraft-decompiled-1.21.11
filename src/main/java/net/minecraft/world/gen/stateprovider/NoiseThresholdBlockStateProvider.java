package net.minecraft.world.gen.stateprovider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Поставщик состояний блоков с пороговым шумом Перлина.
 * Если значение шума ниже порога — возвращает случайный блок из {@code lowStates},
 * иначе с вероятностью {@code highChance} — из {@code highStates}, или {@code defaultState}.
 */
public class NoiseThresholdBlockStateProvider extends AbstractNoiseBlockStateProvider {

	public static final MapCodec<NoiseThresholdBlockStateProvider> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillCodecFields(instance)
					.and(
							instance.group(
									Codec.floatRange(-1.0F, 1.0F)
											.fieldOf("threshold")
											.forGetter(provider -> provider.threshold),
									Codec.floatRange(0.0F, 1.0F)
											.fieldOf("high_chance")
											.forGetter(provider -> provider.highChance),
									BlockState.CODEC
											.fieldOf("default_state")
											.forGetter(provider -> provider.defaultState),
									Codecs.nonEmptyList(BlockState.CODEC.listOf())
											.fieldOf("low_states")
											.forGetter(provider -> provider.lowStates),
									Codecs.nonEmptyList(BlockState.CODEC.listOf())
											.fieldOf("high_states")
											.forGetter(provider -> provider.highStates)
							)
					)
					.apply(instance, NoiseThresholdBlockStateProvider::new)
	);
	private final float threshold;
	private final float highChance;
	private final BlockState defaultState;
	private final List<BlockState> lowStates;
	private final List<BlockState> highStates;

	public NoiseThresholdBlockStateProvider(
			long seed,
			DoublePerlinNoiseSampler.NoiseParameters noiseParameters,
			float scale,
			float threshold,
			float highChance,
			BlockState defaultState,
			List<BlockState> lowStates,
			List<BlockState> highStates
	) {
		super(seed, noiseParameters, scale);
		this.threshold = threshold;
		this.highChance = highChance;
		this.defaultState = defaultState;
		this.lowStates = lowStates;
		this.highStates = highStates;
	}

	@Override
	protected BlockStateProviderType<?> getType() {
		return BlockStateProviderType.NOISE_THRESHOLD_PROVIDER;
	}

	@Override
	public BlockState get(Random random, BlockPos pos) {
		double noiseValue = getNoiseValue(pos, scale);

		if (noiseValue < threshold) {
			return Util.getRandom(lowStates, random);
		}

		return random.nextFloat() < highChance
				? Util.getRandom(highStates, random)
				: defaultState;
	}
}
