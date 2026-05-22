package net.minecraft.structure.rule;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Реализация {@link PosRuleTest}, которая всегда возвращает {@code true}.
 * Используется как заглушка, когда позиционная проверка не требуется.
 */
public class AlwaysTruePosRuleTest extends PosRuleTest {

	public static final AlwaysTruePosRuleTest INSTANCE = new AlwaysTruePosRuleTest();
	public static final MapCodec<AlwaysTruePosRuleTest> CODEC = MapCodec.unit(() -> INSTANCE);

	private AlwaysTruePosRuleTest() {
	}

	@Override
	public boolean test(BlockPos originalPos, BlockPos currentPos, BlockPos pivot, Random random) {
		return true;
	}

	@Override
	protected PosRuleTestType<?> getType() {
		return PosRuleTestType.ALWAYS_TRUE;
	}
}
