package net.minecraft.structure.rule;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.random.Random;

/**
 * Реализация {@link RuleTest}, проверяющая принадлежность блока конкретному типу {@link Block}.
 * Игнорирует состояние блока — проверяется только его тип.
 */
public class BlockMatchRuleTest extends RuleTest {

	public static final MapCodec<BlockMatchRuleTest> CODEC = Registries.BLOCK
		.getCodec()
		.fieldOf("block")
		.xmap(BlockMatchRuleTest::new, ruleTest -> ruleTest.block);

	private final Block block;

	public BlockMatchRuleTest(Block block) {
		this.block = block;
	}

	@Override
	public boolean test(BlockState state, Random random) {
		return state.isOf(block);
	}

	@Override
	protected RuleTestType<?> getType() {
		return RuleTestType.BLOCK_MATCH;
	}
}
