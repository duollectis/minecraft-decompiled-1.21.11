package net.minecraft.structure.processor;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.feature.Feature;
import org.jspecify.annotations.Nullable;

/**
 * Процессор структур, защищающий блоки из заданного тега от замены.
 * Если блок в мире на позиции шаблона входит в тег {@code protectedBlocksTag},
 * блок шаблона не размещается (возвращается {@code null}).
 * Используется для предотвращения перезаписи важных блоков (например, {@code features_cannot_replace}).
 */
public class ProtectedBlocksStructureProcessor extends StructureProcessor {

	public static final MapCodec<ProtectedBlocksStructureProcessor> CODEC = TagKey.codec(RegistryKeys.BLOCK)
		.xmap(
			ProtectedBlocksStructureProcessor::new,
			processor -> processor.protectedBlocksTag
		)
		.fieldOf("value");

	public final TagKey<Block> protectedBlocksTag;

	public ProtectedBlocksStructureProcessor(TagKey<Block> protectedBlocksTag) {
		this.protectedBlocksTag = protectedBlocksTag;
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
		return Feature.notInBlockTagPredicate(protectedBlocksTag).test(world.getBlockState(currentBlockInfo.pos()))
			? currentBlockInfo
			: null;
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return StructureProcessorType.PROTECTED_BLOCKS;
	}
}
