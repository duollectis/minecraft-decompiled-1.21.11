package net.minecraft.entity;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

/**
 * Стратегия проверки допустимости позиции спауна для сущности.
 * Используется в {@link SpawnRestriction} для определения, может ли моб заспауниться
 * в конкретной позиции мира.
 */
public interface SpawnLocation {

	boolean isSpawnPositionOk(WorldView world, BlockPos pos, @Nullable EntityType<?> entityType);

	default BlockPos adjustPosition(WorldView world, BlockPos pos) {
		return pos;
	}
}
