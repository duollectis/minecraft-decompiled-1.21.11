package net.minecraft.structure.rule;

import com.mojang.serialization.Codec;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Базовый абстрактный класс для позиционных тестов в правилах процессора структур.
 * Реализации определяют, подходит ли позиция блока для замены на основе её координат
 * относительно опорной точки (pivot) структуры.
 */
public abstract class PosRuleTest {

	public static final Codec<PosRuleTest> BASE_CODEC = Registries.POS_RULE_TEST
		.getCodec()
		.dispatch("predicate_type", PosRuleTest::getType, PosRuleTestType::codec);

	/**
	 * Проверяет, проходит ли позиция блока данный тест.
	 *
	 * @param originalPos исходная позиция блока в шаблоне
	 * @param currentPos  текущая (трансформированная) позиция блока в мире
	 * @param pivot       опорная точка структуры, относительно которой считается расстояние
	 * @param random      генератор случайных чисел
	 * @return {@code true}, если позиция проходит тест
	 */
	public abstract boolean test(BlockPos originalPos, BlockPos currentPos, BlockPos pivot, Random random);

	protected abstract PosRuleTestType<?> getType();
}
