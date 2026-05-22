package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Supplier;

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

	/**
	 * Возвращает источник свойств двойного блока для данного сундука.
	 * Используется для объединения инвентарей соседних сундуков в двойной.
	 *
	 * @param state         текущее состояние блока
	 * @param world         мир, в котором находится блок
	 * @param pos           позиция блока
	 * @param ignoreBlocked {@code true} — игнорировать блокировку крышки сундука
	 * @return источник свойств двойного блока
	 */
	public abstract DoubleBlockProperties.PropertySource<? extends ChestBlockEntity> getBlockEntitySource(
		BlockState state, World world, BlockPos pos, boolean ignoreBlocked
	);
}
