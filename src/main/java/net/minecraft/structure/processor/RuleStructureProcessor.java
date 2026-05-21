package net.minecraft.structure.processor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * {@code RuleStructureProcessor}.
 */
public class RuleStructureProcessor extends StructureProcessor {

	public static final MapCodec<RuleStructureProcessor> CODEC = StructureProcessorRule.CODEC
			.listOf()
			.fieldOf("rules")
			.xmap(RuleStructureProcessor::new, processor -> processor.rules);
	private final ImmutableList<StructureProcessorRule> rules;

	public RuleStructureProcessor(List<? extends StructureProcessorRule> rules) {
		this.rules = ImmutableList.copyOf(rules);
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
		Random random = Random.create(MathHelper.hashCode(currentBlockInfo.pos()));
		BlockState blockState = world.getBlockState(currentBlockInfo.pos());
		UnmodifiableIterator var9 = this.rules.iterator();

		while (var9.hasNext()) {
			StructureProcessorRule structureProcessorRule = (StructureProcessorRule) var9.next();
			if (structureProcessorRule.test(
					currentBlockInfo.state(),
					blockState,
					originalBlockInfo.pos(),
					currentBlockInfo.pos(),
					pivot,
					random
			)) {
				return new StructureTemplate.StructureBlockInfo(
						currentBlockInfo.pos(),
						structureProcessorRule.getOutputState(),
						structureProcessorRule.getOutputNbt(random, currentBlockInfo.nbt())
				);
			}
		}

		return currentBlockInfo;
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return StructureProcessorType.RULE;
	}
}
