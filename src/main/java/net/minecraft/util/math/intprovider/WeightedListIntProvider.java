package net.minecraft.util.math.intprovider;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.collection.Weighted;
import net.minecraft.util.math.random.Random;

/**
 * Поставщик целых чисел, выбирающий один из вложенных {@link IntProvider}
 * с учётом весов. Диапазон {@code [min, max]} вычисляется как объединение
 * диапазонов всех вложенных поставщиков.
 */
public class WeightedListIntProvider extends IntProvider {

	public static final MapCodec<WeightedListIntProvider> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(
				Pool.createNonEmptyCodec(IntProvider.VALUE_CODEC)
					.fieldOf("distribution")
					.forGetter(provider -> provider.weightedList)
			)
			.apply(instance, WeightedListIntProvider::new)
	);

	private final Pool<IntProvider> weightedList;
	private final int min;
	private final int max;

	public WeightedListIntProvider(Pool<IntProvider> weightedList) {
		this.weightedList = weightedList;

		int globalMin = Integer.MAX_VALUE;
		int globalMax = Integer.MIN_VALUE;

		for (Weighted<IntProvider> weighted : weightedList.getEntries()) {
			globalMin = Math.min(globalMin, weighted.value().getMin());
			globalMax = Math.max(globalMax, weighted.value().getMax());
		}

		min = globalMin;
		max = globalMax;
	}

	@Override
	public int get(Random random) {
		return weightedList.get(random).get(random);
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
		return IntProviderType.WEIGHTED_LIST;
	}
}
