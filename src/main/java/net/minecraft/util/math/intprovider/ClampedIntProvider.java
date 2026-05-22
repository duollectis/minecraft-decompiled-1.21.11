package net.minecraft.util.math.intprovider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Поставщик целых чисел, зажимающий результат вложенного {@link IntProvider}
 * в заданный диапазон {@code [min, max]}. Позволяет ограничить любое распределение
 * жёсткими границами без изменения его формы внутри диапазона.
 */
public class ClampedIntProvider extends IntProvider {

	public static final MapCodec<ClampedIntProvider> CODEC = RecordCodecBuilder
		.<ClampedIntProvider>mapCodec(
			instance -> instance.group(
				IntProvider.VALUE_CODEC.fieldOf("source").forGetter(provider -> provider.source),
				Codec.INT.fieldOf("min_inclusive").forGetter(provider -> provider.min),
				Codec.INT.fieldOf("max_inclusive").forGetter(provider -> provider.max)
			).apply(instance, ClampedIntProvider::new)
		)
		.validate(
			provider -> provider.max < provider.min
				? DataResult.error(
					() -> "Max must be at least min, min_inclusive: " + provider.min + ", max_inclusive: " + provider.max
				)
				: DataResult.success(provider)
		);

	private final IntProvider source;
	private final int min;
	private final int max;

	public static ClampedIntProvider create(IntProvider source, int min, int max) {
		return new ClampedIntProvider(source, min, max);
	}

	public ClampedIntProvider(IntProvider source, int min, int max) {
		this.source = source;
		this.min = min;
		this.max = max;
	}

	@Override
	public int get(Random random) {
		return MathHelper.clamp(source.get(random), min, max);
	}

	@Override
	public int getMin() {
		return Math.max(min, source.getMin());
	}

	@Override
	public int getMax() {
		return Math.min(max, source.getMax());
	}

	@Override
	public IntProviderType<?> getType() {
		return IntProviderType.CLAMPED;
	}
}
