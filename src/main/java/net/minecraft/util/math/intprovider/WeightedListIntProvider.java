package net.minecraft.util.math.intprovider;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.collection.Weighted;
import net.minecraft.util.math.random.Random;

/**
 * {@code WeightedListIntProvider}.
 */
public class WeightedListIntProvider extends IntProvider {

	public static final MapCodec<WeightedListIntProvider> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Pool
							.createNonEmptyCodec(IntProvider.VALUE_CODEC)
							.fieldOf("distribution")
							.forGetter(provider -> provider.weightedList))
					.apply(instance, WeightedListIntProvider::new)
	);
	private final Pool<IntProvider> weightedList;
	private final int min;
	private final int max;

	public WeightedListIntProvider(Pool<IntProvider> weightedList) {
		this.weightedList = weightedList;
		int i = Integer.MAX_VALUE;
		int j = Integer.MIN_VALUE;

		for (Weighted<IntProvider> weighted : weightedList.getEntries()) {
			int k = weighted.value().getMin();
			int l = weighted.value().getMax();
			i = Math.min(i, k);
			j = Math.max(j, l);
		}

		this.min = i;
		this.max = j;
	}

	@Override
	public int get(Random random) {
		return this.weightedList.get(random).get(random);
	}

	@Override
	public int getMin() {
		return this.min;
	}

	@Override
	public int getMax() {
		return this.max;
	}

	@Override
	public IntProviderType<?> getType() {
		return IntProviderType.WEIGHTED_LIST;
	}
}
