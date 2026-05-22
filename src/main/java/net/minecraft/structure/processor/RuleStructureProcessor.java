package net.minecraft.structure.processor;

import com.google.common.collect.ImmutableList;
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
 * Процессор структур, применяющий набор правил замены блоков.
 * Для каждого блока шаблона последовательно проверяются все правила {@link StructureProcessorRule};
 * первое совпавшее правило определяет итоговый блок и его NBT-данные.
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
		BlockState worldState = world.getBlockState(currentBlockInfo.pos());

		for (StructureProcessorRule rule : rules) {
			if (rule.test(currentBlockInfo.state(), worldState, originalBlockInfo.pos(), currentBlockInfo.pos(), pivot, random)) {
				return new StructureTemplate.StructureBlockInfo(
					currentBlockInfo.pos(),
					rule.getOutputState(),
					rule.getOutputNbt(random, currentBlockInfo.nbt())
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
