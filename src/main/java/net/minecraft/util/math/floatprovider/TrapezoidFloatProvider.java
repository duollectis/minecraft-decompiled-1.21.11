package net.minecraft.util.math.floatprovider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.random.Random;

/**
 * Провайдер, генерирующий значения по трапециевидному распределению.
 * Плато задаёт ширину равновероятной центральной зоны, склоны — линейные.
 */
public class TrapezoidFloatProvider extends FloatProvider {

	public static final MapCodec<TrapezoidFloatProvider> CODEC = RecordCodecBuilder
		.<TrapezoidFloatProvider>mapCodec(
			instance -> instance.group(
				Codec.FLOAT.fieldOf("min").forGetter(provider -> provider.min),
				Codec.FLOAT.fieldOf("max").forGetter(provider -> provider.max),
				Codec.FLOAT.fieldOf("plateau").forGetter(provider -> provider.plateau)
			).apply(instance, TrapezoidFloatProvider::new)
		)
		.validate(provider -> {
			if (provider.max < provider.min) {
				return DataResult.error(
					() -> "Max must be larger than min: [" + provider.min + ", " + provider.max + "]"
				);
			}

			return provider.plateau > provider.max - provider.min
				? DataResult.error(
					() -> "Plateau can at most be the full span: [" + provider.min + ", " + provider.max + "]"
				)
				: DataResult.success(provider);
		});

	private final float min;
	private final float max;
	private final float plateau;

	public static TrapezoidFloatProvider create(float min, float max, float plateau) {
		return new TrapezoidFloatProvider(min, max, plateau);
	}

	private TrapezoidFloatProvider(float min, float max, float plateau) {
		this.min = min;
		this.max = max;
		this.plateau = plateau;
	}

	@Override
	public float get(Random random) {
		float span = max - min;
		float slopeWidth = (span - plateau) / 2.0F;
		float slopeAndPlateau = span - slopeWidth;
		return min + random.nextFloat() * slopeAndPlateau + random.nextFloat() * slopeWidth;
	}

	@Override
	public float getMin() {
		return min;
	}

	@Override
	public float getMax() {
		return max;
	}

	@Override
	public FloatProviderType<?> getType() {
		return FloatProviderType.TRAPEZOID;
	}

	@Override
	public String toString() {
		return "trapezoid(" + plateau + ") in [" + min + "-" + max + "]";
	}
}
