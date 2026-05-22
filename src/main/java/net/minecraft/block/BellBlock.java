package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.Attachment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
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
 * Блок колокола. Поддерживает 4 типа крепления ({@link Attachment}): к полу, потолку
 * и одной/двум стенам. Звонит при нажатии игрока, попадании снаряда, взрыве или
 * редстоун-сигнале. При звоне уведомляет ближайших рейдеров через {@link BellBlockEntity}.
 */
public class BellBlock extends BlockWithEntity {

	public static final MapCodec<BellBlock> CODEC = createCodec(BellBlock::new);
	public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
	public static final EnumProperty<Attachment> ATTACHMENT = Properties.ATTACHMENT;
	public static final BooleanProperty POWERED = Properties.POWERED;
	public static final int BELL_RING_NOTIFY_FLAGS = 1;
	private static final VoxelShape BELL_SHAPE =
			VoxelShapes.union(Block.createColumnShape(6.0, 6.0, 13.0), Block.createColumnShape(8.0, 4.0, 6.0));
	private static final VoxelShape CEILING_SHAPE =
			VoxelShapes.union(BELL_SHAPE, Block.createColumnShape(2.0, 13.0, 16.0));
	private static final Map<Direction.Axis, VoxelShape> FLOOR_SHAPES =
			VoxelShapes.createHorizontalAxisShapeMap(Block.createCuboidShape(16.0, 16.0, 8.0));
	private static final Map<Direction.Axis, VoxelShape> DOUBLE_WALL_SHAPES =
			VoxelShapes.createHorizontalAxisShapeMap(
					VoxelShapes.union(BELL_SHAPE, Block.createColumnShape(2.0, 16.0, 13.0, 15.0))
			);
	private static final Map<Direction, VoxelShape> SINGLE_WALL_SHAPES =
			VoxelShapes.createHorizontalFacingShapeMap(
					VoxelShapes.union(BELL_SHAPE, Block.createCuboidZShape(2.0, 13.0, 15.0, 0.0, 13.0))
			);

	@Override
	public MapCodec<BellBlock> getCodec() {
		return CODEC;
	}

	public BellBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager
				.getDefaultState()
				.with(FACING, Direction.NORTH)
				.with(ATTACHMENT, Attachment.FLOOR)
				.with(POWERED, false));
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
		boolean powered = world.isReceivingRedstonePower(pos);

		if (powered == state.get(POWERED)) {
			return;
		}

		if (powered) {
			ring(world, pos, null);
		}

		world.setBlockState(pos, state.with(POWERED, powered), 3);
	}

	@Override
	protected void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
		PlayerEntity shooter = projectile.getOwner() instanceof PlayerEntity player ? player : null;
		ring(world, state, hit, shooter, true);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		return ring(world, state, hit, player, true) ? ActionResult.SUCCESS : ActionResult.PASS;
	}

	/**
	 * Звонит в колокол при попадании в него (с проверкой точки удара или без).
	 * Если {@code checkHitPos} равен {@code true} — проверяет, что удар пришёлся
	 * именно по телу колокола, а не по опоре.
	 */
	public boolean ring(
			World world,
			BlockState state,
			BlockHitResult hitResult,
			@Nullable PlayerEntity player,
			boolean checkHitPos
	) {
		Direction side = hitResult.getSide();
		BlockPos hitPos = hitResult.getBlockPos();
		boolean hitOnBell = checkHitPos == false
				|| isPointOnBell(state, side, hitResult.getPos().y - hitPos.getY());

		if (hitOnBell == false) {
			return false;
		}

		boolean rang = ring(player, world, hitPos, side);

		if (rang && player != null) {
			player.incrementStat(Stats.BELL_RING);
		}

		return true;
	}

	private boolean isPointOnBell(BlockState state, Direction side, double y) {
		if (side.getAxis() == Direction.Axis.Y || y > 0.8124F) {
			return false;
		}

		Direction facing = state.get(FACING);
		Attachment attachment = state.get(ATTACHMENT);

		return switch (attachment) {
			case FLOOR -> facing.getAxis() == side.getAxis();
			case SINGLE_WALL, DOUBLE_WALL -> facing.getAxis() != side.getAxis();
			case CEILING -> true;
		};
	}

	public boolean ring(World world, BlockPos pos, @Nullable Direction direction) {
		return ring(null, world, pos, direction);
	}

	/**
	 * Активирует колокол на сервере: запускает анимацию через {@link BellBlockEntity},
	 * воспроизводит звук и испускает игровое событие. Если {@code direction} равен
	 * {@code null} — использует направление, в которое смотрит блок.
	 */
	public boolean ring(@Nullable Entity entity, World world, BlockPos pos, @Nullable Direction direction) {
		if (world.isClient() || world.getBlockEntity(pos) instanceof BellBlockEntity == false) {
			return false;
		}

		BellBlockEntity bell = (BellBlockEntity) world.getBlockEntity(pos);
		Direction ringDirection = direction != null ? direction : world.getBlockState(pos).get(FACING);

		bell.activate(ringDirection);
		world.playSound(null, pos, SoundEvents.BLOCK_BELL_USE, SoundCategory.BLOCKS, 2.0F, 1.0F);
		world.emitGameEvent(entity, GameEvent.BLOCK_CHANGE, pos);

		return true;
	}

	private VoxelShape getShape(BlockState state) {
		Direction facing = state.get(FACING);

		return switch (state.get(ATTACHMENT)) {
			case FLOOR -> FLOOR_SHAPES.get(facing.getAxis());
			case SINGLE_WALL -> SINGLE_WALL_SHAPES.get(facing);
			case DOUBLE_WALL -> DOUBLE_WALL_SHAPES.get(facing.getAxis());
			case CEILING -> CEILING_SHAPE;
		};
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return getShape(state);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return getShape(state);
	}

	/**
	 * Определяет начальное состояние при размещении. Пробует разместить колокол
	 * в порядке приоритета: потолок/пол (при ударе по горизонтальной поверхности),
	 * двойная стена → одиночная стена → пол/потолок (при ударе по вертикальной).
	 */
	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		Direction side = ctx.getSide();
		BlockPos pos = ctx.getBlockPos();
		World world = ctx.getWorld();
		Direction.Axis axis = side.getAxis();

		if (axis == Direction.Axis.Y) {
			Attachment attachment = side == Direction.DOWN ? Attachment.CEILING : Attachment.FLOOR;
			BlockState candidate = getDefaultState()
					.with(ATTACHMENT, attachment)
					.with(FACING, ctx.getHorizontalPlayerFacing());

			if (candidate.canPlaceAt(world, pos)) {
				return candidate;
			}

			return null;
		}

		boolean hasDoubleWallSupport = axis == Direction.Axis.X
				&& world.getBlockState(pos.west()).isSideSolidFullSquare(world, pos.west(), Direction.EAST)
				&& world.getBlockState(pos.east()).isSideSolidFullSquare(world, pos.east(), Direction.WEST)
				|| axis == Direction.Axis.Z
				&& world.getBlockState(pos.north()).isSideSolidFullSquare(world, pos.north(), Direction.SOUTH)
				&& world.getBlockState(pos.south()).isSideSolidFullSquare(world, pos.south(), Direction.NORTH);

		BlockState wallCandidate = getDefaultState()
				.with(FACING, side.getOpposite())
				.with(ATTACHMENT, hasDoubleWallSupport ? Attachment.DOUBLE_WALL : Attachment.SINGLE_WALL);

		if (wallCandidate.canPlaceAt(world, pos)) {
			return wallCandidate;
		}

		boolean hasFloorSupport = world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP);
		BlockState fallbackCandidate = wallCandidate.with(ATTACHMENT, hasFloorSupport ? Attachment.FLOOR : Attachment.CEILING);

		if (fallbackCandidate.canPlaceAt(world, pos)) {
			return fallbackCandidate;
		}

		return null;
	}

	@Override
	protected void onExploded(
			BlockState state,
			ServerWorld world,
			BlockPos pos,
			Explosion explosion,
			BiConsumer<ItemStack, BlockPos> stackMerger
	) {
		if (explosion.canTriggerBlocks()) {
			ring(world, pos, null);
		}

		super.onExploded(state, world, pos, explosion, stackMerger);
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
		Attachment attachment = state.get(ATTACHMENT);
		Direction supportSide = getPlacementSide(state).getOpposite();

		if (supportSide == direction
				&& state.canPlaceAt(world, pos) == false
				&& attachment != Attachment.DOUBLE_WALL
		) {
			return Blocks.AIR.getDefaultState();
		}

		if (direction.getAxis() == state.get(FACING).getAxis()) {
			if (attachment == Attachment.DOUBLE_WALL
					&& neighborState.isSideSolidFullSquare(world, neighborPos, direction) == false
			) {
				return state.with(ATTACHMENT, Attachment.SINGLE_WALL).with(FACING, direction.getOpposite());
			}

			if (attachment == Attachment.SINGLE_WALL
					&& supportSide.getOpposite() == direction
					&& neighborState.isSideSolidFullSquare(world, neighborPos, state.get(FACING))
			) {
				return state.with(ATTACHMENT, Attachment.DOUBLE_WALL);
			}
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
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		Direction supportSide = getPlacementSide(state).getOpposite();

		return supportSide == Direction.UP
				? Block.sideCoversSmallSquare(world, pos.up(), Direction.DOWN)
				: WallMountedBlock.canPlaceAt(world, pos, supportSide);
	}

	private static Direction getPlacementSide(BlockState state) {
		return switch (state.get(ATTACHMENT)) {
			case FLOOR -> Direction.UP;
			case CEILING -> Direction.DOWN;
			default -> state.get(FACING).getOpposite();
		};
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, ATTACHMENT, POWERED);
	}

	@Override
	public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BellBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
			World world,
			BlockState state,
			BlockEntityType<T> type
	) {
		return validateTicker(
				type,
				BlockEntityType.BELL,
				world.isClient() ? BellBlockEntity::clientTick : BellBlockEntity::serverTick
		);
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}
}
