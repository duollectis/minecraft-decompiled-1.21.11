package net.minecraft.world.gen.treedecorator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Декоратор дерева: с заданной вероятностью размещает улей на боковой стороне ствола
 * и заселяет его пчёлами. Используется для дубов и берёз.
 */
public class BeehiveTreeDecorator extends TreeDecorator {

	public static final MapCodec<BeehiveTreeDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
	                                                                .fieldOf("probability")
	                                                                .xmap(
			                                                                BeehiveTreeDecorator::new,
			                                                                decorator -> decorator.probability
	                                                                );
	private static final Direction BEE_NEST_FACE = Direction.SOUTH;
	private static final Direction[] GENERATE_DIRECTIONS = Direction.Type.HORIZONTAL
			.stream()
			.filter(direction -> direction != BEE_NEST_FACE.getOpposite())
			.toArray(Direction[]::new);
	private final float probability;

	public BeehiveTreeDecorator(float probability) {
		this.probability = probability;
	}

	@Override
	protected TreeDecoratorType<?> getType() {
		return TreeDecoratorType.BEEHIVE;
	}

	@Override
	public void generate(TreeDecorator.Generator generator) {
		List<BlockPos> leavesPositions = generator.getLeavesPositions();
		List<BlockPos> logPositions = generator.getLogPositions();

		if (logPositions.isEmpty()) {
			return;
		}

		Random random = generator.getRandom();

		if (random.nextFloat() >= probability) {
			return;
		}

		int nestY = leavesPositions.isEmpty()
				? Math.min(logPositions.getFirst().getY() + 1 + random.nextInt(3), logPositions.getLast().getY())
				: Math.max(leavesPositions.getFirst().getY() - 1, logPositions.getFirst().getY() + 1);

		List<BlockPos> candidatePositions = logPositions.stream()
				.filter(pos -> pos.getY() == nestY)
				.flatMap(pos -> Stream.of(GENERATE_DIRECTIONS).map(pos::offset))
				.collect(Collectors.toList());

		if (candidatePositions.isEmpty()) {
			return;
		}

		Util.shuffle(candidatePositions, random);
		Optional<BlockPos> nestPos = candidatePositions.stream()
				.filter(pos -> generator.isAir(pos) && generator.isAir(pos.offset(BEE_NEST_FACE)))
				.findFirst();

		if (nestPos.isEmpty()) {
			return;
		}

		generator.replace(nestPos.get(), Blocks.BEE_NEST.getDefaultState().with(BeehiveBlock.FACING, BEE_NEST_FACE));
		generator.getWorld()
				.getBlockEntity(nestPos.get(), BlockEntityType.BEEHIVE)
				.ifPresent(blockEntity -> {
					int beeCount = 2 + random.nextInt(2);

					for (int bee = 0; bee < beeCount; bee++) {
						blockEntity.addBee(BeehiveBlockEntity.BeeData.create(random.nextInt(599)));
					}
				});
	}
}
