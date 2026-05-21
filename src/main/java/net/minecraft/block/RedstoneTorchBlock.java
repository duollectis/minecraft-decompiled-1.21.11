package net.minecraft.block;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.block.OrientationHelper;
import net.minecraft.world.block.WireOrientation;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * {@code RedstoneTorchBlock}.
 */
public class RedstoneTorchBlock extends AbstractTorchBlock {

	public static final MapCodec<RedstoneTorchBlock> CODEC = createCodec(RedstoneTorchBlock::new);
	public static final BooleanProperty LIT = Properties.LIT;
	private static final Map<BlockView, List<RedstoneTorchBlock.BurnoutEntry>> BURNOUT_MAP = new WeakHashMap<>();
	public static final int BURNOUT_WINDOW_TICKS = 60;
	public static final int MAX_BURNOUT_COUNT = 8;
	public static final int BURNOUT_RELIGHT_DELAY = 160;
	private static final int SCHEDULED_TICK_DELAY = 2;

	@Override
	public MapCodec<? extends RedstoneTorchBlock> getCodec() {
		return CODEC;
	}

	public RedstoneTorchBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState().with(LIT, true));
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		this.update(world, pos, state);
	}

	private void update(World world, BlockPos pos, BlockState state) {
		WireOrientation wireOrientation = this.getEmissionOrientation(world, state);

		for (Direction direction : Direction.values()) {
			world.updateNeighborsAlways(
					pos.offset(direction),
					this,
					OrientationHelper.withFrontNullable(wireOrientation, direction)
			);
		}
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		if (!moved) {
			this.update(world, pos, state);
		}
	}

	@Override
	protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return state.get(LIT) && Direction.UP != direction ? 15 : 0;
	}

	/**
	 * Определяет, следует ли unpower.
	 *
	 * @param world world
	 * @param pos pos
	 * @param state state
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldUnpower(World world, BlockPos pos, BlockState state) {
		return world.isEmittingRedstonePower(pos.down(), Direction.DOWN);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		boolean bl = this.shouldUnpower(world, pos, state);
		List<RedstoneTorchBlock.BurnoutEntry> list = BURNOUT_MAP.get(world);

		while (list != null && !list.isEmpty() && world.getTime() - list.get(0).time > 60L) {
			list.remove(0);
		}

		if (state.get(LIT)) {
			if (bl) {
				world.setBlockState(pos, state.with(LIT, false), 3);
				if (isBurnedOut(world, pos, true)) {
					world.syncWorldEvent(1502, pos, 0);
					world.scheduleBlockTick(pos, world.getBlockState(pos).getBlock(), 160);
				}
			}
		}
		else if (!bl && !isBurnedOut(world, pos, false)) {
			world.setBlockState(pos, state.with(LIT, true), 3);
		}
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
		if (state.get(LIT) == this.shouldUnpower(world, pos, state) && !world
				.getBlockTickScheduler()
				.isTicking(pos, this)) {
			world.scheduleBlockTick(pos, this, 2);
		}
	}

	@Override
	protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return direction == Direction.DOWN ? state.getWeakRedstonePower(world, pos, direction) : 0;
	}

	@Override
	protected boolean emitsRedstonePower(BlockState state) {
		return true;
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (state.get(LIT)) {
			double d = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
			double e = pos.getY() + 0.7 + (random.nextDouble() - 0.5) * 0.2;
			double f = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
			world.addParticleClient(DustParticleEffect.DEFAULT, d, e, f, 0.0, 0.0, 0.0);
		}
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	private static boolean isBurnedOut(World world, BlockPos pos, boolean addNew) {
		List<RedstoneTorchBlock.BurnoutEntry> list = BURNOUT_MAP.computeIfAbsent(world, worldx -> Lists.newArrayList());
		if (addNew) {
			list.add(new RedstoneTorchBlock.BurnoutEntry(pos.toImmutable(), world.getTime()));
		}

		int i = 0;

		for (RedstoneTorchBlock.BurnoutEntry burnoutEntry : list) {
			if (burnoutEntry.pos.equals(pos)) {
				if (++i >= 8) {
					return true;
				}
			}
		}

		return false;
	}

	protected @Nullable WireOrientation getEmissionOrientation(World world, BlockState state) {
		return OrientationHelper.getEmissionOrientation(world, null, Direction.UP);
	}

	/**
	 * {@code BurnoutEntry}.
	 */
	public static class BurnoutEntry {

		final BlockPos pos;
		final long time;

		public BurnoutEntry(BlockPos pos, long time) {
			this.pos = pos;
			this.time = time;
		}
	}
}
