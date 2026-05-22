package net.minecraft.world.gen.stateprovider;

import com.mojang.datafixers.Products.P4;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.block.BlockState;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Поставщик состояний блоков на основе шума Перлина.
 * Значение шума в диапазоне [-1, 1] нормализуется в [0, 1) и используется
 * для выбора блока из списка {@code states} по индексу.
 */
public class NoiseBlockStateProvider extends AbstractNoiseBlockStateProvider {

	public static final MapCodec<NoiseBlockStateProvider> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillNoiseCodecFields(instance).apply(instance, NoiseBlockStateProvider::new)
	);
	protected final List<BlockState> states;

	protected static <P extends NoiseBlockStateProvider> P4<Mu<P>, Long, DoublePerlinNoiseSampler.NoiseParameters, Float, List<BlockState>> fillNoiseCodecFields(
			Instance<P> instance
	) {
		return fillCodecFields(instance)
				.and(
						Codecs.nonEmptyList(BlockState.CODEC.listOf())
								.fieldOf("states")
								.forGetter(provider -> provider.states)
				);
	}

	public NoiseBlockStateProvider(
			long seed,
			DoublePerlinNoiseSampler.NoiseParameters noiseParameters,
			float scale,
			List<BlockState> states
	) {
		super(seed, noiseParameters, scale);
		this.states = states;
	}

	@Override
	protected BlockStateProviderType<?> getType() {
		return BlockStateProviderType.NOISE_PROVIDER;
	}

	@Override
	public BlockState get(Random random, BlockPos pos) {
		return getStateFromList(states, pos, scale);
	}

	protected BlockState getStateFromList(List<BlockState> states, BlockPos pos, double scale) {
		double noiseValue = getNoiseValue(pos, scale);
		return getStateAtValue(states, noiseValue);
	}

	protected BlockState getStateAtValue(List<BlockState> states, double value) {
		double normalized = MathHelper.clamp((1.0 + value) / 2.0, 0.0, 0.9999);
		return states.get((int) (normalized * states.size()));
	}
}
