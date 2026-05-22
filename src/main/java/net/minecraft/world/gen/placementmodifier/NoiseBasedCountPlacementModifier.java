package net.minecraft.world.gen.placementmodifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;

/**
 * Модификатор размещения, вычисляющий количество попыток на основе значения
 * шума листвы в данной XZ-позиции.
 */
public class NoiseBasedCountPlacementModifier extends AbstractCountPlacementModifier {

	public static final MapCodec<NoiseBasedCountPlacementModifier> MODIFIER_CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Codec.INT
				.fieldOf("noise_to_count_ratio")
				.forGetter(modifier -> modifier.noiseToCountRatio),
			Codec.DOUBLE
				.fieldOf("noise_factor")
				.forGetter(modifier -> modifier.noiseFactor),
			Codec.DOUBLE
				.fieldOf("noise_offset")
				.orElse(0.0)
				.forGetter(modifier -> modifier.noiseOffset)
		)
		.apply(instance, NoiseBasedCountPlacementModifier::new)
	);
	private final int noiseToCountRatio;
	private final double noiseFactor;
	private final double noiseOffset;

	private NoiseBasedCountPlacementModifier(int noiseToCountRatio, double noiseFactor, double noiseOffset) {
		this.noiseToCountRatio = noiseToCountRatio;
		this.noiseFactor = noiseFactor;
		this.noiseOffset = noiseOffset;
	}

	public static NoiseBasedCountPlacementModifier of(int noiseToCountRatio, double noiseFactor, double noiseOffset) {
		return new NoiseBasedCountPlacementModifier(noiseToCountRatio, noiseFactor, noiseOffset);
	}

	@Override
	protected int getCount(Random random, BlockPos pos) {
		double noiseValue = Biome.FOLIAGE_NOISE.sample(pos.getX() / noiseFactor, pos.getZ() / noiseFactor, false);
		return (int) Math.ceil((noiseValue + noiseOffset) * noiseToCountRatio);
	}

	@Override
	public PlacementModifierType<?> getType() {
		return PlacementModifierType.NOISE_BASED_COUNT;
	}
}
