package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * {@code BlockWithEntity}.
 */
public abstract class BlockWithEntity extends Block implements BlockEntityProvider {

	protected BlockWithEntity(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected abstract MapCodec<? extends BlockWithEntity> getCodec();

	@Override
	protected boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data) {
		super.onSyncedBlockEvent(state, world, pos, type, data);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity != null && blockEntity.onSyncedBlockEvent(type, data);
	}

	@Override
	protected @Nullable NamedScreenHandlerFactory createScreenHandlerFactory(
			BlockState state,
			World world,
			BlockPos pos
	) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity instanceof NamedScreenHandlerFactory ? (NamedScreenHandlerFactory) blockEntity : null;
	}

	@SuppressWarnings("unchecked")
	protected static <E extends BlockEntity, A extends BlockEntity> @Nullable BlockEntityTicker<A> validateTicker(
			BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker
	) {
		return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
	}
}
