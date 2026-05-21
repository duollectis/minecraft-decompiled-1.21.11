package net.minecraft.structure.processor;

import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * {@code StructureProcessor}.
 */
public abstract class StructureProcessor {

	public StructureTemplate.@Nullable StructureBlockInfo process(
			WorldView world,
			BlockPos pos,
			BlockPos pivot,
			StructureTemplate.StructureBlockInfo originalBlockInfo,
			StructureTemplate.StructureBlockInfo currentBlockInfo,
			StructurePlacementData data
	) {
		return currentBlockInfo;
	}

	protected abstract StructureProcessorType<?> getType();

	public List<StructureTemplate.StructureBlockInfo> reprocess(
			ServerWorldAccess world,
			BlockPos pos,
			BlockPos pivot,
			List<StructureTemplate.StructureBlockInfo> originalBlockInfos,
			List<StructureTemplate.StructureBlockInfo> currentBlockInfos,
			StructurePlacementData data
	) {
		return currentBlockInfos;
	}
}
