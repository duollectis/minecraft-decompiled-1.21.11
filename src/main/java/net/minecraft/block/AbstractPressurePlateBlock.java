package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для всех типов нажимных плит.
 * Управляет логикой активации/деактивации редстоун-сигнала,
 * звуковыми эффектами и планированием тиков сброса.
 */
public abstract class AbstractPressurePlateBlock extends Block {

	private static final VoxelShape PRESSED_SHAPE = Block.createColumnShape(14.0, 0.0, 0.5);
	private static final VoxelShape DEFAULT_SHAPE = Block.createColumnShape(14.0, 0.0, 1.0);
	protected static final Box BOX = Block.createColumnShape(14.0, 0.0, 4.0).getBoundingBoxes().getFirst();

	/** Флаг обновления блока: уведомить соседей + отправить клиентам. */
	private static final int BLOCK_UPDATE_FLAGS = 2;

	protected final BlockSetType blockSetType;

	protected AbstractPressurePlateBlock(AbstractBlock.Settings settings, BlockSetType blockSetType) {
		super(settings.sounds(blockSetType.soundType()));
		this.blockSetType = blockSetType;
	}

	@Override
	protected abstract MapCodec<? extends AbstractPressurePlateBlock> getCodec();

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return getRedstoneOutput(state) > 0 ? PRESSED_SHAPE : DEFAULT_SHAPE;
	}

	protected int getTickRate() {
		return 20;
	}

	@Override
	public boolean canMobSpawnInside(BlockState state) {
		return true;
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
		return direction == Direction.DOWN && !state.canPlaceAt(world, pos)
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
		return hasTopRim(world, below) || sideCoversSmallSquare(world, below, Direction.UP);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		int currentOutput = getRedstoneOutput(state);

		if (currentOutput > 0) {
			updatePlateState(null, world, pos, state, currentOutput);
		}
	}

	@Override
	protected void onEntityCollision(
		BlockState state,
		World world,
		BlockPos pos,
		Entity entity,
		EntityCollisionHandler handler,
		boolean firstCollision
	) {
		if (world.isClient()) {
			return;
		}

		int currentOutput = getRedstoneOutput(state);

		if (currentOutput == 0) {
			updatePlateState(entity, world, pos, state, currentOutput);
		}
	}

	/**
	 * Обновляет состояние плиты: изменяет редстоун-выход, воспроизводит звук
	 * и планирует тик сброса при активации.
	 */
	private void updatePlateState(@Nullable Entity entity, World world, BlockPos pos, BlockState state, int prevOutput) {
		int newOutput = getRedstoneOutput(world, pos);
		boolean wasActive = prevOutput > 0;
		boolean isActive = newOutput > 0;

		if (prevOutput != newOutput) {
			BlockState newState = setRedstoneOutput(state, newOutput);
			world.setBlockState(pos, newState, BLOCK_UPDATE_FLAGS);
			updateNeighbors(world, pos);
			world.scheduleBlockRerenderIfNeeded(pos, state, newState);
		}

		if (!isActive && wasActive) {
			world.playSound(null, pos, blockSetType.pressurePlateClickOff(), SoundCategory.BLOCKS);
			world.emitGameEvent(entity, GameEvent.BLOCK_DEACTIVATE, pos);
		} else if (isActive && !wasActive) {
			world.playSound(null, pos, blockSetType.pressurePlateClickOn(), SoundCategory.BLOCKS);
			world.emitGameEvent(entity, GameEvent.BLOCK_ACTIVATE, pos);
		}

		if (isActive) {
			world.scheduleBlockTick(new BlockPos(pos), this, getTickRate());
		}
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		if (!moved && getRedstoneOutput(state) > 0) {
			updateNeighbors(world, pos);
		}
	}

	protected void updateNeighbors(World world, BlockPos pos) {
		world.updateNeighbors(pos, this);
		world.updateNeighbors(pos.down(), this);
	}

	@Override
	protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return getRedstoneOutput(state);
	}

	@Override
	protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return direction == Direction.UP ? getRedstoneOutput(state) : 0;
	}

	@Override
	protected boolean emitsRedstonePower(BlockState state) {
		return true;
	}

	protected static int getEntityCount(World world, Box box, Class<? extends Entity> entityClass) {
		return world
			.getEntitiesByClass(
				entityClass,
				box,
				EntityPredicates.EXCEPT_SPECTATOR.and(entity -> !entity.canAvoidTraps())
			)
			.size();
	}

	protected abstract int getRedstoneOutput(World world, BlockPos pos);

	protected abstract int getRedstoneOutput(BlockState state);

	protected abstract BlockState setRedstoneOutput(BlockState state, int output);
}
