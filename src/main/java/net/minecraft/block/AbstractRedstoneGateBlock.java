package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.OrientationHelper;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.tick.TickPriority;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для редстоун-вентилей (повторитель, компаратор).
 * Управляет логикой питания, задержкой переключения и обновлением соседей.
 */
public abstract class AbstractRedstoneGateBlock extends HorizontalFacingBlock {

	public static final BooleanProperty POWERED = Properties.POWERED;

	private static final VoxelShape SHAPE = Block.createColumnShape(16.0, 0.0, 2.0);

	protected AbstractRedstoneGateBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected abstract MapCodec<? extends AbstractRedstoneGateBlock> getCodec();

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos below = pos.down();
		return canPlaceAbove(world, below, world.getBlockState(below));
	}

	/**
	 * Проверяет, можно ли разместить вентиль над данным блоком.
	 * По умолчанию требует жёсткой твёрдой поверхности сверху.
	 */
	protected boolean canPlaceAbove(WorldView world, BlockPos pos, BlockState state) {
		return state.isSideSolid(world, pos, Direction.UP, SideShapeType.RIGID);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (isLocked(world, pos, state)) {
			return;
		}

		boolean powered = state.get(POWERED);
		boolean hasPower = hasPower(world, pos, state);

		if (powered && !hasPower) {
			world.setBlockState(pos, state.with(POWERED, false), 2);
		} else if (!powered) {
			world.setBlockState(pos, state.with(POWERED, true), 2);

			if (!hasPower) {
				world.scheduleBlockTick(pos, this, getUpdateDelayInternal(state), TickPriority.VERY_HIGH);
			}
		}
	}

	@Override
	protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return state.getWeakRedstonePower(world, pos, direction);
	}

	@Override
	protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		if (!state.get(POWERED)) {
			return 0;
		}

		return state.get(FACING) == direction ? getOutputLevel(world, pos, state) : 0;
	}

	@Override
	protected void neighborUpdate(
		BlockState state,
		World world,
		BlockPos pos,
		Block sourceBlock,
		@Nullable WireOrientation wireOrientation,
		boolean notify
	) {
		if (state.canPlaceAt(world, pos)) {
			updatePowered(world, pos, state);
			return;
		}

		BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
		dropStacks(state, world, pos, blockEntity);
		world.removeBlock(pos, false);

		for (Direction direction : Direction.values()) {
			world.updateNeighbors(pos.offset(direction), this);
		}
	}

	/**
	 * Планирует тик переключения состояния вентиля при изменении входного сигнала.
	 * Приоритет тика зависит от того, является ли цель «несогласованной» (misaligned).
	 */
	protected void updatePowered(World world, BlockPos pos, BlockState state) {
		if (isLocked(world, pos, state)) {
			return;
		}

		boolean powered = state.get(POWERED);
		boolean hasPower = hasPower(world, pos, state);

		if (powered == hasPower || world.getBlockTickScheduler().isTicking(pos, this)) {
			return;
		}

		TickPriority priority;

		if (isTargetNotAligned(world, pos, state)) {
			priority = TickPriority.EXTREMELY_HIGH;
		} else if (powered) {
			priority = TickPriority.VERY_HIGH;
		} else {
			priority = TickPriority.HIGH;
		}

		world.scheduleBlockTick(pos, this, getUpdateDelayInternal(state), priority);
	}

	public boolean isLocked(WorldView world, BlockPos pos, BlockState state) {
		return false;
	}

	protected boolean hasPower(World world, BlockPos pos, BlockState state) {
		return getPower(world, pos, state) > 0;
	}

	/**
	 * Возвращает уровень входного сигнала с учётом редстоун-провода.
	 * Если мощность уже максимальная (15), пропускает проверку провода.
	 */
	protected int getPower(World world, BlockPos pos, BlockState state) {
		Direction facing = state.get(FACING);
		BlockPos frontPos = pos.offset(facing);
		int power = world.getEmittedRedstonePower(frontPos, facing);

		if (power >= 15) {
			return power;
		}

		BlockState frontState = world.getBlockState(frontPos);
		return Math.max(power, frontState.isOf(Blocks.REDSTONE_WIRE) ? frontState.get(RedstoneWireBlock.POWER) : 0);
	}

	protected int getMaxInputLevelSides(RedstoneView world, BlockPos pos, BlockState state) {
		Direction facing = state.get(FACING);
		Direction right = facing.rotateYClockwise();
		Direction left = facing.rotateYCounterclockwise();
		boolean gatesOnly = getSideInputFromGatesOnly();

		return Math.max(
			world.getEmittedRedstonePower(pos.offset(right), right, gatesOnly),
			world.getEmittedRedstonePower(pos.offset(left), left, gatesOnly)
		);
	}

	@Override
	protected boolean emitsRedstonePower(BlockState state) {
		return true;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	public void onPlaced(
		World world,
		BlockPos pos,
		BlockState state,
		@Nullable LivingEntity placer,
		ItemStack itemStack
	) {
		if (hasPower(world, pos, state)) {
			world.scheduleBlockTick(pos, this, 1);
		}
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		updateTarget(world, pos, state);
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		if (!moved) {
			updateTarget(world, pos, state);
		}
	}

	protected void updateTarget(World world, BlockPos pos, BlockState state) {
		Direction facing = state.get(FACING);
		BlockPos targetPos = pos.offset(facing.getOpposite());
		WireOrientation orientation = OrientationHelper.getEmissionOrientation(world, facing.getOpposite(), Direction.UP);

		world.updateNeighbor(targetPos, this, orientation);
		world.updateNeighborsExcept(targetPos, this, facing, orientation);
	}

	protected boolean getSideInputFromGatesOnly() {
		return false;
	}

	protected int getOutputLevel(BlockView world, BlockPos pos, BlockState state) {
		return 15;
	}

	public static boolean isRedstoneGate(BlockState state) {
		return state.getBlock() instanceof AbstractRedstoneGateBlock;
	}

	public boolean isTargetNotAligned(BlockView world, BlockPos pos, BlockState state) {
		Direction opposite = state.get(FACING).getOpposite();
		BlockState targetState = world.getBlockState(pos.offset(opposite));
		return isRedstoneGate(targetState) && targetState.get(FACING) != opposite;
	}

	protected abstract int getUpdateDelayInternal(BlockState state);
}
