package net.minecraft.world.gen.treedecorator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Blocks;
import net.minecraft.block.CreakingHeartBlock;
import net.minecraft.block.enums.CreakingHeartState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Декоратор дерева, размещающий блок Creaking Heart внутри ствола бледного дуба.
 * Ищет бревно, со всех шести сторон окружённое другими брёвнами, и с заданной вероятностью
 * заменяет его на активное сердце крикуна.
 */
public class CreakingHeartTreeDecorator extends TreeDecorator {

	public static final MapCodec<CreakingHeartTreeDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
	                                                                      .fieldOf("probability")
	                                                                      .xmap(
			                                                                      CreakingHeartTreeDecorator::new,
			                                                                      treeDecorator -> treeDecorator.probability
	                                                                      );
	private final float probability;

	public CreakingHeartTreeDecorator(float probability) {
		this.probability = probability;
	}

	@Override
	protected TreeDecoratorType<?> getType() {
		return TreeDecoratorType.CREAKING_HEART;
	}

	@Override
	public void generate(TreeDecorator.Generator generator) {
		Random random = generator.getRandom();
		List<BlockPos> logPositions = generator.getLogPositions();

		if (logPositions.isEmpty() || random.nextFloat() >= probability) {
			return;
		}

		List<BlockPos> shuffledLogs = new ArrayList<>(logPositions);
		Util.shuffle(shuffledLogs, random);

		Optional<BlockPos> heartPos = shuffledLogs.stream()
				.filter(pos -> {
					for (Direction direction : Direction.values()) {
						if (!generator.matches(pos.offset(direction), state -> state.isIn(BlockTags.LOGS))) {
							return false;
						}
					}

					return true;
				})
				.findFirst();

		heartPos.ifPresent(pos -> generator.replace(
				pos,
				Blocks.CREAKING_HEART
						.getDefaultState()
						.with(CreakingHeartBlock.ACTIVE, CreakingHeartState.DORMANT)
						.with(CreakingHeartBlock.NATURAL, true)
		));
	}
}
