package net.minecraft.structure.rule;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.random.Random;

/**
 * Реализация {@link RuleTest}, которая всегда возвращает {@code true}.
 * Используется как заглушка, когда замена блока должна происходить безусловно.
 */
public class AlwaysTrueRuleTest extends RuleTest {

	public static final AlwaysTrueRuleTest INSTANCE = new AlwaysTrueRuleTest();
	public static final MapCodec<AlwaysTrueRuleTest> CODEC = MapCodec.unit(() -> INSTANCE);

	private AlwaysTrueRuleTest() {
	}

	@Override
	public boolean test(BlockState state, Random random) {
		return true;
	}

	@Override
	protected RuleTestType<?> getType() {
		return RuleTestType.ALWAYS_TRUE;
	}
}
