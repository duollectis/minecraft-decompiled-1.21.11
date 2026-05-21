package net.minecraft.block;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.listener.GameEventListener;
import org.jspecify.annotations.Nullable;

/**
 * {@code BlockEntityProvider}.
 */
public interface BlockEntityProvider {

	@Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state);

	default <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
			World world,
			BlockState state,
			BlockEntityType<T> type
	) {
		return null;
	}

	default <T extends BlockEntity> @Nullable GameEventListener getGameEventListener(ServerWorld world, T blockEntity) {
		return blockEntity instanceof GameEventListener.Holder<?> holder ? holder.getEventListener() : null;
	}
}
