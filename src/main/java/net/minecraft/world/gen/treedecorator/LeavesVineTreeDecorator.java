package net.minecraft.world.gen.treedecorator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.VineBlock;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Декоратор дерева, размещающий лозу (vine) на листьях дерева.
 * Для каждого листового блока с заданной вероятностью проверяются четыре горизонтальных
 * направления, и если соседний блок является воздухом, там размещается свисающая лоза.
 */
public class LeavesVineTreeDecorator extends TreeDecorator {

	public static final MapCodec<LeavesVineTreeDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
	                                                                   .fieldOf("probability")
	                                                                   .xmap(
			                                                                   LeavesVineTreeDecorator::new,
			                                                                   treeDecorator -> treeDecorator.probability
	                                                                   );
	private final float probability;

	@Override
	protected TreeDecoratorType<?> getType() {
		return TreeDecoratorType.LEAVE_VINE;
	}

	public LeavesVineTreeDecorator(float probability) {
		this.probability = probability;
	}

	@Override
	public void generate(TreeDecorator.Generator generator) {
		Random random = generator.getRandom();

		generator.getLeavesPositions().forEach(pos -> {
			BlockPos west = pos.west();
			if (random.nextFloat() < probability && generator.isAir(west)) {
				placeVines(west, VineBlock.EAST, generator);
			}

			BlockPos east = pos.east();
			if (random.nextFloat() < probability && generator.isAir(east)) {
				placeVines(east, VineBlock.WEST, generator);
			}

			BlockPos north = pos.north();
			if (random.nextFloat() < probability && generator.isAir(north)) {
				placeVines(north, VineBlock.SOUTH, generator);
			}

			BlockPos south = pos.south();
			if (random.nextFloat() < probability && generator.isAir(south)) {
				placeVines(south, VineBlock.NORTH, generator);
			}
		});
	}

	private static void placeVines(BlockPos pos, BooleanProperty faceProperty, TreeDecorator.Generator generator) {
		generator.replaceWithVine(pos, faceProperty);
		int remaining = 4;

		for (BlockPos current = pos.down(); generator.isAir(current) && remaining > 0; remaining--) {
			generator.replaceWithVine(current, faceProperty);
			current = current.down();
		}
	}
}
