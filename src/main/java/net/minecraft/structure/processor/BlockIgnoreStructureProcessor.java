package net.minecraft.structure.processor;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Процессор структур, пропускающий (игнорирующий) блоки из заданного списка.
 * Если блок шаблона входит в список {@code blocks}, он не размещается в мире
 * (метод возвращает {@code null}).
 */
public class BlockIgnoreStructureProcessor extends StructureProcessor {

	public static final MapCodec<BlockIgnoreStructureProcessor> CODEC = BlockState.CODEC
		.xmap(AbstractBlock.AbstractBlockState::getBlock, Block::getDefaultState)
		.listOf()
		.fieldOf("blocks")
		.xmap(BlockIgnoreStructureProcessor::new, processor -> processor.blocks);

	public static final BlockIgnoreStructureProcessor IGNORE_STRUCTURE_BLOCKS =
		new BlockIgnoreStructureProcessor(ImmutableList.of(Blocks.STRUCTURE_BLOCK));

	public static final BlockIgnoreStructureProcessor IGNORE_AIR =
		new BlockIgnoreStructureProcessor(ImmutableList.of(Blocks.AIR));

	public static final BlockIgnoreStructureProcessor IGNORE_AIR_AND_STRUCTURE_BLOCKS =
		new BlockIgnoreStructureProcessor(ImmutableList.of(Blocks.AIR, Blocks.STRUCTURE_BLOCK));

	private final ImmutableList<Block> blocks;

	public BlockIgnoreStructureProcessor(List<Block> blocks) {
		this.blocks = ImmutableList.copyOf(blocks);
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
		return blocks.contains(currentBlockInfo.state().getBlock()) ? null : currentBlockInfo;
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return StructureProcessorType.BLOCK_IGNORE;
	}
}
