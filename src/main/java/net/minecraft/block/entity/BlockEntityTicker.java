package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Функциональный интерфейс для тиков блок-сущностей.
 * <p>
 * Регистрируется через {@code Block#getTicker} и вызывается каждый игровой тик
 * на стороне сервера или клиента в зависимости от контекста.
 */
@FunctionalInterface
public interface BlockEntityTicker<T extends BlockEntity> {

	void tick(World world, BlockPos pos, BlockState state, T blockEntity);
}
