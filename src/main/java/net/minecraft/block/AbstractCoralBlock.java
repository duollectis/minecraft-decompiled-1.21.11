package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

public abstract class AbstractCoralBlock extends Block implements Waterloggable {

	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

	private static final VoxelShape SHAPE = Block.createColumnShape(12.0, 0.0, 4.0);

	/** Задержка перед превращением в мёртвый коралл при отсутствии воды (в тиках). */
	private static final int DEATH_DELAY_BASE = 60;
	/** Случайная добавка к задержке смерти (в тиках). */
	private static final int DEATH_DELAY_RANDOM = 40;
	/** Уровень жидкости, соответствующий полному блоку воды (source block). */
	private static final int FULL_WATER_LEVEL = 8;

	protected AbstractCoralBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(WATERLOGGED, true));
	}

	@Override
	protected abstract MapCodec<? extends AbstractCoralBlock> getCodec();

	/**
	 * Проверяет условия выживания коралла и планирует тик смерти,
	 * если блок не находится в воде.
	 */
	protected void checkLivingConditions(
		BlockState state,
		BlockView world,
		ScheduledTickView tickView,
		Random random,
		BlockPos pos
	) {
		if (isInWater(state, world, pos)) {
			return;
		}

		tickView.scheduleBlockTick(pos, this, DEATH_DELAY_BASE + random.nextInt(DEATH_DELAY_RANDOM));
	}

	/**
	 * Возвращает {@code true}, если коралл находится в воде:
	 * либо сам блок залогирован, либо хотя бы один сосед содержит воду.
	 */
	protected static boolean isInWater(BlockState state, BlockView world, BlockPos pos) {
		if (state.get(WATERLOGGED)) {
			return true;
		}

		for (Direction direction : Direction.values()) {
			if (world.getFluidState(pos.offset(direction)).isIn(FluidTags.WATER)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Определяет начальное состояние коралла при размещении: устанавливает
	 * {@link #WATERLOGGED} в {@code true} только если блок помещается в source-блок воды
	 * (уровень {@value #FULL_WATER_LEVEL}), а не в текущую воду или другую жидкость.
	 */
	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
		boolean isSubmerged = fluidState.isIn(FluidTags.WATER) && fluidState.getLevel() == FULL_WATER_LEVEL;
		return getDefaultState().with(WATERLOGGED, isSubmerged);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	/**
	 * Планирует тик воды при изменении соседей (если блок залогирован),
	 * а при потере твёрдой опоры снизу немедленно заменяет коралл воздухом.
	 * Двойная ответственность обусловлена тем, что коралл — одновременно
	 * {@link Waterloggable} и блок, требующий опоры.
	 */
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
		if (state.get(WATERLOGGED)) {
			tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}

		return direction == Direction.DOWN && !canPlaceAt(state, world, pos)
			? Blocks.AIR.getDefaultState()
			: super.getStateForNeighborUpdate(
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
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos below = pos.down();
		return world.getBlockState(below).isSideSolidFullSquare(world, below, Direction.UP);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}
}
