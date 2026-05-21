package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Supplier;

/**
 * {@code AbstractChestBlock}.
 */
public abstract class AbstractChestBlock<E extends BlockEntity> extends BlockWithEntity {

	protected final Supplier<BlockEntityType<? extends E>> entityTypeRetriever;

	protected AbstractChestBlock(
			AbstractBlock.Settings settings,
			Supplier<BlockEntityType<? extends E>> entityTypeRetriever
	) {
		super(settings);
		this.entityTypeRetriever = entityTypeRetriever;
	}

	@Override
	protected abstract MapCodec<? extends AbstractChestBlock<E>> getCodec();

	public abstract DoubleBlockProperties.PropertySource<? extends ChestBlockEntity> getBlockEntitySource(
			BlockState state, World world, BlockPos pos, boolean ignoreBlocked
	);
}
