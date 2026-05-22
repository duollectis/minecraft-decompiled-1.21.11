package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Блок двери — двухблочная конструкция (нижняя и верхняя половины).
 * Поддерживает открытие вручную, редстоун-сигналом и взрывом ветра.
 * Петля двери определяется геометрией окружения при установке.
 */
public class DoorBlock extends Block {

	public static final MapCodec<DoorBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(
							BlockSetType.CODEC.fieldOf("block_set_type").forGetter(DoorBlock::getBlockSetType),
							createSettingsCodec()
					)
					.apply(instance, DoorBlock::new)
	);
	public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
	public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
	public static final EnumProperty<DoorHinge> HINGE = Properties.DOOR_HINGE;
	public static final BooleanProperty OPEN = Properties.OPEN;
	public static final BooleanProperty POWERED = Properties.POWERED;
	private static final Map<Direction, VoxelShape>
			SHAPES_BY_DIRECTION =
			VoxelShapes.createHorizontalFacingShapeMap(Block.createCuboidZShape(16.0, 13.0, 16.0));
	private final BlockSetType blockSetType;

	@Override
	public MapCodec<? extends DoorBlock> getCodec() {
		return CODEC;
	}

	public DoorBlock(BlockSetType type, AbstractBlock.Settings settings) {
		super(settings.sounds(type.soundType()));
		this.blockSetType = type;
		this.setDefaultState(
				this.stateManager
						.getDefaultState()
						.with(FACING, Direction.NORTH)
						.with(OPEN, false)
						.with(HINGE, DoorHinge.LEFT)
						.with(POWERED, false)
						.with(HALF, DoubleBlockHalf.LOWER)
		);
	}

	public BlockSetType getBlockSetType() {
		return this.blockSetType;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		Direction facing = state.get(FACING);
		Direction visualFacing = state.get(OPEN)
				? (state.get(HINGE) == DoorHinge.RIGHT
						? facing.rotateYCounterclockwise()
						: facing.rotateYClockwise()
				)
				: facing;

		return SHAPES_BY_DIRECTION.get(visualFacing);
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
		DoubleBlockHalf half = state.get(HALF);
		boolean isVertical = direction.getAxis() == Direction.Axis.Y;
		boolean isMatchingVertical = half == DoubleBlockHalf.LOWER == (direction == Direction.UP);

		if (isVertical && isMatchingVertical) {
			return neighborState.getBlock() instanceof DoorBlock && neighborState.get(HALF) != half
					? neighborState.with(HALF, half)
					: Blocks.AIR.getDefaultState();
		}

		return half == DoubleBlockHalf.LOWER && direction == Direction.DOWN && state.canPlaceAt(world, pos) == false
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
	protected void onExploded(
			BlockState state,
			ServerWorld world,
			BlockPos pos,
			Explosion explosion,
			BiConsumer<ItemStack, BlockPos> stackMerger
	) {
		if (explosion.canTriggerBlocks()
				&& state.get(HALF) == DoubleBlockHalf.LOWER
				&& blockSetType.canOpenByWindCharge()
				&& state.get(POWERED) == false
		) {
			setOpen(null, world, state, pos, isOpen(state) == false);
		}

		super.onExploded(state, world, pos, explosion, stackMerger);
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient() && (player.shouldSkipBlockDrops() || !player.canHarvest(state))) {
			TallPlantBlock.onBreakInCreative(world, pos, state, player);
		}

		return super.onBreak(world, pos, state, player);
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return switch (type) {
			case LAND, AIR -> state.get(OPEN);
			case WATER -> false;
		};
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockPos blockPos = ctx.getBlockPos();
		World world = ctx.getWorld();

		if (blockPos.getY() >= world.getTopYInclusive() || world.getBlockState(blockPos.up()).canReplace(ctx) == false) {
			return null;
		}

		boolean powered = world.isReceivingRedstonePower(blockPos) || world.isReceivingRedstonePower(blockPos.up());

		return getDefaultState()
				.with(FACING, ctx.getHorizontalPlayerFacing())
				.with(HINGE, getHinge(ctx))
				.with(POWERED, powered)
				.with(OPEN, powered)
				.with(HALF, DoubleBlockHalf.LOWER);
	}

	@Override
	public void onPlaced(
			World world,
			BlockPos pos,
			BlockState state,
			@Nullable LivingEntity placer,
			ItemStack itemStack
	) {
		world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
	}

	/**
	 * Определяет сторону петли двери при установке на основе геометрии соседних блоков
	 * и точки попадания курсора. Учитывает наличие соседних дверей и полных кубов.
	 */
	private DoorHinge getHinge(ItemPlacementContext ctx) {
		BlockView blockView = ctx.getWorld();
		BlockPos blockPos = ctx.getBlockPos();
		Direction facing = ctx.getHorizontalPlayerFacing();
		BlockPos above = blockPos.up();

		Direction ccw = facing.rotateYCounterclockwise();
		BlockPos ccwPos = blockPos.offset(ccw);
		BlockState ccwState = blockView.getBlockState(ccwPos);
		BlockPos ccwAbove = above.offset(ccw);
		BlockState ccwAboveState = blockView.getBlockState(ccwAbove);

		Direction cw = facing.rotateYClockwise();
		BlockPos cwPos = blockPos.offset(cw);
		BlockState cwState = blockView.getBlockState(cwPos);
		BlockPos cwAbove = above.offset(cw);
		BlockState cwAboveState = blockView.getBlockState(cwAbove);

		int score = (ccwState.isFullCube(blockView, ccwPos) ? -1 : 0)
				+ (ccwAboveState.isFullCube(blockView, ccwAbove) ? -1 : 0)
				+ (cwState.isFullCube(blockView, cwPos) ? 1 : 0)
				+ (cwAboveState.isFullCube(blockView, cwAbove) ? 1 : 0);

		boolean hasCcwDoor = ccwState.getBlock() instanceof DoorBlock && ccwState.get(HALF) == DoubleBlockHalf.LOWER;
		boolean hasCwDoor = cwState.getBlock() instanceof DoorBlock && cwState.get(HALF) == DoubleBlockHalf.LOWER;

		if ((hasCcwDoor == false || hasCwDoor) && score <= 0) {
			if ((hasCwDoor == false || hasCcwDoor) && score >= 0) {
				int offsetX = facing.getOffsetX();
				int offsetZ = facing.getOffsetZ();
				Vec3d hit = ctx.getHitPos();
				double hitX = hit.x - blockPos.getX();
				double hitZ = hit.z - blockPos.getZ();

				return (offsetX >= 0 || hitZ >= 0.5)
						&& (offsetX <= 0 || hitZ <= 0.5)
						&& (offsetZ >= 0 || hitX <= 0.5)
						&& (offsetZ <= 0 || hitX >= 0.5)
						? DoorHinge.LEFT
						: DoorHinge.RIGHT;
			}

			return DoorHinge.LEFT;
		}

		return DoorHinge.RIGHT;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (blockSetType.canOpenByHand() == false) {
			return ActionResult.PASS;
		}

		state = state.cycle(OPEN);
		world.setBlockState(pos, state, 10);
		playOpenCloseSound(player, world, pos, state.get(OPEN));
		world.emitGameEvent(player, isOpen(state) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);

		return ActionResult.SUCCESS;
	}

	public boolean isOpen(BlockState state) {
		return state.get(OPEN);
	}

	public void setOpen(@Nullable Entity entity, World world, BlockState state, BlockPos pos, boolean open) {
		if (state.isOf(this) == false || state.get(OPEN) == open) {
			return;
		}

		world.setBlockState(pos, state.with(OPEN, open), 10);
		playOpenCloseSound(entity, world, pos, open);
		world.emitGameEvent(entity, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
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
		Direction otherHalf = state.get(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
		boolean powered = world.isReceivingRedstonePower(pos) || world.isReceivingRedstonePower(pos.offset(otherHalf));
		boolean wasPowered = state.get(POWERED);

		if (getDefaultState().isOf(sourceBlock) || powered == wasPowered) {
			return;
		}

		if (powered != state.get(OPEN)) {
			playOpenCloseSound(null, world, pos, powered);
			world.emitGameEvent(null, powered ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
		}

		world.setBlockState(pos, state.with(POWERED, powered).with(OPEN, powered), Block.NOTIFY_LISTENERS);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos blockPos = pos.down();
		BlockState blockState = world.getBlockState(blockPos);
		return state.get(HALF) == DoubleBlockHalf.LOWER ? blockState.isSideSolidFullSquare(
				world,
				blockPos,
				Direction.UP
		) : blockState.isOf(this);
	}

	private void playOpenCloseSound(@Nullable Entity entity, World world, BlockPos pos, boolean open) {
		world.playSound(
				entity,
				pos,
				open ? this.blockSetType.doorOpen() : this.blockSetType.doorClose(),
				SoundCategory.BLOCKS,
				1.0F,
				world.getRandom().nextFloat() * 0.1F + 0.9F
		);
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return mirror == BlockMirror.NONE ? state : state.rotate(mirror.getRotation(state.get(FACING))).cycle(HINGE);
	}

	@Override
	protected long getRenderingSeed(BlockState state, BlockPos pos) {
		return MathHelper.hashCode(
				pos.getX(),
				pos.down(state.get(HALF) == DoubleBlockHalf.LOWER ? 0 : 1).getY(),
				pos.getZ()
		);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(HALF, FACING, OPEN, HINGE, POWERED);
	}

	public static boolean canOpenByHand(World world, BlockPos pos) {
		return canOpenByHand(world.getBlockState(pos));
	}

	public static boolean canOpenByHand(BlockState state) {
		return state.getBlock() instanceof DoorBlock doorBlock && doorBlock.getBlockSetType().canOpenByHand();
	}
}
