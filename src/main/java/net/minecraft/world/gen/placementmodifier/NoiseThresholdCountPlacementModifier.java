package net.minecraft.world.gen.placementmodifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;

/**
 * Модификатор размещения, выбирающий количество попыток в зависимости от того,
 * превышает ли значение шума листвы заданный порог.
 */
public class NoiseThresholdCountPlacementModifier extends AbstractCountPlacementModifier {

	private static final double NOISE_SCALE = 200.0;

	public static final MapCodec<NoiseThresholdCountPlacementModifier> MODIFIER_CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Codec.DOUBLE.fieldOf("noise_level").forGetter(modifier -> modifier.noiseLevel),
			Codec.INT.fieldOf("below_noise").forGetter(modifier -> modifier.belowNoise),
			Codec.INT.fieldOf("above_noise").forGetter(modifier -> modifier.aboveNoise)
		)
		.apply(instance, NoiseThresholdCountPlacementModifier::new)
	);
	private final double noiseLevel;
	private final int belowNoise;
	private final int aboveNoise;

	private NoiseThresholdCountPlacementModifier(double noiseLevel, int belowNoise, int aboveNoise) {
		this.noiseLevel = noiseLevel;
		this.belowNoise = belowNoise;
		this.aboveNoise = aboveNoise;
	}

	public static NoiseThresholdCountPlacementModifier of(double noiseLevel, int belowNoise, int aboveNoise) {
		return new NoiseThresholdCountPlacementModifier(noiseLevel, belowNoise, aboveNoise);
	}

	@Override
	protected int getCount(Random random, BlockPos pos) {
		double noiseValue = Biome.FOLIAGE_NOISE.sample(pos.getX() / NOISE_SCALE, pos.getZ() / NOISE_SCALE, false);
		return noiseValue < noiseLevel ? belowNoise : aboveNoise;
	}

	@Override
	public PlacementModifierType<?> getType() {
		return PlacementModifierType.NOISE_THRESHOLD_COUNT;
	}
}
