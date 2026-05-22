package net.minecraft.block;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.Optional;

/**
 * Интерфейс деградации блока (например, окисление меди). Блок периодически проверяет
 * соседей в радиусе {@link #DEGRADING_RANGE} и деградирует с вероятностью, зависящей
 * от доли более окисленных соседей.
 */
public interface Degradable<T extends Enum<T>> {

	int DEGRADING_RANGE = 4;
	float BASE_DEGRADATION_CHANCE = 0.05688889F;

	Optional<BlockState> getDegradationResult(BlockState state);

	float getDegradationChanceMultiplier();

	T getDegradationLevel();

	default void tickDegradation(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (random.nextFloat() < BASE_DEGRADATION_CHANCE) {
			tryDegrade(state, world, pos, random).ifPresent(degraded -> world.setBlockState(pos, degraded));
		}
	}

	/**
	 * Пытается деградировать блок на основе статистики соседей того же типа деградации.
	 * Возвращает пустой Optional, если рядом есть менее окисленный сосед (блок не деградирует первым).
	 */
	default Optional<BlockState> tryDegrade(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		int currentLevel = getDegradationLevel().ordinal();
		int moreOxidizedCount = 0;
		int sameOxidizedCount = 0;

		for (BlockPos neighbor : BlockPos.iterateOutwards(pos, DEGRADING_RANGE, DEGRADING_RANGE, DEGRADING_RANGE)) {
			if (neighbor.getManhattanDistance(pos) > DEGRADING_RANGE) {
				break;
			}

			if (neighbor.equals(pos)) {
				continue;
			}

			Block neighborBlock = world.getBlockState(neighbor).getBlock();

			if (!(neighborBlock instanceof Degradable<?>)) {
				continue;
			}

			Enum<?> neighborLevel = ((Degradable<?>) neighborBlock).getDegradationLevel();

			if (getDegradationLevel().getClass() != neighborLevel.getClass()) {
				continue;
			}

			int neighborOrdinal = neighborLevel.ordinal();

			if (neighborOrdinal < currentLevel) {
				return Optional.empty();
			}

			if (neighborOrdinal > currentLevel) {
				moreOxidizedCount++;
			} else {
				sameOxidizedCount++;
			}
		}

		float ratio = (float) (moreOxidizedCount + 1) / (moreOxidizedCount + sameOxidizedCount + 1);
		float chance = ratio * ratio * getDegradationChanceMultiplier();

		return random.nextFloat() < chance ? getDegradationResult(state) : Optional.empty();
	}
}
