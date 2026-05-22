package net.minecraft.world.gen.placementmodifier;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.FeaturePlacementContext;

import java.util.stream.Stream;

/**
 * Модификатор размещения, случайно смещающий позицию в пределах чанка (16×16)
 * по осям X и Z, сохраняя исходную высоту Y.
 */
public class SquarePlacementModifier extends PlacementModifier {

	private static final SquarePlacementModifier INSTANCE = new SquarePlacementModifier();
	public static final MapCodec<SquarePlacementModifier> MODIFIER_CODEC = MapCodec.unit(() -> INSTANCE);

	public static SquarePlacementModifier of() {
		return INSTANCE;
	}

	@Override
	public Stream<BlockPos> getPositions(FeaturePlacementContext context, Random random, BlockPos pos) {
		int x = random.nextInt(16) + pos.getX();
		int z = random.nextInt(16) + pos.getZ();
		return Stream.of(new BlockPos(x, pos.getY(), z));
	}

	@Override
	public PlacementModifierType<?> getType() {
		return PlacementModifierType.IN_SQUARE;
	}
}
