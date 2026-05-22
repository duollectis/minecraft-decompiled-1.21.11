package net.minecraft.structure.rule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.random.Random;

/**
 * Реализация {@link RuleTest}, проверяющая тип блока с вероятностной составляющей.
 * Блок проходит тест только если совпадает его тип И случайное число меньше заданной вероятности.
 */
public class RandomBlockMatchRuleTest extends RuleTest {

	public static final MapCodec<RandomBlockMatchRuleTest> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Registries.BLOCK.getCodec().fieldOf("block").forGetter(ruleTest -> ruleTest.block),
			Codec.FLOAT.fieldOf("probability").forGetter(ruleTest -> ruleTest.probability)
		)
		.apply(instance, RandomBlockMatchRuleTest::new)
	);

	private final Block block;
	private final float probability;

	public RandomBlockMatchRuleTest(Block block, float probability) {
		this.block = block;
		this.probability = probability;
	}

	@Override
	public boolean test(BlockState state, Random random) {
		return state.isOf(block) && random.nextFloat() < probability;
	}

	@Override
	protected RuleTestType<?> getType() {
		return RuleTestType.RANDOM_BLOCK_MATCH;
	}
}
