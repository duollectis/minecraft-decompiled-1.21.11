package net.minecraft.world.gen.placementmodifier;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.FeaturePlacementContext;

import java.util.stream.Stream;

/**
 * Базовый класс условного модификатора размещения — либо пропускает позицию,
 * либо отфильтровывает её в зависимости от результата {@link #shouldPlace}.
 */
public abstract class AbstractConditionalPlacementModifier extends PlacementModifier {

	@Override
	public final Stream<BlockPos> getPositions(FeaturePlacementContext context, Random random, BlockPos pos) {
		return shouldPlace(context, random, pos) ? Stream.of(pos) : Stream.of();
	}

	protected abstract boolean shouldPlace(FeaturePlacementContext context, Random random, BlockPos pos);
}
