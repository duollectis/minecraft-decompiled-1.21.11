package net.minecraft.world.gen.heightprovider;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.YOffset;
import org.slf4j.Logger;

/**
 * Провайдер высоты со смещением к нижней границе диапазона.
 * Использует двойную случайность: сначала выбирается диапазон, затем позиция внутри него,
 * что создаёт нелинейное распределение с концентрацией значений у минимума.
 */
public class BiasedToBottomHeightProvider extends HeightProvider {

	public static final MapCodec<BiasedToBottomHeightProvider> BIASED_TO_BOTTOM_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    YOffset.OFFSET_CODEC.fieldOf("min_inclusive").forGetter(provider -> provider.minOffset),
					                    YOffset.OFFSET_CODEC.fieldOf("max_inclusive").forGetter(provider -> provider.maxOffset),
					                    Codec
							                    .intRange(1, Integer.MAX_VALUE)
							                    .optionalFieldOf("inner", 1)
							                    .forGetter(provider -> provider.inner)
			                    )
			                    .apply(instance, BiasedToBottomHeightProvider::new)
	);
	private static final Logger LOGGER = LogUtils.getLogger();
	private final YOffset minOffset;
	private final YOffset maxOffset;
	private final int inner;

	private BiasedToBottomHeightProvider(YOffset minOffset, YOffset maxOffset, int inner) {
		this.minOffset = minOffset;
		this.maxOffset = maxOffset;
		this.inner = inner;
	}

	public static BiasedToBottomHeightProvider create(YOffset minOffset, YOffset maxOffset, int inner) {
		return new BiasedToBottomHeightProvider(minOffset, maxOffset, inner);
	}

	@Override
	public int get(Random random, HeightContext context) {
		int minY = minOffset.getY(context);
		int maxY = maxOffset.getY(context);

		if (maxY - minY - inner + 1 <= 0) {
			LOGGER.warn("Empty height range: {}", this);
			return minY;
		}

		int range = random.nextInt(maxY - minY - inner + 1);
		return random.nextInt(range + inner) + minY;
	}

	@Override
	public HeightProviderType<?> getType() {
		return HeightProviderType.BIASED_TO_BOTTOM;
	}

	@Override
	public String toString() {
		return "biased[" + minOffset + "-" + maxOffset + " inner: " + inner + "]";
	}
}
