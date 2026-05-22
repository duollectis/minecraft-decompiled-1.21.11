package net.minecraft.structure.processor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

/**
 * Процессор структур, имитирующий «состаривание» каменных блоков.
 * Заменяет каменные кирпичи, ступени, плиты, стены и обсидиан на замшелые,
 * потрескавшиеся или другие «состаренные» варианты с вероятностью, зависящей от {@code mossiness}.
 */
public class BlockAgeStructureProcessor extends StructureProcessor {

	public static final MapCodec<BlockAgeStructureProcessor> CODEC = Codec.FLOAT
		.fieldOf("mossiness")
		.xmap(BlockAgeStructureProcessor::new, processor -> processor.mossiness);

	private static final float BLOCK_REPLACE_THRESHOLD = 0.5F;
	private static final float OBSIDIAN_REPLACE_THRESHOLD = 0.15F;

	private static final BlockState[] AGEABLE_SLABS = {
		Blocks.STONE_SLAB.getDefaultState(),
		Blocks.STONE_BRICK_SLAB.getDefaultState()
	};

	private final float mossiness;

	public BlockAgeStructureProcessor(float mossiness) {
		this.mossiness = mossiness;
	}

	@Override
	public StructureTemplate.@Nullable StructureBlockInfo process(
		WorldView world,
		BlockPos pos,
		BlockPos pivot,
		StructureTemplate.StructureBlockInfo originalBlockInfo,
		StructureTemplate.StructureBlockInfo currentBlockInfo,
		StructurePlacementData data
	) {
		Random random = data.getRandom(currentBlockInfo.pos());
		BlockState blockState = currentBlockInfo.state();
		BlockPos blockPos = currentBlockInfo.pos();

		BlockState aged = computeAgedState(blockState, random);

		return aged != null
			? new StructureTemplate.StructureBlockInfo(blockPos, aged, currentBlockInfo.nbt())
			: currentBlockInfo;
	}

	private @Nullable BlockState computeAgedState(BlockState blockState, Random random) {
		if (blockState.isOf(Blocks.STONE_BRICKS)
			|| blockState.isOf(Blocks.STONE)
			|| blockState.isOf(Blocks.CHISELED_STONE_BRICKS)
		) {
			return processBlocks(random);
		}

		if (blockState.isIn(BlockTags.STAIRS)) {
			return processStairs(blockState, random);
		}

		if (blockState.isIn(BlockTags.SLABS)) {
			return processSlabs(blockState, random);
		}

		if (blockState.isIn(BlockTags.WALLS)) {
			return processWalls(blockState, random);
		}

		if (blockState.isOf(Blocks.OBSIDIAN)) {
			return processObsidian(random);
		}

		return null;
	}

	private @Nullable BlockState processBlocks(Random random) {
		if (random.nextFloat() >= BLOCK_REPLACE_THRESHOLD) {
			return null;
		}

		BlockState[] regularStates = {
			Blocks.CRACKED_STONE_BRICKS.getDefaultState(),
			randomStairProperties(random, Blocks.STONE_BRICK_STAIRS)
		};
		BlockState[] mossyStates = {
			Blocks.MOSSY_STONE_BRICKS.getDefaultState(),
			randomStairProperties(random, Blocks.MOSSY_STONE_BRICK_STAIRS)
		};

		return selectByMossiness(random, regularStates, mossyStates);
	}

	private @Nullable BlockState processStairs(BlockState blockState, Random random) {
		if (random.nextFloat() >= BLOCK_REPLACE_THRESHOLD) {
			return null;
		}

		BlockState[] mossyStates = {
			Blocks.MOSSY_STONE_BRICK_STAIRS.getStateWithProperties(blockState),
			Blocks.MOSSY_STONE_BRICK_SLAB.getDefaultState()
		};

		return selectByMossiness(random, AGEABLE_SLABS, mossyStates);
	}

	private @Nullable BlockState processSlabs(BlockState blockState, Random random) {
		return random.nextFloat() < mossiness
			? Blocks.MOSSY_STONE_BRICK_SLAB.getStateWithProperties(blockState)
			: null;
	}

	private @Nullable BlockState processWalls(BlockState blockState, Random random) {
		return random.nextFloat() < mossiness
			? Blocks.MOSSY_STONE_BRICK_WALL.getStateWithProperties(blockState)
			: null;
	}

	private @Nullable BlockState processObsidian(Random random) {
		return random.nextFloat() < OBSIDIAN_REPLACE_THRESHOLD
			? Blocks.CRYING_OBSIDIAN.getDefaultState()
			: null;
	}

	private static BlockState randomStairProperties(Random random, Block stairs) {
		return stairs.getDefaultState()
			.with(StairsBlock.FACING, Direction.Type.HORIZONTAL.random(random))
			.with(StairsBlock.HALF, Util.getRandom(BlockHalf.values(), random));
	}

	private BlockState selectByMossiness(Random random, BlockState[] regularStates, BlockState[] mossyStates) {
		return random.nextFloat() < mossiness
			? randomState(random, mossyStates)
			: randomState(random, regularStates);
	}

	private static BlockState randomState(Random random, BlockState[] states) {
		return states[random.nextInt(states.length)];
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return StructureProcessorType.BLOCK_AGE;
	}
}
