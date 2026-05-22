package net.minecraft.world.gen.placementmodifier;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.FeaturePlacementContext;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Базовый класс счётного модификатора — повторяет входную позицию
 * {@link #getCount} раз, создавая несколько попыток размещения.
 */
public abstract class AbstractCountPlacementModifier extends PlacementModifier {

	protected abstract int getCount(Random random, BlockPos pos);

	@Override
	public Stream<BlockPos> getPositions(FeaturePlacementContext context, Random random, BlockPos pos) {
		return IntStream.range(0, getCount(random, pos)).mapToObj(ignored -> pos);
	}
}
