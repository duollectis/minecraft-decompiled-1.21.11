package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

/**
 * Блок высушенного гаста. При помещении в воду постепенно гидратируется (3 стадии),
 * после чего спавнит детёныша счастливого гаста. Без воды — медленно теряет гидратацию.
 */
public class DriedGhastBlock extends HorizontalFacingBlock implements Waterloggable {

	public static final MapCodec<DriedGhastBlock> CODEC = createCodec(DriedGhastBlock::new);
	public static final int MAX_HYDRATION = 3;
	public static final IntProperty HYDRATION = Properties.HYDRATION;
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	public static final int HYDRATION_TICK_TIME = 5000;
	private static final VoxelShape SHAPE = Block.createColumnShape(10.0, 10.0, 0.0, 10.0);

	@Override
	public MapCodec<DriedGhastBlock> getCodec() {
		return CODEC;
	}

	public DriedGhastBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(
				stateManager
						.getDefaultState()
						.with(FACING, Direction.NORTH)
						.with(HYDRATION, 0)
						.with(WATERLOGGED, false)
		);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, HYDRATION, WATERLOGGED);
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
		if (state.get(WATERLOGGED)) {
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
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	public int getHydration(BlockState state) {
		return state.get(HYDRATION);
	}

	private boolean isFullyHydrated(BlockState state) {
		return getHydration(state) == MAX_HYDRATION;
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (state.get(WATERLOGGED)) {
			tickHydration(state, world, pos, random);
			return;
		}

		int hydration = getHydration(state);

		if (hydration > 0) {
			world.setBlockState(pos, state.with(HYDRATION, hydration - 1), Block.NOTIFY_LISTENERS);
			world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(state));
		}
	}

	private void tickHydration(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (isFullyHydrated(state) == false) {
			world.playSound(null, pos, SoundEvents.BLOCK_DRIED_GHAST_TRANSITION, SoundCategory.BLOCKS, 1.0F, 1.0F);
			world.setBlockState(pos, state.with(HYDRATION, getHydration(state) + 1), Block.NOTIFY_LISTENERS);
			world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(state));
			return;
		}

		spawnGhastling(world, pos, state);
	}

	private void spawnGhastling(ServerWorld world, BlockPos pos, BlockState state) {
		world.removeBlock(pos, false);
		HappyGhastEntity ghastling = EntityType.HAPPY_GHAST.create(world, SpawnReason.BREEDING);

		if (ghastling == null) {
			return;
		}

		Vec3d spawnPos = pos.toBottomCenterPos();
		float yaw = Direction.getHorizontalDegreesOrThrow(state.get(FACING));

		ghastling.setBaby(true);
		ghastling.setHeadYaw(yaw);
		ghastling.refreshPositionAndAngles(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), yaw, 0.0F);
		world.spawnEntity(ghastling);
		world.playSoundFromEntity(null, ghastling, SoundEvents.ENTITY_GHASTLING_SPAWN, SoundCategory.BLOCKS, 1.0F, 1.0F);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;

		if (state.get(WATERLOGGED)) {
			if (random.nextInt(40) == 0) {
				world.playSoundClient(x, y, z, SoundEvents.BLOCK_DRIED_GHAST_AMBIENT_WATER, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
			}

			if (random.nextInt(6) == 0) {
				world.addParticleClient(
						ParticleTypes.HAPPY_VILLAGER,
						x + (random.nextFloat() * 2.0F - 1.0F) / 3.0F,
						y + 0.4,
						z + (random.nextFloat() * 2.0F - 1.0F) / 3.0F,
						0.0,
						random.nextFloat(),
						0.0
				);
			}

			return;
		}

		if (random.nextInt(40) == 0
				&& world.getBlockState(pos.down()).isIn(BlockTags.TRIGGERS_AMBIENT_DRIED_GHAST_BLOCK_SOUNDS)
		) {
			world.playSoundClient(x, y, z, SoundEvents.BLOCK_DRIED_GHAST_AMBIENT, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
		}

		if (random.nextInt(6) == 0) {
			world.addParticleClient(ParticleTypes.WHITE_SMOKE, x, y, z, 0.0, 0.02, 0.0);
		}
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		boolean needsTick = state.get(WATERLOGGED) || state.get(HYDRATION) > 0;

		if (needsTick && world.getBlockTickScheduler().isQueued(pos, this) == false) {
			world.scheduleBlockTick(pos, this, HYDRATION_TICK_TIME);
		}
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
		boolean inWater = fluidState.getFluid() == Fluids.WATER;

		return super
				.getPlacementState(ctx)
				.with(WATERLOGGED, inWater)
				.with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	@Override
	public boolean tryFillWithFluid(WorldAccess world, BlockPos pos, BlockState state, FluidState fluidState) {
		if (state.get(Properties.WATERLOGGED) || fluidState.getFluid() != Fluids.WATER) {
			return false;
		}

		if (world.isClient() == false) {
			world.setBlockState(pos, state.with(Properties.WATERLOGGED, true), Block.NOTIFY_ALL);
			world.scheduleFluidTick(pos, fluidState.getFluid(), fluidState.getFluid().getTickRate(world));
			world.playSound(null, pos, SoundEvents.BLOCK_DRIED_GHAST_PLACE_IN_WATER, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}

		return true;
	}

	@Override
	public void onPlaced(
			World world,
			BlockPos pos,
			BlockState state,
			@Nullable LivingEntity placer,
			ItemStack itemStack
	) {
		super.onPlaced(world, pos, state, placer, itemStack);
		world.playSound(
				null,
				pos,
				state.get(WATERLOGGED) ? SoundEvents.BLOCK_DRIED_GHAST_PLACE_IN_WATER
				                       : SoundEvents.BLOCK_DRIED_GHAST_PLACE,
				SoundCategory.BLOCKS,
				1.0F,
				1.0F
		);
	}

	@Override
	public boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}
}
