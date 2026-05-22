package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ComparatorBlockEntity;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import net.minecraft.world.tick.TickPriority;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Блок компаратора — измеряет и сравнивает сигналы редстоуна.
 * В режиме COMPARE выдаёт входной сигнал, если он не меньше бокового.
 * В режиме SUBTRACT вычитает боковой сигнал из входного.
 * Также считывает уровень заполнения контейнеров и рамок для предметов.
 */
public class ComparatorBlock extends AbstractRedstoneGateBlock implements BlockEntityProvider {

	public static final MapCodec<ComparatorBlock> CODEC = createCodec(ComparatorBlock::new);
	public static final EnumProperty<ComparatorMode> MODE = Properties.COMPARATOR_MODE;

	private static final int TICK_DELAY = 2;
	private static final int MAX_SIGNAL = 15;

	@Override
	public MapCodec<ComparatorBlock> getCodec() {
		return CODEC;
	}

	public ComparatorBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(
				stateManager
						.getDefaultState()
						.with(FACING, Direction.NORTH)
						.with(POWERED, false)
						.with(MODE, ComparatorMode.COMPARE)
		);
	}

	@Override
	protected int getUpdateDelayInternal(BlockState state) {
		return TICK_DELAY;
	}

	@Override
	public BlockState getStateForNeighborUpdate(
			BlockState state,
			WorldView world,
			ScheduledTickView tickView,
			BlockPos pos,
			Direction direction,
			BlockPos neighborPos,
			BlockState neighborState,
			Random random
	) {
		return direction == Direction.DOWN && !canPlaceAbove(world, neighborPos, neighborState)
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
	protected int getOutputLevel(BlockView world, BlockPos pos, BlockState state) {
		return world.getBlockEntity(pos) instanceof ComparatorBlockEntity comparator
				? comparator.getOutputSignal()
				: 0;
	}

	/**
	 * Вычисляет итоговый выходной сигнал с учётом режима (COMPARE / SUBTRACT) и боковых входов.
	 * В режиме SUBTRACT: результат = входной − максимальный боковой (минимум 0).
	 */
	private int calculateOutputSignal(World world, BlockPos pos, BlockState state) {
		int input = getPower(world, pos, state);

		if (input == 0) {
			return 0;
		}

		int sideMax = getMaxInputLevelSides(world, pos, state);

		if (sideMax > input) {
			return 0;
		}

		return state.get(MODE) == ComparatorMode.SUBTRACT ? input - sideMax : input;
	}

	@Override
	protected boolean hasPower(World world, BlockPos pos, BlockState state) {
		int input = getPower(world, pos, state);

		if (input == 0) {
			return false;
		}

		int sideMax = getMaxInputLevelSides(world, pos, state);

		return input > sideMax || (input == sideMax && state.get(MODE) == ComparatorMode.COMPARE);
	}

	/**
	 * Считывает мощность входного сигнала спереди.
	 * Если блок спереди — сплошной, проверяет также рамку для предметов и блок за ним.
	 */
	@Override
	protected int getPower(World world, BlockPos pos, BlockState state) {
		int power = super.getPower(world, pos, state);
		Direction facing = state.get(FACING);
		BlockPos frontPos = pos.offset(facing);
		BlockState frontState = world.getBlockState(frontPos);

		if (frontState.hasComparatorOutput()) {
			return frontState.getComparatorOutput(world, frontPos, facing.getOpposite());
		}

		if (power < MAX_SIGNAL && frontState.isSolidBlock(world, frontPos)) {
			BlockPos behindPos = frontPos.offset(facing);
			BlockState behindState = world.getBlockState(behindPos);
			ItemFrameEntity itemFrame = getAttachedItemFrame(world, facing, behindPos);

			int behindPower = Math.max(
					itemFrame == null ? Integer.MIN_VALUE : itemFrame.getComparatorPower(),
					behindState.hasComparatorOutput()
							? behindState.getComparatorOutput(world, behindPos, facing.getOpposite())
							: Integer.MIN_VALUE
			);

			if (behindPower != Integer.MIN_VALUE) {
				return behindPower;
			}
		}

		return power;
	}

	private @Nullable ItemFrameEntity getAttachedItemFrame(World world, Direction facing, BlockPos pos) {
		List<ItemFrameEntity> frames = world.getEntitiesByClass(
				ItemFrameEntity.class,
				new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
				frame -> frame.getHorizontalFacing() == facing
		);

		return frames.size() == 1 ? frames.get(0) : null;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!player.getAbilities().allowModifyWorld) {
			return ActionResult.PASS;
		}

		state = state.cycle(MODE);
		float pitch = state.get(MODE) == ComparatorMode.SUBTRACT ? 0.55F : 0.5F;
		world.playSound(player, pos, SoundEvents.BLOCK_COMPARATOR_CLICK, SoundCategory.BLOCKS, 0.3F, pitch);
		world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
		update(world, pos, state);

		return ActionResult.SUCCESS;
	}

	@Override
	protected void updatePowered(World world, BlockPos pos, BlockState state) {
		if (world.getBlockTickScheduler().isTicking(pos, this)) {
			return;
		}

		int newSignal = calculateOutputSignal(world, pos, state);
		int currentSignal = world.getBlockEntity(pos) instanceof ComparatorBlockEntity comparator
				? comparator.getOutputSignal()
				: 0;

		if (newSignal != currentSignal || state.get(POWERED) != hasPower(world, pos, state)) {
			TickPriority priority = isTargetNotAligned(world, pos, state)
					? TickPriority.HIGH
					: TickPriority.NORMAL;

			world.scheduleBlockTick(pos, this, TICK_DELAY, priority);
		}
	}

	private void update(World world, BlockPos pos, BlockState state) {
		int newSignal = calculateOutputSignal(world, pos, state);
		int prevSignal = 0;

		if (world.getBlockEntity(pos) instanceof ComparatorBlockEntity comparator) {
			prevSignal = comparator.getOutputSignal();
			comparator.setOutputSignal(newSignal);
		}

		if (prevSignal == newSignal && state.get(MODE) != ComparatorMode.COMPARE) {
			return;
		}

		boolean powered = hasPower(world, pos, state);
		boolean wasPowered = state.get(POWERED);

		if (wasPowered && !powered) {
			world.setBlockState(pos, state.with(POWERED, false), Block.NOTIFY_LISTENERS);
		} else if (!wasPowered && powered) {
			world.setBlockState(pos, state.with(POWERED, true), Block.NOTIFY_LISTENERS);
		}

		updateTarget(world, pos, state);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		this.update(world, pos, state);
	}

	@Override
	protected boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data) {
		super.onSyncedBlockEvent(state, world, pos, type, data);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity != null && blockEntity.onSyncedBlockEvent(type, data);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new ComparatorBlockEntity(pos, state);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, MODE, POWERED);
	}
}
