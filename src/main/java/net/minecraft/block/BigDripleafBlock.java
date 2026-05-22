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
 * Блок большого листа капли. Имеет 4 состояния наклона ({@link Tilt}): NONE, UNSTABLE,
 * PARTIAL, FULL. При нахождении существа на листе запускается таймер наклона: лист
 * постепенно опускается (NONE → UNSTABLE → PARTIAL → FULL), затем сбрасывается обратно.
 * Редстоун-сигнал немедленно сбрасывает наклон. Поддерживает водозаполнение и удобрение.
 */
public class BigDripleafBlock extends HorizontalFacingBlock implements Fertilizable, Waterloggable {

	public static final MapCodec<BigDripleafBlock> CODEC = createCodec(BigDripleafBlock::new);
	private static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	private static final EnumProperty<Tilt> TILT = Properties.TILT;
	private static final int NO_TILT_DELAY = -1;
	private static final int UNSTABLE_TILT_DELAY = 5;
	private static final int LEAF_SHAPE_MIN_HEIGHT = 11;
	private static final int LEAF_SHAPE_MAX_HEIGHT = 13;
	private static final Object2IntMap<Tilt> NEXT_TILT_DELAYS = Util.make(
			new Object2IntArrayMap(), delays -> {
				delays.defaultReturnValue(NO_TILT_DELAY);
				delays.put(Tilt.UNSTABLE, 10);
				delays.put(Tilt.PARTIAL, 10);
				delays.put(Tilt.FULL, 100);
			}
	);
	private static final Map<Tilt, VoxelShape> SHAPES_BY_TILT = Maps.newEnumMap(
			Map.of(
					Tilt.NONE, Block.createColumnShape(16.0, LEAF_SHAPE_MIN_HEIGHT, 15.0),
					Tilt.UNSTABLE, Block.createColumnShape(16.0, LEAF_SHAPE_MIN_HEIGHT, 15.0),
					Tilt.PARTIAL, Block.createColumnShape(16.0, LEAF_SHAPE_MIN_HEIGHT, LEAF_SHAPE_MAX_HEIGHT),
					Tilt.FULL, VoxelShapes.empty()
			)
	);
	private final Function<BlockState, VoxelShape> shapeFunction;

	@Override
	public MapCodec<BigDripleafBlock> getCodec() {
		return CODEC;
	}

	public BigDripleafBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager
				.getDefaultState()
				.with(WATERLOGGED, false)
				.with(FACING, Direction.NORTH)
				.with(TILT, Tilt.NONE));
		shapeFunction = createShapeFunction();
	}

	private Function<BlockState, VoxelShape> createShapeFunction() {
		Map<Direction, VoxelShape> stemShapes = VoxelShapes.createHorizontalFacingShapeMap(
				Block.createColumnShape(6.0, 0.0, 13.0).offset(0.0, 0.0, 0.25).simplify()
		);

		return createShapeFunction(
				state -> VoxelShapes.union(
						SHAPES_BY_TILT.get(state.get(TILT)),
						stemShapes.get(state.get(FACING))
				),
				WATERLOGGED
		);
	}

	/**
	 * Выращивает колонию большого листа капли заданной высоты (2–5 блоков) вверх от
	 * указанной позиции. Заполняет промежуточные блоки стеблями, верхний — листом.
	 */
	public static void grow(WorldAccess world, Random random, BlockPos pos, Direction direction) {
		int targetHeight = MathHelper.nextInt(random, 2, 5);
		BlockPos.Mutable mutable = pos.mutableCopy();
		int grownBlocks = 0;

		while (grownBlocks < targetHeight && canGrowInto(world, mutable, world.getBlockState(mutable))) {
			grownBlocks++;
			mutable.move(Direction.UP);
		}

		int topY = pos.getY() + grownBlocks - 1;
		mutable.setY(pos.getY());

		while (mutable.getY() < topY) {
			BigDripleafStemBlock.placeStemAt(world, mutable, world.getFluidState(mutable), direction);
			mutable.move(Direction.UP);
		}

		placeDripleafAt(world, mutable, world.getFluidState(mutable), direction);
	}

	private static boolean canGrowInto(BlockState state) {
		return state.isAir() || state.isOf(Blocks.WATER) || state.isOf(Blocks.SMALL_DRIPLEAF);
	}

	protected static boolean canGrowInto(HeightLimitView world, BlockPos pos, BlockState state) {
		return world.isOutOfHeightLimit(pos) == false && canGrowInto(state);
	}

	protected static boolean placeDripleafAt(
			WorldAccess world,
			BlockPos pos,
			FluidState fluidState,
			Direction direction
	) {
		BlockState leafState = Blocks.BIG_DRIPLEAF
				.getDefaultState()
				.with(WATERLOGGED, fluidState.isEqualAndStill(Fluids.WATER))
				.with(FACING, direction);

		return world.setBlockState(pos, leafState, 3);
	}

	@Override
	protected void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
		changeTilt(state, world, hit.getBlockPos(), Tilt.FULL, SoundEvents.BLOCK_BIG_DRIPLEAF_TILT_DOWN);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockState belowState = world.getBlockState(pos.down());

		return belowState.isOf(this)
				|| belowState.isOf(Blocks.BIG_DRIPLEAF_STEM)
				|| belowState.isIn(BlockTags.BIG_DRIPLEAF_PLACEABLE);
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
		if (direction == Direction.DOWN && state.canPlaceAt(world, pos) == false) {
			return Blocks.AIR.getDefaultState();
		}

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

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		return canGrowInto(world.getBlockState(pos.up()));
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		BlockPos abovePos = pos.up();
		BlockState aboveState = world.getBlockState(abovePos);

		if (canGrowInto(world, abovePos, aboveState) == false) {
			return;
		}

		Direction facing = state.get(FACING);
		BigDripleafStemBlock.placeStemAt(world, pos, state.getFluidState(), facing);
		placeDripleafAt(world, abovePos, aboveState.getFluidState(), facing);
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
		if (world.isClient()) {
			return;
		}

		if (state.get(TILT) == Tilt.NONE
				&& isEntityAbove(pos, entity)
				&& world.isReceivingRedstonePower(pos) == false
		) {
			changeTilt(state, world, pos, Tilt.UNSTABLE, null);
		}
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.isReceivingRedstonePower(pos)) {
			resetTilt(state, world, pos);
			return;
		}

		Tilt tilt = state.get(TILT);

		if (tilt == Tilt.UNSTABLE) {
			changeTilt(state, world, pos, Tilt.PARTIAL, SoundEvents.BLOCK_BIG_DRIPLEAF_TILT_DOWN);
		} else if (tilt == Tilt.PARTIAL) {
			changeTilt(state, world, pos, Tilt.FULL, SoundEvents.BLOCK_BIG_DRIPLEAF_TILT_DOWN);
		} else if (tilt == Tilt.FULL) {
			resetTilt(state, world, pos);
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
		float pitch = MathHelper.nextBetween(world.random, 0.8F, 1.2F);
		world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, pitch);
	}

	private static boolean isEntityAbove(BlockPos pos, Entity entity) {
		return entity.isOnGround() && entity.getEntityPos().y > pos.getY() + 0.6875F;
	}

	private void changeTilt(BlockState state, World world, BlockPos pos, Tilt tilt, @Nullable SoundEvent sound) {
		changeTilt(state, world, pos, tilt);

		if (sound != null) {
			playTiltSound(world, pos, sound);
		}

		int delay = NEXT_TILT_DELAYS.getInt(tilt);

		if (delay != NO_TILT_DELAY) {
			world.scheduleBlockTick(pos, this, delay);
		}
	}

	private static void resetTilt(BlockState state, World world, BlockPos pos) {
		changeTilt(state, world, pos, Tilt.NONE);

		if (state.get(TILT) != Tilt.NONE) {
			playTiltSound(world, pos, SoundEvents.BLOCK_BIG_DRIPLEAF_TILT_UP);
		}
	}

	private static void changeTilt(BlockState state, World world, BlockPos pos, Tilt tilt) {
		Tilt previousTilt = state.get(TILT);
		world.setBlockState(pos, state.with(TILT, tilt), 2);

		if (tilt.isStable() && tilt != previousTilt) {
			world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
		}
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_TILT.get(state.get(TILT));
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapeFunction.apply(state);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState belowState = ctx.getWorld().getBlockState(ctx.getBlockPos().down());
		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
		boolean inheritFacing = belowState.isOf(Blocks.BIG_DRIPLEAF) || belowState.isOf(Blocks.BIG_DRIPLEAF_STEM);

		return getDefaultState()
				.with(WATERLOGGED, fluidState.isEqualAndStill(Fluids.WATER))
				.with(FACING, inheritFacing ? belowState.get(FACING) : ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED, FACING, TILT);
	}
}
