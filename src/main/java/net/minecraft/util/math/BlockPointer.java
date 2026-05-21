package net.minecraft.util.math;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * {@code BlockPointer}.
 */
public record BlockPointer(ServerWorld world, BlockPos pos, BlockState state, DispenserBlockEntity blockEntity) {

	/**
	 * Center pos.
	 *
	 * @return Vec3d — результат операции
	 */
	public Vec3d centerPos() {
		return this.pos.toCenterPos();
	}
}
