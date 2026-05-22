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
 * Факел редстоуна — излучает сигнал мощностью 15 во все стороны, кроме верхней.
 * Гасится, если блок под ним получает сигнал редстоуна. При частом переключении
 * (более {@link #MAX_BURNOUT_COUNT} раз за {@link #BURNOUT_WINDOW_TICKS} тиков)
 * «перегорает» и перезажигается только через {@link #BURNOUT_RELIGHT_DELAY} тиков.
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
		setDefaultState(stateManager.getDefaultState().with(LIT, true));
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
	 * Проверяет, должен ли факел погаснуть — то есть получает ли блок под ним сигнал редстоуна.
	 */
	protected boolean shouldUnpower(World world, BlockPos pos, BlockState state) {
		return world.isEmittingRedstonePower(pos.down(), Direction.DOWN);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		boolean shouldUnpower = shouldUnpower(world, pos, state);
		List<RedstoneTorchBlock.BurnoutEntry> burnoutList = BURNOUT_MAP.get(world);

		while (burnoutList != null && !burnoutList.isEmpty() && world.getTime() - burnoutList.get(0).time > BURNOUT_WINDOW_TICKS) {
			burnoutList.remove(0);
		}

		if (state.get(LIT)) {
			if (shouldUnpower) {
				world.setBlockState(pos, state.with(LIT, false), 3);

				if (isBurnedOut(world, pos, true)) {
					world.syncWorldEvent(1502, pos, 0);
					world.scheduleBlockTick(pos, world.getBlockState(pos).getBlock(), BURNOUT_RELIGHT_DELAY);
				}
			}
		} else if (!shouldUnpower && !isBurnedOut(world, pos, false)) {
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
		if (state.get(LIT) == shouldUnpower(world, pos, state)
				&& !world.getBlockTickScheduler().isTicking(pos, this)
		) {
			world.scheduleBlockTick(pos, this, SCHEDULED_TICK_DELAY);
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

	/**
	 * Проверяет, «перегорел» ли факел — то есть переключался ли он слишком часто.
	 * Если {@code addNew} равен {@code true}, добавляет текущий момент в историю переключений.
	 */
	private static boolean isBurnedOut(World world, BlockPos pos, boolean addNew) {
		List<RedstoneTorchBlock.BurnoutEntry> entries = BURNOUT_MAP.computeIfAbsent(world, w -> Lists.newArrayList());

		if (addNew) {
			entries.add(new RedstoneTorchBlock.BurnoutEntry(pos.toImmutable(), world.getTime()));
		}

		int count = 0;

		for (RedstoneTorchBlock.BurnoutEntry entry : entries) {
			if (entry.pos.equals(pos)) {
				if (++count >= MAX_BURNOUT_COUNT) {
					return true;
				}
			}
		}

		return false;
	}

	protected @Nullable WireOrientation getEmissionOrientation(World world, BlockState state) {
		return OrientationHelper.getEmissionOrientation(world, null, Direction.UP);
	}

	public static class BurnoutEntry {

		final BlockPos pos;
		final long time;

		public BurnoutEntry(BlockPos pos, long time) {
			this.pos = pos;
			this.time = time;
		}
	}
}
