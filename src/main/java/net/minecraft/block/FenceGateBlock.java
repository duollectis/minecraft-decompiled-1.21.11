package net.minecraft.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Util;
import net.minecraft.util.function.BooleanBiFunction;
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
 * Блок калитки (ворот) для заборов. Поддерживает открытие/закрытие игроком и редстоун-сигналом,
 * а также автоматически опускается при установке рядом со стеной (IN_WALL).
 */
public class FenceGateBlock extends HorizontalFacingBlock {

	public static final MapCodec<FenceGateBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(WoodType.CODEC.fieldOf("wood_type").forGetter(block -> block.type), createSettingsCodec())
					.apply(instance, FenceGateBlock::new)
	);
	public static final BooleanProperty OPEN = Properties.OPEN;
	public static final BooleanProperty POWERED = Properties.POWERED;
	public static final BooleanProperty IN_WALL = Properties.IN_WALL;
	private static final Map<Direction.Axis, VoxelShape>
			REGULAR_OUTLINE_SHAPES =
			VoxelShapes.createHorizontalAxisShapeMap(
					Block.createCuboidShape(16.0, 16.0, 4.0)
			);
	private static final Map<Direction.Axis, VoxelShape> IN_WALL_OUTLINE_SHAPES = Maps.newEnumMap(
			Util.transformMapValues(
					REGULAR_OUTLINE_SHAPES,
					shape -> VoxelShapes.combineAndSimplify(
							shape,
							Block.createColumnShape(16.0, 13.0, 16.0),
							BooleanBiFunction.ONLY_FIRST
					)
			)
	);
	private static final Map<Direction.Axis, VoxelShape>
			CLOSED_COLLISION_SHAPES =
			VoxelShapes.createHorizontalAxisShapeMap(
					Block.createColumnShape(16.0, 4.0, 0.0, 24.0)
			);
	private static final Map<Direction.Axis, VoxelShape> CLOSED_SIDES_SHAPES = VoxelShapes.createHorizontalAxisShapeMap(
			Block.createColumnShape(16.0, 4.0, 5.0, 24.0)
	);
	private static final Map<Direction.Axis, VoxelShape>
			REGULAR_CULLING_SHAPES =
			VoxelShapes.createHorizontalAxisShapeMap(
					VoxelShapes.union(
							Block.createCuboidShape(0.0, 5.0, 7.0, 2.0, 16.0, 9.0),
							Block.createCuboidShape(14.0, 5.0, 7.0, 16.0, 16.0, 9.0)
					)
			);
	private static final Map<Direction.Axis, VoxelShape> IN_WALL_CULLING_SHAPES = Maps.newEnumMap(
			Util.transformMapValues(REGULAR_CULLING_SHAPES, shape -> shape.offset(0.0, -0.1875, 0.0).simplify())
	);
	private final WoodType type;

	@Override
	public MapCodec<FenceGateBlock> getCodec() {
		return CODEC;
	}

	public FenceGateBlock(WoodType type, AbstractBlock.Settings settings) {
		super(settings.sounds(type.soundType()));
		this.type = type;
		this.setDefaultState(this.stateManager
				.getDefaultState()
				.with(OPEN, false)
				.with(POWERED, false)
				.with(IN_WALL, false));
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		Direction.Axis axis = state.get(FACING).getAxis();
		return (state.get(IN_WALL) ? IN_WALL_OUTLINE_SHAPES : REGULAR_OUTLINE_SHAPES).get(axis);
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
		Direction.Axis axis = direction.getAxis();
		if (state.get(FACING).rotateYClockwise().getAxis() != axis) {
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

		boolean inWall = isWall(neighborState) || isWall(world.getBlockState(pos.offset(direction.getOpposite())));
		return state.with(IN_WALL, inWall);
	}

	@Override
	protected VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
		Direction.Axis axis = state.get(FACING).getAxis();
		return state.get(OPEN) ? VoxelShapes.empty() : CLOSED_SIDES_SHAPES.get(axis);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		Direction.Axis axis = state.get(FACING).getAxis();
		return state.get(OPEN) ? VoxelShapes.empty() : CLOSED_COLLISION_SHAPES.get(axis);
	}

	@Override
	protected VoxelShape getCullingShape(BlockState state) {
		Direction.Axis axis = state.get(FACING).getAxis();
		return (state.get(IN_WALL) ? IN_WALL_CULLING_SHAPES : REGULAR_CULLING_SHAPES).get(axis);
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return switch (type) {
			case LAND, AIR -> state.get(OPEN);
			case WATER -> false;
		};
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		World world = ctx.getWorld();
		BlockPos pos = ctx.getBlockPos();
		boolean powered = world.isReceivingRedstonePower(pos);
		Direction direction = ctx.getHorizontalPlayerFacing();
		Direction.Axis axis = direction.getAxis();
		boolean inWall = axis == Direction.Axis.Z
				&& (isWall(world.getBlockState(pos.west())) || isWall(world.getBlockState(pos.east())))
				|| axis == Direction.Axis.X
				&& (isWall(world.getBlockState(pos.north())) || isWall(world.getBlockState(pos.south())));
		return getDefaultState()
				.with(FACING, direction)
				.with(OPEN, powered)
				.with(POWERED, powered)
				.with(IN_WALL, inWall);
	}

	private boolean isWall(BlockState state) {
		return state.isIn(BlockTags.WALLS);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (state.get(OPEN)) {
			state = state.with(OPEN, false);
		} else {
			Direction direction = player.getHorizontalFacing();
			if (state.get(FACING) == direction.getOpposite()) {
				state = state.with(FACING, direction);
			}

			state = state.with(OPEN, true);
		}

		world.setBlockState(pos, state, 10);

		boolean open = state.get(OPEN);
		world.playSound(
				player,
				pos,
				open ? type.fenceGateOpen() : type.fenceGateClose(),
				SoundCategory.BLOCKS,
				1.0F,
				world.getRandom().nextFloat() * 0.1F + 0.9F
		);
		world.emitGameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
		return ActionResult.SUCCESS;
	}

	@Override
	protected void onExploded(
			BlockState state,
			ServerWorld world,
			BlockPos pos,
			Explosion explosion,
			BiConsumer<ItemStack, BlockPos> stackMerger
	) {
		if (explosion.canTriggerBlocks() && state.get(POWERED) == false) {
			boolean open = state.get(OPEN);
			world.setBlockState(pos, state.with(OPEN, open == false));
			world.playSound(
					null,
					pos,
					open ? type.fenceGateClose() : type.fenceGateOpen(),
					SoundCategory.BLOCKS,
					1.0F,
					world.getRandom().nextFloat() * 0.1F + 0.9F
			);
			world.emitGameEvent(open ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, pos, GameEvent.Emitter.of(state));
		}

		super.onExploded(state, world, pos, explosion, stackMerger);
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
		if (world.isClient()) {
			return;
		}

		boolean powered = world.isReceivingRedstonePower(pos);
		if (state.get(POWERED) == powered) {
			return;
		}

		world.setBlockState(pos, state.with(POWERED, powered).with(OPEN, powered), Block.NOTIFY_LISTENERS);

		if (state.get(OPEN) != powered) {
			world.playSound(
					null,
					pos,
					powered ? type.fenceGateOpen() : type.fenceGateClose(),
					SoundCategory.BLOCKS,
					1.0F,
					world.getRandom().nextFloat() * 0.1F + 0.9F
			);
			world.emitGameEvent(null, powered ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
		}
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN, POWERED, IN_WALL);
	}

	public static boolean canWallConnect(BlockState state, Direction side) {
		return state.get(FACING).getAxis() == side.rotateYClockwise().getAxis();
	}
}
