package net.minecraft.util.math;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Указатель на блок диспенсера в мире: содержит мир, позицию, состояние блока
 * и ссылку на блок-сущность. Используется в логике диспенсеров при выбросе предметов.
 */
public record BlockPointer(ServerWorld world, BlockPos pos, BlockState state, DispenserBlockEntity blockEntity) {

	public Vec3d centerPos() {
		return pos.toCenterPos();
	}
}
