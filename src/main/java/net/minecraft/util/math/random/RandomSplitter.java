package net.minecraft.util.math.random;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Фабрика дочерних генераторов случайных чисел, порождаемых из одного родительского сида.
 * Позволяет детерминированно создавать независимые генераторы для разных контекстов
 * (позиция блока, строковый ключ, числовой сид) без взаимного влияния последовательностей.
 */
public interface RandomSplitter {

	default Random split(BlockPos pos) {
		return split(pos.getX(), pos.getY(), pos.getZ());
	}

	default Random split(Identifier seed) {
		return split(seed.toString());
	}

	Random split(String seed);

	Random split(long seed);

	Random split(int x, int y, int z);

	@VisibleForTesting
	void addDebugInfo(StringBuilder info);
}
