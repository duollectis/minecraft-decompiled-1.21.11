package net.minecraft.util.math.intprovider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.random.Random;

/**
 * Поставщик целых чисел с равномерным распределением, смещённым к нижней границе диапазона.
 * Алгоритм: {@code min + rand(rand(max - min + 1) + 1)} — двойная рандомизация
 * делает значения, близкие к {@code min}, значительно более вероятными.
 */
public class BiasedToBottomIntProvider extends IntProvider {

	public static final MapCodec<BiasedToBottomIntProvider> CODEC = RecordCodecBuilder
		.<BiasedToBottomIntProvider>mapCodec(
			instance -> instance.group(
				Codec.INT.fieldOf("min_inclusive").forGetter(provider -> provider.min),
				Codec.INT.fieldOf("max_inclusive").forGetter(provider -> provider.max)
			).apply(instance, BiasedToBottomIntProvider::new)
		)
		.validate(
			provider -> provider.max < provider.min
				? DataResult.error(
					() -> "Max must be at least min, min_inclusive: " + provider.min + ", max_inclusive: " + provider.max
				)
				: DataResult.success(provider)
		);

	private final int min;
	private final int max;

	private BiasedToBottomIntProvider(int min, int max) {
		this.min = min;
		this.max = max;
	}

	public static BiasedToBottomIntProvider create(int min, int max) {
		return new BiasedToBottomIntProvider(min, max);
	}

	@Override
	public int get(Random random) {
		return min + random.nextInt(random.nextInt(max - min + 1) + 1);
	}

	@Override
	public int getMin() {
		return min;
	}

	@Override
	public int getMax() {
		return max;
	}

	@Override
	public IntProviderType<?> getType() {
		return IntProviderType.BIASED_TO_BOTTOM;
	}

	@Override
	public String toString() {
		return "[" + min + "-" + max + "]";
	}
}
