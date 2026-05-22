package net.minecraft.structure.processor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.rule.AlwaysTruePosRuleTest;
import net.minecraft.structure.rule.PosRuleTest;
import net.minecraft.structure.rule.RuleTest;
import net.minecraft.structure.rule.blockentity.PassthroughRuleBlockEntityModifier;
import net.minecraft.structure.rule.blockentity.RuleBlockEntityModifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

/**
 * Правило замены блока в процессоре структур.
 * Содержит три предиката: для входного блока шаблона, для блока в мире на той же позиции
 * и для позиции блока. Если все три предиката выполнены, блок заменяется на {@code outputState},
 * а его NBT-данные модифицируются через {@code blockEntityModifier}.
 */
public class StructureProcessorRule {

	public static final PassthroughRuleBlockEntityModifier DEFAULT_BLOCK_ENTITY_MODIFIER =
		PassthroughRuleBlockEntityModifier.INSTANCE;

	public static final Codec<StructureProcessorRule> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			RuleTest.TYPE_CODEC.fieldOf("input_predicate").forGetter(rule -> rule.inputPredicate),
			RuleTest.TYPE_CODEC.fieldOf("location_predicate").forGetter(rule -> rule.locationPredicate),
			PosRuleTest.BASE_CODEC
				.lenientOptionalFieldOf("position_predicate", AlwaysTruePosRuleTest.INSTANCE)
				.forGetter(rule -> rule.positionPredicate),
			BlockState.CODEC.fieldOf("output_state").forGetter(rule -> rule.outputState),
			RuleBlockEntityModifier.TYPE_CODEC
				.lenientOptionalFieldOf("block_entity_modifier", DEFAULT_BLOCK_ENTITY_MODIFIER)
				.forGetter(rule -> rule.blockEntityModifier)
		).apply(instance, StructureProcessorRule::new)
	);

	private final RuleTest inputPredicate;
	private final RuleTest locationPredicate;
	private final PosRuleTest positionPredicate;
	private final BlockState outputState;
	private final RuleBlockEntityModifier blockEntityModifier;

	public StructureProcessorRule(RuleTest inputPredicate, RuleTest locationPredicate, BlockState state) {
		this(inputPredicate, locationPredicate, AlwaysTruePosRuleTest.INSTANCE, state);
	}

	public StructureProcessorRule(
		RuleTest inputPredicate,
		RuleTest locationPredicate,
		PosRuleTest positionPredicate,
		BlockState state
	) {
		this(inputPredicate, locationPredicate, positionPredicate, state, DEFAULT_BLOCK_ENTITY_MODIFIER);
	}

	public StructureProcessorRule(
		RuleTest inputPredicate,
		RuleTest locationPredicate,
		PosRuleTest positionPredicate,
		BlockState outputState,
		RuleBlockEntityModifier blockEntityModifier
	) {
		this.inputPredicate = inputPredicate;
		this.locationPredicate = locationPredicate;
		this.positionPredicate = positionPredicate;
		this.outputState = outputState;
		this.blockEntityModifier = blockEntityModifier;
	}

	public boolean test(
		BlockState input,
		BlockState worldState,
		BlockPos originalPos,
		BlockPos currentPos,
		BlockPos pivot,
		Random random
	) {
		return inputPredicate.test(input, random)
			&& locationPredicate.test(worldState, random)
			&& positionPredicate.test(originalPos, currentPos, pivot, random);
	}

	public BlockState getOutputState() {
		return outputState;
	}

	public @Nullable NbtCompound getOutputNbt(Random random, @Nullable NbtCompound nbt) {
		return blockEntityModifier.modifyBlockEntityNbt(random, nbt);
	}
}
