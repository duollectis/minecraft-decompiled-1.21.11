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
 * {@code BlockAgeStructureProcessor}.
 */
public class BlockAgeStructureProcessor extends StructureProcessor {

	public static final MapCodec<BlockAgeStructureProcessor> CODEC = Codec.FLOAT
			.fieldOf("mossiness")
			.xmap(BlockAgeStructureProcessor::new, processor -> processor.mossiness);
	private static final float BLOCK_REPLACE_THRESHOLD = 0.5F;
	private static final float STAIR_REPLACE_THRESHOLD = 0.5F;
	private static final float OBSIDIAN_REPLACE_THRESHOLD = 0.15F;
	private static final BlockState[]
			AGEABLE_SLABS =
			new BlockState[]{Blocks.STONE_SLAB.getDefaultState(), Blocks.STONE_BRICK_SLAB.getDefaultState()};
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
		BlockState blockState2 = null;
		if (blockState.isOf(Blocks.STONE_BRICKS) || blockState.isOf(Blocks.STONE)
				|| blockState.isOf(Blocks.CHISELED_STONE_BRICKS)) {
			blockState2 = this.processBlocks(random);
		}
		else if (blockState.isIn(BlockTags.STAIRS)) {
			blockState2 = this.processStairs(blockState, random);
		}
		else if (blockState.isIn(BlockTags.SLABS)) {
			blockState2 = this.processSlabs(blockState, random);
		}
		else if (blockState.isIn(BlockTags.WALLS)) {
			blockState2 = this.processWalls(blockState, random);
		}
		else if (blockState.isOf(Blocks.OBSIDIAN)) {
			blockState2 = this.processObsidian(random);
		}

		return blockState2 != null ? new StructureTemplate.StructureBlockInfo(
				blockPos,
				blockState2,
				currentBlockInfo.nbt()
		) : currentBlockInfo;
	}

	private @Nullable BlockState processBlocks(Random random) {
		if (random.nextFloat() >= 0.5F) {
			return null;
		}
		else {
			BlockState[]
					blockStates =
					new BlockState[]{
							Blocks.CRACKED_STONE_BRICKS.getDefaultState(),
							randomStairProperties(random, Blocks.STONE_BRICK_STAIRS)
					};
			BlockState[] blockStates2 = new BlockState[]{
					Blocks.MOSSY_STONE_BRICKS.getDefaultState(),
					randomStairProperties(random, Blocks.MOSSY_STONE_BRICK_STAIRS)
			};
			return this.process(random, blockStates, blockStates2);
		}
	}

	private @Nullable BlockState processStairs(BlockState blockState, Random random) {
		if (random.nextFloat() >= 0.5F) {
			return null;
		}
		else {
			BlockState[] blockStates = new BlockState[]{
					Blocks.MOSSY_STONE_BRICK_STAIRS.getStateWithProperties(blockState),
					Blocks.MOSSY_STONE_BRICK_SLAB.getDefaultState()
			};
			return this.process(random, AGEABLE_SLABS, blockStates);
		}
	}

	private @Nullable BlockState processSlabs(BlockState blockState, Random random) {
		return random.nextFloat() < this.mossiness ? Blocks.MOSSY_STONE_BRICK_SLAB.getStateWithProperties(blockState)
		                                           : null;
	}

	private @Nullable BlockState processWalls(BlockState blockState, Random random) {
		return random.nextFloat() < this.mossiness ? Blocks.MOSSY_STONE_BRICK_WALL.getStateWithProperties(blockState)
		                                           : null;
	}

	private @Nullable BlockState processObsidian(Random random) {
		return random.nextFloat() < 0.15F ? Blocks.CRYING_OBSIDIAN.getDefaultState() : null;
	}

	private static BlockState randomStairProperties(Random random, Block stairs) {
		return stairs.getDefaultState()
		             .with(StairsBlock.FACING, Direction.Type.HORIZONTAL.random(random))
		             .with(StairsBlock.HALF, Util.getRandom(BlockHalf.values(), random));
	}

	private BlockState process(Random random, BlockState[] regularStates, BlockState[] mossyStates) {
		return random.nextFloat() < this.mossiness ? randomState(random, mossyStates)
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
