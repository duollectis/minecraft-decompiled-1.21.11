package net.minecraft.world.gen.treedecorator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Blocks;
import net.minecraft.block.CocoaBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Декоратор дерева: с заданной вероятностью размещает стручки какао на нижних брёвнах ствола.
 * Используется для джунглевых деревьев.
 */
public class CocoaTreeDecorator extends TreeDecorator {

	public static final MapCodec<CocoaTreeDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
	                                                              .fieldOf("probability")
	                                                              .xmap(
			                                                              CocoaTreeDecorator::new,
			                                                              decorator -> decorator.probability
	                                                              );
	private final float probability;

	public CocoaTreeDecorator(float probability) {
		this.probability = probability;
	}

	@Override
	protected TreeDecoratorType<?> getType() {
		return TreeDecoratorType.COCOA;
	}

	private static final float COCOA_PLACEMENT_CHANCE = 0.25F;
	private static final int COCOA_MAX_HEIGHT_OFFSET = 2;

	@Override
	public void generate(TreeDecorator.Generator generator) {
		Random random = generator.getRandom();

		if (random.nextFloat() >= probability) {
			return;
		}

		List<BlockPos> logPositions = generator.getLogPositions();

		if (logPositions.isEmpty()) {
			return;
		}

		int baseY = logPositions.getFirst().getY();
		logPositions.stream().filter(pos -> pos.getY() - baseY <= COCOA_MAX_HEIGHT_OFFSET).forEach(pos -> {
			for (Direction direction : Direction.Type.HORIZONTAL) {
				if (random.nextFloat() > COCOA_PLACEMENT_CHANCE) {
					continue;
				}

				Direction attachFace = direction.getOpposite();
				BlockPos cocoaPos = pos.add(attachFace.getOffsetX(), 0, attachFace.getOffsetZ());

				if (generator.isAir(cocoaPos)) {
					generator.replace(
							cocoaPos,
							Blocks.COCOA
									.getDefaultState()
									.with(CocoaBlock.AGE, random.nextInt(3))
									.with(CocoaBlock.FACING, direction)
					);
				}
			}
		});
	}
}
