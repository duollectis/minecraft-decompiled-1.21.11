package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

import java.util.Optional;

/**
 * Базовый класс для тела многоблочных растений (водоросли, лоза и т.п.).
 * Реализует логику роста через {@link Fertilizable} и делегирует
 * фактический рост к головному стеблю {@link AbstractPlantStemBlock}.
 */
public abstract class AbstractPlantBlock extends AbstractPlantPartBlock implements Fertilizable {

	protected AbstractPlantBlock(
		AbstractBlock.Settings settings,
		Direction direction,
		VoxelShape voxelShape,
		boolean tickWater
	) {
		super(settings, direction, voxelShape, tickWater);
	}

	@Override
	protected abstract MapCodec<? extends AbstractPlantBlock> getCodec();

	/**
	 * Копирует свойства состояния при переходе от одного блока к другому.
	 * По умолчанию возвращает целевое состояние без изменений;
	 * переопределяется подклассами для переноса специфических свойств (например, AGE).
	 */
	protected BlockState copyState(BlockState from, BlockState to) {
		return to;
	}

	@Override
	protected BlockState getStateForNeighborUpdate(
		BlockState state,
		WorldView world,
		ScheduledTickView tickView,
		BlockPos pos,
		Direction direction,
		BlockPos neighborPos,
		BlockState neighborState,
		Random random
	) {
		if (direction == growthDirection.getOpposite() && !state.canPlaceAt(world, pos)) {
			tickView.scheduleBlockTick(pos, this, 1);
		}

		AbstractPlantStemBlock stem = getStem();

		if (direction == growthDirection && !neighborState.isOf(this) && !neighborState.isOf(stem)) {
			return copyState(state, stem.getRandomGrowthState(random));
		}

		if (tickWater) {
			tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}

		return super.getStateForNeighborUpdate(
			state,
			world,
			tickView,
			pos,
			direction,
			neighborPos,
			neighborState,
			random
		);
	}

	@Override
	protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(getStem());
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		Optional<BlockPos> headPos = getStemHeadPos(world, pos, state.getBlock());
		return headPos.isPresent()
			&& getStem().chooseStemState(world.getBlockState(headPos.get().offset(growthDirection)));
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		Optional<BlockPos> headPos = getStemHeadPos(world, pos, state.getBlock());

		if (headPos.isPresent()) {
			BlockState headState = world.getBlockState(headPos.get());
			((AbstractPlantStemBlock) headState.getBlock()).grow(world, random, headPos.get(), headState);
		}
	}

	private Optional<BlockPos> getStemHeadPos(BlockView world, BlockPos pos, Block block) {
		return BlockLocating.findColumnEnd(world, pos, block, growthDirection, getStem());
	}

	@Override
	protected boolean canReplace(BlockState state, ItemPlacementContext context) {
		boolean canReplace = super.canReplace(state, context);
		return canReplace && context.getStack().isOf(getStem().asItem()) ? false : canReplace;
	}

	@Override
	protected Block getPlant() {
		return this;
	}
}
