package net.minecraft.world.gen.placementmodifier;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.FeaturePlacementContext;

import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

/**
 * @deprecated Устаревший модификатор размещения, генерирующий позиции на каждом
 * горизонтальном слое в пределах чанка. Используйте современные альтернативы.
 */
@Deprecated
public class CountMultilayerPlacementModifier extends PlacementModifier {

	public static final MapCodec<CountMultilayerPlacementModifier> MODIFIER_CODEC =
		IntProvider.createValidatingCodec(0, 256)
			.fieldOf("count")
			.xmap(CountMultilayerPlacementModifier::new, modifier -> modifier.count);
	private final IntProvider count;

	private CountMultilayerPlacementModifier(IntProvider count) {
		this.count = count;
	}

	public static CountMultilayerPlacementModifier of(IntProvider count) {
		return new CountMultilayerPlacementModifier(count);
	}

	public static CountMultilayerPlacementModifier of(int count) {
		return of(ConstantIntProvider.create(count));
	}

	@Override
	public Stream<BlockPos> getPositions(FeaturePlacementContext context, Random random, BlockPos pos) {
		Builder<BlockPos> builder = Stream.builder();
		int layer = 0;

		boolean foundAny;
		do {
			foundAny = false;

			for (int attempt = 0; attempt < count.get(random); attempt++) {
				int x = random.nextInt(16) + pos.getX();
				int z = random.nextInt(16) + pos.getZ();
				int topY = context.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
				int foundY = findPos(context, x, topY, z, layer);

				if (foundY != Integer.MAX_VALUE) {
					builder.add(new BlockPos(x, foundY, z));
					foundAny = true;
				}
			}

			layer++;
		}
		while (foundAny);

		return builder.build();
	}

	@Override
	public PlacementModifierType<?> getType() {
		return PlacementModifierType.COUNT_ON_EVERY_LAYER;
	}

	private static int findPos(FeaturePlacementContext context, int x, int y, int z, int targetLayer) {
		BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);
		int layerIndex = 0;
		BlockState current = context.getBlockState(mutable);

		for (int scanY = y; scanY >= context.getBottomY() + 1; scanY--) {
			mutable.setY(scanY - 1);
			BlockState below = context.getBlockState(mutable);

			if (!blocksSpawn(below) && blocksSpawn(current) && !below.isOf(Blocks.BEDROCK)) {
				if (layerIndex == targetLayer) {
					return mutable.getY() + 1;
				}

				layerIndex++;
			}

			current = below;
		}

		return Integer.MAX_VALUE;
	}

	private static boolean blocksSpawn(BlockState state) {
		return state.isAir() || state.isOf(Blocks.WATER) || state.isOf(Blocks.LAVA);
	}
}
