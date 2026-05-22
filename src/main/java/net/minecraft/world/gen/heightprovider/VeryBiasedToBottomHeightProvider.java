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
 * Провайдер высоты с сильным смещением к нижней границе.
 * Использует тройную случайность для ещё более агрессивного смещения к минимуму
 * по сравнению с {@link BiasedToBottomHeightProvider}.
 */
public class VeryBiasedToBottomHeightProvider extends HeightProvider {

	public static final MapCodec<VeryBiasedToBottomHeightProvider> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    YOffset.OFFSET_CODEC.fieldOf("min_inclusive").forGetter(provider -> provider.minOffset),
					                    YOffset.OFFSET_CODEC.fieldOf("max_inclusive").forGetter(provider -> provider.maxOffset),
					                    Codec
							                    .intRange(1, Integer.MAX_VALUE)
							                    .optionalFieldOf("inner", 1)
							                    .forGetter(provider -> provider.inner)
			                    )
			                    .apply(instance, VeryBiasedToBottomHeightProvider::new)
	);
	private static final Logger LOGGER = LogUtils.getLogger();
	private final YOffset minOffset;
	private final YOffset maxOffset;
	private final int inner;

	private VeryBiasedToBottomHeightProvider(YOffset minOffset, YOffset maxOffset, int inner) {
		this.minOffset = minOffset;
		this.maxOffset = maxOffset;
		this.inner = inner;
	}

	public static VeryBiasedToBottomHeightProvider create(YOffset minOffset, YOffset maxOffset, int inner) {
		return new VeryBiasedToBottomHeightProvider(minOffset, maxOffset, inner);
	}

	@Override
	public int get(Random random, HeightContext context) {
		int minY = minOffset.getY(context);
		int maxY = maxOffset.getY(context);

		if (maxY - minY - inner + 1 <= 0) {
			LOGGER.warn("Empty height range: {}", this);
			return minY;
		}

		int upper = MathHelper.nextInt(random, minY + inner, maxY);
		int mid = MathHelper.nextInt(random, minY, upper - 1);
		return MathHelper.nextInt(random, minY, mid - 1 + inner);
	}

	@Override
	public HeightProviderType<?> getType() {
		return HeightProviderType.VERY_BIASED_TO_BOTTOM;
	}

	@Override
	public String toString() {
		return "biased[" + minOffset + "-" + maxOffset + " inner: " + inner + "]";
	}
}
