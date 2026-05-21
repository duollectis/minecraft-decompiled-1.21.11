package net.minecraft.world.gen.placementmodifier;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.FeaturePlacementContext;

import java.util.stream.Stream;

/**
 * {@code SquarePlacementModifier}.
 */
public class SquarePlacementModifier extends PlacementModifier {

	private static final SquarePlacementModifier INSTANCE = new SquarePlacementModifier();
	public static final MapCodec<SquarePlacementModifier> MODIFIER_CODEC = MapCodec.unit(() -> INSTANCE);

	/**
	 * Of.
	 *
	 * @return SquarePlacementModifier — результат операции
	 */
	public static SquarePlacementModifier of() {
		return INSTANCE;
	}

	@Override
	public Stream<BlockPos> getPositions(FeaturePlacementContext context, Random random, BlockPos pos) {
		int i = random.nextInt(16) + pos.getX();
		int j = random.nextInt(16) + pos.getZ();
		return Stream.of(new BlockPos(i, pos.getY(), j));
	}

	@Override
	public PlacementModifierType<?> getType() {
		return PlacementModifierType.IN_SQUARE;
	}
}
