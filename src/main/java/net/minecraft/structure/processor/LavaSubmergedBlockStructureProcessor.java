package net.minecraft.structure.processor;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

/**
 * Процессор структур, заменяющий блоки лавой, если они находятся в лаве.
 * Если в мире на позиции блока уже стоит лава, и форма блока шаблона не является
 * полным кубом (т.е. лава может «затопить» его), блок заменяется на лаву.
 * Используется при генерации структур в Нижнем мире.
 */
public class LavaSubmergedBlockStructureProcessor extends StructureProcessor {

	public static final MapCodec<LavaSubmergedBlockStructureProcessor> CODEC =
		MapCodec.unit(() -> LavaSubmergedBlockStructureProcessor.INSTANCE);

	public static final LavaSubmergedBlockStructureProcessor INSTANCE = new LavaSubmergedBlockStructureProcessor();

	@Override
	public StructureTemplate.@Nullable StructureBlockInfo process(
		WorldView world,
		BlockPos pos,
		BlockPos pivot,
		StructureTemplate.StructureBlockInfo originalBlockInfo,
		StructureTemplate.StructureBlockInfo currentBlockInfo,
		StructurePlacementData data
	) {
		BlockPos blockPos = currentBlockInfo.pos();
		boolean isSubmergedInLava = world.getBlockState(blockPos).isOf(Blocks.LAVA);
		boolean isNotFullCube = !Block.isShapeFullCube(currentBlockInfo.state().getOutlineShape(world, blockPos));

		return isSubmergedInLava && isNotFullCube
			? new StructureTemplate.StructureBlockInfo(blockPos, Blocks.LAVA.getDefaultState(), currentBlockInfo.nbt())
			: currentBlockInfo;
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return StructureProcessorType.LAVA_SUBMERGED_BLOCK;
	}
}
