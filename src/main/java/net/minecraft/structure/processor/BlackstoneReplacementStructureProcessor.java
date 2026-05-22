package net.minecraft.structure.processor;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

import java.util.Map;

/**
 * Процессор структур, заменяющий каменные блоки на их блэкстоун-аналоги.
 * Используется при генерации бастионов в Нижнем мире, где каменные материалы
 * заменяются на полированный чёрный камень и его производные.
 * Является синглтоном — используется через {@link #INSTANCE}.
 */
public class BlackstoneReplacementStructureProcessor extends StructureProcessor {

	public static final MapCodec<BlackstoneReplacementStructureProcessor> CODEC =
		MapCodec.unit(() -> BlackstoneReplacementStructureProcessor.INSTANCE);

	public static final BlackstoneReplacementStructureProcessor INSTANCE =
		new BlackstoneReplacementStructureProcessor();

	private final Map<Block, Block> replacementMap = Util.make(Maps.newHashMap(), replacements -> {
		replacements.put(Blocks.COBBLESTONE, Blocks.BLACKSTONE);
		replacements.put(Blocks.MOSSY_COBBLESTONE, Blocks.BLACKSTONE);
		replacements.put(Blocks.STONE, Blocks.POLISHED_BLACKSTONE);
		replacements.put(Blocks.STONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS);
		replacements.put(Blocks.MOSSY_STONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS);
		replacements.put(Blocks.COBBLESTONE_STAIRS, Blocks.BLACKSTONE_STAIRS);
		replacements.put(Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.BLACKSTONE_STAIRS);
		replacements.put(Blocks.STONE_STAIRS, Blocks.POLISHED_BLACKSTONE_STAIRS);
		replacements.put(Blocks.STONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
		replacements.put(Blocks.MOSSY_STONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
		replacements.put(Blocks.COBBLESTONE_SLAB, Blocks.BLACKSTONE_SLAB);
		replacements.put(Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.BLACKSTONE_SLAB);
		replacements.put(Blocks.SMOOTH_STONE_SLAB, Blocks.POLISHED_BLACKSTONE_SLAB);
		replacements.put(Blocks.STONE_SLAB, Blocks.POLISHED_BLACKSTONE_SLAB);
		replacements.put(Blocks.STONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
		replacements.put(Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
		replacements.put(Blocks.STONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
		replacements.put(Blocks.MOSSY_STONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
		replacements.put(Blocks.COBBLESTONE_WALL, Blocks.BLACKSTONE_WALL);
		replacements.put(Blocks.MOSSY_COBBLESTONE_WALL, Blocks.BLACKSTONE_WALL);
		replacements.put(Blocks.CHISELED_STONE_BRICKS, Blocks.CHISELED_POLISHED_BLACKSTONE);
		replacements.put(Blocks.CRACKED_STONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
		replacements.put(Blocks.IRON_BARS, Blocks.IRON_CHAIN);
	});

	private BlackstoneReplacementStructureProcessor() {
	}

	@Override
	public StructureTemplate.StructureBlockInfo process(
		WorldView world,
		BlockPos pos,
		BlockPos pivot,
		StructureTemplate.StructureBlockInfo originalBlockInfo,
		StructureTemplate.StructureBlockInfo currentBlockInfo,
		StructurePlacementData data
	) {
		Block replacement = replacementMap.get(currentBlockInfo.state().getBlock());

		if (replacement == null) {
			return currentBlockInfo;
		}

		BlockState sourceState = currentBlockInfo.state();
		BlockState targetState = replacement.getDefaultState();

		if (sourceState.contains(StairsBlock.FACING)) {
			targetState = targetState.with(StairsBlock.FACING, sourceState.get(StairsBlock.FACING));
		}

		if (sourceState.contains(StairsBlock.HALF)) {
			targetState = targetState.with(StairsBlock.HALF, sourceState.get(StairsBlock.HALF));
		}

		if (sourceState.contains(SlabBlock.TYPE)) {
			targetState = targetState.with(SlabBlock.TYPE, sourceState.get(SlabBlock.TYPE));
		}

		return new StructureTemplate.StructureBlockInfo(
			currentBlockInfo.pos(),
			targetState,
			currentBlockInfo.nbt()
		);
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return StructureProcessorType.BLACKSTONE_REPLACE;
	}
}
