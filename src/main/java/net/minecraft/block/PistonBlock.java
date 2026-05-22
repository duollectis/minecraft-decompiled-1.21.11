package net.minecraft.block;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.block.enums.PistonType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import net.minecraft.world.block.OrientationHelper;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Блок поршня — выдвигается при получении сигнала редстоуна, толкая до {@link #MAX_MOVABLE_BLOCKS} блоков.
 * Липкий поршень ({@code sticky=true}) также тянет блок обратно при сжатии.
 */
public class PistonBlock extends FacingBlock {

	public static final MapCodec<PistonBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Codec.BOOL.fieldOf("sticky").forGetter(block -> block.sticky), createSettingsCodec())
					.apply(instance, PistonBlock::new)
	);
	public static final BooleanProperty EXTENDED = Properties.EXTENDED;

	/** Тип события: поршень не движется (используется в {@link #onSyncedBlockEvent}). */
	public static final int EVENT_EXTEND = 0;

	/** Тип события: поршень сжимается (обычный). */
	public static final int EVENT_RETRACT = 1;

	/** Тип события: поршень сжимается с задержкой (встречный поршень). */
	public static final int EVENT_RETRACT_DELAYED = 2;

	public static final int MAX_MOVABLE_BLOCKS = 4;
	private static final Map<Direction, VoxelShape> EXTENDED_SHAPES_BY_DIRECTION =
			VoxelShapes.createFacingShapeMap(Block.createCuboidZShape(16.0, 4.0, 16.0));
	private final boolean sticky;

	@Override
	public MapCodec<PistonBlock> getCodec() {
		return CODEC;
	}

	public PistonBlock(boolean sticky, AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(FACING, Direction.NORTH).with(EXTENDED, false));
		this.sticky = sticky;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return state.get(EXTENDED) ? EXTENDED_SHAPES_BY_DIRECTION.get(state.get(FACING)) : VoxelShapes.fullCube();
	}

	@Override
	public void onPlaced(
			World world,
			BlockPos pos,
			BlockState state,
			@Nullable LivingEntity placer,
			ItemStack itemStack
	) {
		if (!world.isClient()) {
			this.tryMove(world, pos, state);
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
		if (!world.isClient()) {
			this.tryMove(world, pos, state);
		}
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if (!oldState.isOf(state.getBlock())) {
			if (!world.isClient() && world.getBlockEntity(pos) == null) {
				this.tryMove(world, pos, state);
			}
		}
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite()).with(EXTENDED, false);
	}

	private void tryMove(World world, BlockPos pos, BlockState state) {
		Direction facing = state.get(FACING);
		boolean shouldExtend = shouldExtend(world, pos, facing);

		if (shouldExtend && state.get(EXTENDED) == false) {
			if (new PistonHandler(world, pos, facing, true).calculatePush()) {
				world.addSyncedBlockEvent(pos, this, EVENT_EXTEND, facing.getIndex());
			}

			return;
		}

		if (shouldExtend == false && state.get(EXTENDED)) {
			BlockPos twoAhead = pos.offset(facing, 2);
			BlockState twoAheadState = world.getBlockState(twoAhead);
			int retractEvent = EVENT_RETRACT;

			if (twoAheadState.isOf(Blocks.MOVING_PISTON)
					&& twoAheadState.get(FACING) == facing
					&& world.getBlockEntity(twoAhead) instanceof PistonBlockEntity movingPiston
					&& movingPiston.isExtending()
					&& (movingPiston.getProgress(0.0F) < 0.5F
					|| world.getTime() == movingPiston.getSavedWorldTime()
					|| ((ServerWorld) world).isInBlockTick())
			) {
				retractEvent = EVENT_RETRACT_DELAYED;
			}

			world.addSyncedBlockEvent(pos, this, retractEvent, facing.getIndex());
		}
	}

	private boolean shouldExtend(RedstoneView world, BlockPos pos, Direction pistonFace) {
		for (Direction direction : Direction.values()) {
			if (direction != pistonFace && world.isEmittingRedstonePower(pos.offset(direction), direction)) {
				return true;
			}
		}

		if (world.isEmittingRedstonePower(pos, Direction.DOWN)) {
			return true;
		}

		BlockPos above = pos.up();

		for (Direction direction : Direction.values()) {
			if (direction != Direction.DOWN && world.isEmittingRedstonePower(above.offset(direction), direction)) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data) {
		Direction facing = state.get(FACING);
		BlockState extendedState = state.with(EXTENDED, true);

		if (world.isClient() == false) {
			boolean powered = shouldExtend(world, pos, facing);

			if (powered && (type == EVENT_RETRACT || type == EVENT_RETRACT_DELAYED)) {
				world.setBlockState(pos, extendedState, Block.NOTIFY_LISTENERS);
				return false;
			}

			if (powered == false && type == EVENT_EXTEND) {
				return false;
			}
		}

		if (type == EVENT_EXTEND) {
			if (move(world, pos, facing, true) == false) {
				return false;
			}

			world.setBlockState(pos, extendedState, 67);
			world.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.25F + 0.6F);
			world.emitGameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Emitter.of(extendedState));
			return true;
		}

		if (type == EVENT_RETRACT || type == EVENT_RETRACT_DELAYED) {
			BlockEntity frontEntity = world.getBlockEntity(pos.offset(facing));

			if (frontEntity instanceof PistonBlockEntity pistonFront) {
				pistonFront.finish();
			}

			PistonType pistonType = sticky ? PistonType.STICKY : PistonType.DEFAULT;
			BlockState movingState = Blocks.MOVING_PISTON
					.getDefaultState()
					.with(PistonExtensionBlock.FACING, facing)
					.with(PistonExtensionBlock.TYPE, pistonType);

			world.setBlockState(pos, movingState, 276);
			world.addBlockEntity(PistonExtensionBlock.createBlockEntityPiston(
					pos,
					movingState,
					getDefaultState().with(FACING, Direction.byIndex(data & 7)),
					facing,
					false,
					true
			));
			world.updateNeighbors(pos, movingState.getBlock());
			movingState.updateNeighbors(world, pos, Block.NOTIFY_LISTENERS);

			if (sticky) {
				BlockPos pullTarget = pos.add(
						facing.getOffsetX() * 2,
						facing.getOffsetY() * 2,
						facing.getOffsetZ() * 2
				);
				BlockState pullState = world.getBlockState(pullTarget);
				boolean alreadyFinished = false;

				if (pullState.isOf(Blocks.MOVING_PISTON)
						&& world.getBlockEntity(pullTarget) instanceof PistonBlockEntity movingPiston
						&& movingPiston.getFacing() == facing
						&& movingPiston.isExtending()
				) {
					movingPiston.finish();
					alreadyFinished = true;
				}

				if (alreadyFinished == false) {
					boolean canPull = type == EVENT_RETRACT
							&& pullState.isAir() == false
							&& isMovable(pullState, world, pullTarget, facing.getOpposite(), false, facing)
							&& (pullState.getPistonBehavior() == PistonBehavior.NORMAL
							|| pullState.isOf(Blocks.PISTON)
							|| pullState.isOf(Blocks.STICKY_PISTON));

					if (canPull) {
						move(world, pos, facing, false);
					} else {
						world.removeBlock(pos.offset(facing), false);
					}
				}
			} else {
				world.removeBlock(pos.offset(facing), false);
			}

			world.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.15F + 0.6F);
			world.emitGameEvent(GameEvent.BLOCK_DEACTIVATE, pos, GameEvent.Emitter.of(movingState));
		}

		return true;
	}

	/**
	 * Проверяет, может ли блок быть перемещён поршнем.
	 * Учитывает границы мира, поведение блока ({@link PistonBehavior}) и наличие блок-сущности.
	 */
	public static boolean isMovable(
			BlockState state,
			World world,
			BlockPos pos,
			Direction direction,
			boolean canBreak,
			Direction pistonDir
	) {
		if (pos.getY() < world.getBottomY()
				|| pos.getY() > world.getTopYInclusive()
				|| world.getWorldBorder().contains(pos) == false
		) {
			return false;
		}

		if (state.isAir()) {
			return true;
		}

		if (state.isOf(Blocks.OBSIDIAN)
				|| state.isOf(Blocks.CRYING_OBSIDIAN)
				|| state.isOf(Blocks.RESPAWN_ANCHOR)
				|| state.isOf(Blocks.REINFORCED_DEEPSLATE)
		) {
			return false;
		}

		if (direction == Direction.DOWN && pos.getY() == world.getBottomY()) {
			return false;
		}

		if (direction == Direction.UP && pos.getY() == world.getTopYInclusive()) {
			return false;
		}

		if (state.isOf(Blocks.PISTON) || state.isOf(Blocks.STICKY_PISTON)) {
			return state.get(EXTENDED) == false && state.hasBlockEntity() == false;
		}

		if (state.getHardness(world, pos) == -1.0F) {
			return false;
		}

		return switch (state.getPistonBehavior()) {
			case BLOCK -> false;
			case DESTROY -> canBreak;
			case PUSH_ONLY -> direction == pistonDir;
			default -> state.hasBlockEntity() == false;
		};
	}

	/**
	 * Выполняет физическое перемещение блоков поршнем.
	 * При выдвижении создаёт голову поршня; при сжатии — убирает её.
	 *
	 * @param extend {@code true} — выдвижение, {@code false} — сжатие
	 * @return {@code true}, если перемещение успешно выполнено
	 */
	private boolean move(World world, BlockPos pos, Direction dir, boolean extend) {
		BlockPos frontPos = pos.offset(dir);

		if (extend == false && world.getBlockState(frontPos).isOf(Blocks.PISTON_HEAD)) {
			world.setBlockState(frontPos, Blocks.AIR.getDefaultState(), 276);
		}

		PistonHandler handler = new PistonHandler(world, pos, dir, extend);

		if (handler.calculatePush() == false) {
			return false;
		}

		Map<BlockPos, BlockState> vacatedPositions = Maps.newHashMap();
		List<BlockPos> movedPositions = handler.getMovedBlocks();
		List<BlockState> movedStates = Lists.newArrayList();

		for (BlockPos movedPos : movedPositions) {
			BlockState movedState = world.getBlockState(movedPos);
			movedStates.add(movedState);
			vacatedPositions.put(movedPos, movedState);
		}

		List<BlockPos> brokenPositions = handler.getBrokenBlocks();
		BlockState[] allAffectedStates = new BlockState[movedPositions.size() + brokenPositions.size()];
		Direction moveDir = extend ? dir : dir.getOpposite();
		int stateIndex = 0;

		for (int idx = brokenPositions.size() - 1; idx >= 0; idx--) {
			BlockPos brokenPos = brokenPositions.get(idx);
			BlockState brokenState = world.getBlockState(brokenPos);
			BlockEntity brokenEntity = brokenState.hasBlockEntity() ? world.getBlockEntity(brokenPos) : null;

			dropStacks(brokenState, world, brokenPos, brokenEntity);

			if (brokenState.isIn(BlockTags.FIRE) == false && world.isClient()) {
				world.syncWorldEvent(2001, brokenPos, getRawIdFromState(brokenState));
			}

			world.setBlockState(brokenPos, Blocks.AIR.getDefaultState(), 18);
			world.emitGameEvent(GameEvent.BLOCK_DESTROY, brokenPos, GameEvent.Emitter.of(brokenState));
			allAffectedStates[stateIndex++] = brokenState;
		}

		for (int idx = movedPositions.size() - 1; idx >= 0; idx--) {
			BlockPos sourcePos = movedPositions.get(idx);
			BlockState sourceState = world.getBlockState(sourcePos);
			BlockPos destPos = sourcePos.offset(moveDir);

			vacatedPositions.remove(destPos);

			BlockState movingState = Blocks.MOVING_PISTON.getDefaultState().with(FACING, dir);
			world.setBlockState(destPos, movingState, 324);
			world.addBlockEntity(PistonExtensionBlock.createBlockEntityPiston(
					destPos,
					movingState,
					movedStates.get(idx),
					dir,
					extend,
					false
			));
			allAffectedStates[stateIndex++] = sourceState;
		}

		if (extend) {
			PistonType pistonType = sticky ? PistonType.STICKY : PistonType.DEFAULT;
			BlockState headState = Blocks.PISTON_HEAD
					.getDefaultState()
					.with(PistonHeadBlock.FACING, dir)
					.with(PistonHeadBlock.TYPE, pistonType);
			BlockState movingHeadState = Blocks.MOVING_PISTON
					.getDefaultState()
					.with(PistonExtensionBlock.FACING, dir)
					.with(PistonExtensionBlock.TYPE, pistonType);

			vacatedPositions.remove(pos);
			world.setBlockState(pos, movingHeadState, 324);
			world.addBlockEntity(PistonExtensionBlock.createBlockEntityPiston(
					pos,
					movingHeadState,
					headState,
					dir,
					true,
					true
			));
		}

		BlockState airState = Blocks.AIR.getDefaultState();

		for (BlockPos vacated : vacatedPositions.keySet()) {
			world.setBlockState(vacated, airState, 82);
		}

		for (Entry<BlockPos, BlockState> entry : vacatedPositions.entrySet()) {
			BlockPos vacatedPos = entry.getKey();
			BlockState vacatedState = entry.getValue();
			vacatedState.prepare(world, vacatedPos, Block.NOTIFY_LISTENERS);
			airState.updateNeighbors(world, vacatedPos, Block.NOTIFY_LISTENERS);
			airState.prepare(world, vacatedPos, Block.NOTIFY_LISTENERS);
		}

		WireOrientation wireOrientation = OrientationHelper.getEmissionOrientation(world, handler.getMotionDirection(), null);
		stateIndex = 0;

		for (int idx = brokenPositions.size() - 1; idx >= 0; idx--) {
			BlockState brokenState = allAffectedStates[stateIndex++];
			BlockPos brokenPos = brokenPositions.get(idx);

			if (world instanceof ServerWorld serverWorld) {
				brokenState.onStateReplaced(serverWorld, brokenPos, false);
			}

			brokenState.prepare(world, brokenPos, Block.NOTIFY_LISTENERS);
			world.updateNeighborsAlways(brokenPos, brokenState.getBlock(), wireOrientation);
		}

		for (int idx = movedPositions.size() - 1; idx >= 0; idx--) {
			world.updateNeighborsAlways(movedPositions.get(idx), allAffectedStates[stateIndex++].getBlock(), wireOrientation);
		}

		if (extend) {
			world.updateNeighborsAlways(pos, Blocks.PISTON_HEAD, wireOrientation);
		}

		return true;
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, EXTENDED);
	}

	@Override
	protected boolean hasSidedTransparency(BlockState state) {
		return state.get(EXTENDED);
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}
}
