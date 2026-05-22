package net.minecraft.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

/**
 * Стандартные реализации {@link SpawnLocation} для различных типов поверхностей спауна.
 */
public interface SpawnLocationTypes {

	/** Спаун разрешён в любом месте без дополнительных проверок. */
	SpawnLocation UNRESTRICTED = (world, pos, entityType) -> true;

	/** Спаун разрешён только в воде, если блок сверху не является твёрдым. */
	SpawnLocation IN_WATER = (world, pos, entityType) -> {
		if (entityType == null || !world.getWorldBorder().contains(pos)) {
			return false;
		}

		BlockPos above = pos.up();
		return world.getFluidState(pos).isIn(FluidTags.WATER)
				&& !world.getBlockState(above).isSolidBlock(world, above);
	};

	/** Спаун разрешён только в лаве. */
	SpawnLocation IN_LAVA = (world, pos, entityType) -> entityType != null
			&& world.getWorldBorder().contains(pos)
			&& world.getFluidState(pos).isIn(FluidTags.LAVA);

	/** Спаун разрешён на земле: блок снизу должен допускать спаун, а два блока над позицией — быть свободны. */
	SpawnLocation ON_GROUND = new SpawnLocation() {
		@Override
		public boolean isSpawnPositionOk(WorldView world, BlockPos pos, @Nullable EntityType<?> entityType) {
			if (entityType == null || !world.getWorldBorder().contains(pos)) {
				return false;
			}

			BlockPos above = pos.up();
			BlockPos below = pos.down();
			BlockState belowState = world.getBlockState(below);

			return belowState.allowsSpawning(world, below, entityType)
					&& isClearForSpawn(world, pos, entityType)
					&& isClearForSpawn(world, above, entityType);
		}

		private boolean isClearForSpawn(WorldView world, BlockPos pos, EntityType<?> entityType) {
			BlockState state = world.getBlockState(pos);
			return SpawnHelper.isClearForSpawn(world, pos, state, state.getFluidState(), entityType);
		}

		@Override
		public BlockPos adjustPosition(WorldView world, BlockPos pos) {
			BlockPos below = pos.down();
			return world.getBlockState(below).canPathfindThrough(NavigationType.LAND) ? below : pos;
		}
	};
}
