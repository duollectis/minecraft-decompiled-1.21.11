package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для частей многоблочных растений (стебель и тело).
 * Определяет направление роста, форму и условия размещения.
 */
public abstract class AbstractPlantPartBlock extends Block {

	protected final Direction growthDirection;
	protected final boolean tickWater;
	protected final VoxelShape outlineShape;

	protected AbstractPlantPartBlock(
		AbstractBlock.Settings settings,
		Direction growthDirection,
		VoxelShape outlineShape,
		boolean tickWater
	) {
		super(settings);
		this.growthDirection = growthDirection;
		this.outlineShape = outlineShape;
		this.tickWater = tickWater;
	}

	@Override
	protected abstract MapCodec<? extends AbstractPlantPartBlock> getCodec();

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState neighbor = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(growthDirection));
		return neighbor.isOf(getStem()) || neighbor.isOf(getPlant())
			? getPlant().getDefaultState()
			: getRandomGrowthState(ctx.getWorld().random);
	}

	public BlockState getRandomGrowthState(Random random) {
		return getDefaultState();
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos attachPos = pos.offset(growthDirection.getOpposite());
		BlockState attachState = world.getBlockState(attachPos);

		if (!canAttachTo(attachState)) {
			return false;
		}

		return attachState.isOf(getStem())
			|| attachState.isOf(getPlant())
			|| attachState.isSideSolidFullSquare(world, attachPos, growthDirection);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (!state.canPlaceAt(world, pos)) {
			world.breakBlock(pos, true);
		}
	}

	/**
	 * Проверяет, может ли данная часть растения прикрепиться к соседнему блоку.
	 * По умолчанию разрешено прикрепление к любому блоку.
	 */
	protected boolean canAttachTo(BlockState state) {
		return true;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return outlineShape;
	}

	protected abstract AbstractPlantStemBlock getStem();

	protected abstract Block getPlant();
}
