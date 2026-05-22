package net.minecraft.world.gen.treedecorator;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.VineBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Декоратор дерева, размещающий лозу (vine) на брёвнах ствола.
 * Для каждого бревна с вероятностью 2/3 проверяются четыре горизонтальных направления,
 * и если соседний блок является воздухом, там размещается лоза.
 */
public class TrunkVineTreeDecorator extends TreeDecorator {

	public static final MapCodec<TrunkVineTreeDecorator> CODEC = MapCodec.unit(() -> TrunkVineTreeDecorator.INSTANCE);
	public static final TrunkVineTreeDecorator INSTANCE = new TrunkVineTreeDecorator();

	@Override
	protected TreeDecoratorType<?> getType() {
		return TreeDecoratorType.TRUNK_VINE;
	}

	@Override
	public void generate(TreeDecorator.Generator generator) {
		Random random = generator.getRandom();

		generator.getLogPositions().forEach(pos -> {
			BlockPos west = pos.west();
			if (random.nextInt(3) > 0 && generator.isAir(west)) {
				generator.replaceWithVine(west, VineBlock.EAST);
			}

			BlockPos east = pos.east();
			if (random.nextInt(3) > 0 && generator.isAir(east)) {
				generator.replaceWithVine(east, VineBlock.WEST);
			}

			BlockPos north = pos.north();
			if (random.nextInt(3) > 0 && generator.isAir(north)) {
				generator.replaceWithVine(north, VineBlock.SOUTH);
			}

			BlockPos south = pos.south();
			if (random.nextInt(3) > 0 && generator.isAir(south)) {
				generator.replaceWithVine(south, VineBlock.NORTH);
			}
		});
	}
}
