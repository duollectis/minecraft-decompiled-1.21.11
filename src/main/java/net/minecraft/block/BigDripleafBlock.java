package net.minecraft.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.enums.Tilt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.*;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

/**
 * {@code BigDripleafBlock}.
 */
public class BigDripleafBlock extends HorizontalFacingBlock implements Fertilizable, Waterloggable {

	public static final MapCodec<BigDripleafBlock> CODEC = createCodec(BigDripleafBlock::new);
	private static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	private static final EnumProperty<Tilt> TILT = Properties.TILT;
	private static final int NO_TILT_DELAY = -1;
	private static final Object2IntMap<Tilt> NEXT_TILT_DELAYS = Util.make(
			new Object2IntArrayMap(), delays -> {
				delays.defaultReturnValue(-1);
				delays.put(Tilt.UNSTABLE, 10);
				delays.put(Tilt.PARTIAL, 10);
				delays.put(Tilt.FULL, 100);
			}
	);
	private static final int UNSTABLE_TILT_DELAY = 5;
	private static final int LEAF_SHAPE_MIN_HEIGHT = 11;
	private static final int LEAF_SHAPE_MAX_HEIGHT = 13;
	private static final Map<Tilt, VoxelShape> SHAPES_BY_TILT = Maps.newEnumMap(
			Map.of(
					Tilt.NONE,
					Block.createColumnShape(16.0, 11.0, 15.0),
					Tilt.UNSTABLE,
					Block.createColumnShape(16.0, 11.0, 15.0),
					Tilt.PARTIAL,
					Block.createColumnShape(16.0, 11.0, 13.0),
					Tilt.FULL,
					VoxelShapes.empty()
			)
	);
	private final Function<BlockState, VoxelShape> shapeFunction;

	@Override
	public MapCodec<BigDripleafBlock> getCodec() {
		return CODEC;
	}

	public BigDripleafBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager
				.getDefaultState()
				.with(WATERLOGGED, false)
				.with(FACING, Direction.NORTH)
				.with(TILT, Tilt.NONE));
		this.shapeFunction = this.createShapeFunction();
	}

	private Function<BlockState, VoxelShape> createShapeFunction() {
		Map<Direction, VoxelShape>
				map =
				VoxelShapes.createHorizontalFacingShapeMap(Block
						.createColumnShape(6.0, 0.0, 13.0)
						.offset(0.0, 0.0, 0.25)
						.simplify());
		return this.createShapeFunction(
				state -> VoxelShapes.union(
						SHAPES_BY_TILT.get(state.get(TILT)),
						map.get(state.get(FACING))
				), WATERLOGGED
		);
	}

	/**
	 * Grow.
	 *
	 * @param world world
	 * @param random random
	 * @param pos pos
	 * @param direction direction
	 */
	public static void grow(WorldAccess world, Random random, BlockPos pos, Direction direction) {
		int i = MathHelper.nextInt(random, 2, 5);
		BlockPos.Mutable mutable = pos.mutableCopy();
		int j = 0;

		while (j < i && canGrowInto(world, mutable, world.getBlockState(mutable))) {
			j++;
			mutable.move(Direction.UP);
		}

		int k = pos.getY() + j - 1;
		mutable.setY(pos.getY());

		while (mutable.getY() < k) {
			BigDripleafStemBlock.placeStemAt(world, mutable, world.getFluidState(mutable), direction);
			mutable.move(Direction.UP);
		}

		placeDripleafAt(world, mutable, world.getFluidState(mutable), direction);
	}

	private static boolean canGrowInto(BlockState state) {
		return state.isAir() || state.isOf(Blocks.WATER) || state.isOf(Blocks.SMALL_DRIPLEAF);
	}

	/**
	 * Проверяет возможность grow into.
	 *
	 * @param world world
	 * @param pos pos
	 * @param state state
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	protected static boolean canGrowInto(HeightLimitView world, BlockPos pos, BlockState state) {
		return !world.isOutOfHeightLimit(pos) && canGrowInto(state);
	}

	protected static boolean placeDripleafAt(
			WorldAccess world,
			BlockPos pos,
			FluidState fluidState,
			Direction direction
	) {
		BlockState
				blockState =
				Blocks.BIG_DRIPLEAF
						.getDefaultState()
						.with(WATERLOGGED, fluidState.isEqualAndStill(Fluids.WATER))
						.with(FACING, direction);
		return world.setBlockState(pos, blockState, 3);
	}

	@Override
	protected void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
		this.changeTilt(state, world, hit.getBlockPos(), Tilt.FULL, SoundEvents.BLOCK_BIG_DRIPLEAF_TILT_DOWN);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos blockPos = pos.down();
		BlockState blockState = world.getBlockState(blockPos);
		return blockState.isOf(this) || blockState.isOf(Blocks.BIG_DRIPLEAF_STEM)
				|| blockState.isIn(BlockTags.BIG_DRIPLEAF_PLACEABLE);
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
		if (direction == Direction.DOWN && !state.canPlaceAt(world, pos)) {
			return Blocks.AIR.getDefaultState();
		}
		else {
			if (state.get(WATERLOGGED)) {
				tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
			}

			return direction == Direction.UP && neighborState.isOf(this)
			       ? Blocks.BIG_DRIPLEAF_STEM.getStateWithProperties(state)
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
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		BlockState blockState = world.getBlockState(pos.up());
		return canGrowInto(blockState);
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		BlockPos blockPos = pos.up();
		BlockState blockState = world.getBlockState(blockPos);
		if (canGrowInto(world, blockPos, blockState)) {
			Direction direction = state.get(FACING);
			BigDripleafStemBlock.placeStemAt(world, pos, state.getFluidState(), direction);
			placeDripleafAt(world, blockPos, blockState.getFluidState(), direction);
		}
	}

	@Override
	protected void onEntityCollision(
			BlockState state,
			World world,
			BlockPos pos,
			Entity entity,
			EntityCollisionHandler handler,
			boolean bl
	) {
		if (!world.isClient()) {
			if (state.get(TILT) == Tilt.NONE && isEntityAbove(pos, entity) && !world.isReceivingRedstonePower(pos)) {
				this.changeTilt(state, world, pos, Tilt.UNSTABLE, null);
			}
		}
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.isReceivingRedstonePower(pos)) {
			resetTilt(state, world, pos);
		}
		else {
			Tilt tilt = state.get(TILT);
			if (tilt == Tilt.UNSTABLE) {
				this.changeTilt(state, world, pos, Tilt.PARTIAL, SoundEvents.BLOCK_BIG_DRIPLEAF_TILT_DOWN);
			}
			else if (tilt == Tilt.PARTIAL) {
				this.changeTilt(state, world, pos, Tilt.FULL, SoundEvents.BLOCK_BIG_DRIPLEAF_TILT_DOWN);
			}
			else if (tilt == Tilt.FULL) {
				resetTilt(state, world, pos);
			}
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
		if (world.isReceivingRedstonePower(pos)) {
			resetTilt(state, world, pos);
		}
	}

	private static void playTiltSound(World world, BlockPos pos, SoundEvent soundEvent) {
		float f = MathHelper.nextBetween(world.random, 0.8F, 1.2F);
		world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, f);
	}

	private static boolean isEntityAbove(BlockPos pos, Entity entity) {
		return entity.isOnGround() && entity.getEntityPos().y > pos.getY() + 0.6875F;
	}

	private void changeTilt(BlockState state, World world, BlockPos pos, Tilt tilt, @Nullable SoundEvent sound) {
		changeTilt(state, world, pos, tilt);
		if (sound != null) {
			playTiltSound(world, pos, sound);
		}

		int i = NEXT_TILT_DELAYS.getInt(tilt);
		if (i != -1) {
			world.scheduleBlockTick(pos, this, i);
		}
	}

	private static void resetTilt(BlockState state, World world, BlockPos pos) {
		changeTilt(state, world, pos, Tilt.NONE);
		if (state.get(TILT) != Tilt.NONE) {
			playTiltSound(world, pos, SoundEvents.BLOCK_BIG_DRIPLEAF_TILT_UP);
		}
	}

	private static void changeTilt(BlockState state, World world, BlockPos pos, Tilt tilt) {
		Tilt tilt2 = state.get(TILT);
		world.setBlockState(pos, state.with(TILT, tilt), 2);
		if (tilt.isStable() && tilt != tilt2) {
			world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
		}
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_TILT.get(state.get(TILT));
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return this.shapeFunction.apply(state);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState blockState = ctx.getWorld().getBlockState(ctx.getBlockPos().down());
		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
		boolean bl = blockState.isOf(Blocks.BIG_DRIPLEAF) || blockState.isOf(Blocks.BIG_DRIPLEAF_STEM);
		return this.getDefaultState()
		           .with(WATERLOGGED, fluidState.isEqualAndStill(Fluids.WATER))
		           .with(FACING, bl ? blockState.get(FACING) : ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED, FACING, TILT);
	}
}
