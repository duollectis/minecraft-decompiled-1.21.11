package net.minecraft.world.gen.heightprovider;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.YOffset;
import org.slf4j.Logger;

/**
 * Провайдер высоты с трапециевидным распределением.
 * При plateau=0 даёт треугольное распределение (пик в центре),
 * при plateau>0 — плоскую вершину трапеции.
 */
public class TrapezoidHeightProvider extends HeightProvider {

	public static final MapCodec<TrapezoidHeightProvider> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    YOffset.OFFSET_CODEC.fieldOf("min_inclusive").forGetter(provider -> provider.minOffset),
					                    YOffset.OFFSET_CODEC.fieldOf("max_inclusive").forGetter(provider -> provider.maxOffset),
					                    Codec.INT
							                    .optionalFieldOf("plateau", 0)
							                    .forGetter(trapezoidHeightProvider -> trapezoidHeightProvider.plateau)
			                    )
			                    .apply(instance, TrapezoidHeightProvider::new)
	);
	private static final Logger LOGGER = LogUtils.getLogger();
	private final YOffset minOffset;
	private final YOffset maxOffset;
	private final int plateau;

	private TrapezoidHeightProvider(YOffset minOffset, YOffset maxOffset, int plateau) {
		this.minOffset = minOffset;
		this.maxOffset = maxOffset;
		this.plateau = plateau;
	}

	public static TrapezoidHeightProvider create(YOffset minOffset, YOffset maxOffset, int plateau) {
		return new TrapezoidHeightProvider(minOffset, maxOffset, plateau);
	}

	public static TrapezoidHeightProvider create(YOffset minOffset, YOffset maxOffset) {
		return create(minOffset, maxOffset, 0);
	}

	@Override
	public int get(Random random, HeightContext context) {
		int minY = minOffset.getY(context);
		int maxY = maxOffset.getY(context);

		if (minY > maxY) {
			LOGGER.warn("Empty height range: {}", this);
			return minY;
		}

		int range = maxY - minY;

		if (plateau >= range) {
			return MathHelper.nextBetween(random, minY, maxY);
		}

		int slopeWidth = (range - plateau) / 2;
		int maxSlope = range - slopeWidth;
		return minY + MathHelper.nextBetween(random, 0, maxSlope) + MathHelper.nextBetween(random, 0, slopeWidth);
	}

	@Override
	public HeightProviderType<?> getType() {
		return HeightProviderType.TRAPEZOID;
	}

	@Override
	public String toString() {
		return plateau == 0
				? "triangle (" + minOffset + "-" + maxOffset + ")"
				: "trapezoid(" + plateau + ") in [" + minOffset + "-" + maxOffset + "]";
	}
}
