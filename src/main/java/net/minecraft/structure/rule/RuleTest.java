package net.minecraft.structure.rule;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.random.Random;

/**
 * {@code RuleTest}.
 */
public abstract class RuleTest {

	public static final Codec<RuleTest>
			TYPE_CODEC =
			Registries.RULE_TEST.getCodec().dispatch("predicate_type", RuleTest::getType, RuleTestType::codec);

	public abstract boolean test(BlockState state, Random random);

	protected abstract RuleTestType<?> getType();
}
