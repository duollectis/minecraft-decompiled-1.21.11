package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
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
 * {@code TrapdoorBlock}.
 */
public class TrapdoorBlock extends HorizontalFacingBlock implements Waterloggable {

	public static final MapCodec<TrapdoorBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(
							BlockSetType.CODEC.fieldOf("block_set_type").forGetter(block -> block.blockSetType),
							createSettingsCodec()
					)
					.apply(instance, TrapdoorBlock::new)
	);
	public static final BooleanProperty OPEN = Properties.OPEN;
	public static final EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;
	public static final BooleanProperty POWERED = Properties.POWERED;
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	private static final Map<Direction, VoxelShape>
			shapeByDirection =
			VoxelShapes.createFacingShapeMap(Block.createCuboidZShape(16.0, 13.0, 16.0));
	private final BlockSetType blockSetType;

	@Override
	public MapCodec<? extends TrapdoorBlock> getCodec() {
		return CODEC;
	}

	public TrapdoorBlock(BlockSetType type, AbstractBlock.Settings settings) {
		super(settings.sounds(type.soundType()));
		this.blockSetType = type;
		this.setDefaultState(
				this.stateManager
						.getDefaultState()
						.with(FACING, Direction.NORTH)
						.with(OPEN, false)
						.with(HALF, BlockHalf.BOTTOM)
						.with(POWERED, false)
						.with(WATERLOGGED, false)
		);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapeByDirection.get(state.get(OPEN) ? state.get(FACING)
		                                            : (state.get(HALF) == BlockHalf.TOP ? Direction.DOWN : Direction.UP
		                            ));
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		switch (type) {
			case LAND:
				return state.get(OPEN);
			case WATER:
				return state.get(WATERLOGGED);
			case AIR:
				return state.get(OPEN);
			default:
				return false;
		}
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!this.blockSetType.canOpenByHand()) {
			return ActionResult.PASS;
		}
		else {
			this.flip(state, world, pos, player);
			return ActionResult.SUCCESS;
		}
	}

	@Override
	protected void onExploded(
			BlockState state,
			ServerWorld world,
			BlockPos pos,
			Explosion explosion,
			BiConsumer<ItemStack, BlockPos> stackMerger
	) {
		if (explosion.canTriggerBlocks() && this.blockSetType.canOpenByWindCharge() && !state.get(POWERED)) {
			this.flip(state, world, pos, null);
		}

		super.onExploded(state, world, pos, explosion, stackMerger);
	}

	private void flip(BlockState state, World world, BlockPos pos, @Nullable PlayerEntity player) {
		BlockState blockState = state.cycle(OPEN);
		world.setBlockState(pos, blockState, 2);
		if (blockState.get(WATERLOGGED)) {
			world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}

		this.playToggleSound(player, world, pos, blockState.get(OPEN));
	}

	/**
	 * Play toggle sound.
	 *
	 * @param player player
	 * @param world world
	 * @param pos pos
	 * @param open open
	 */
	protected void playToggleSound(@Nullable PlayerEntity player, World world, BlockPos pos, boolean open) {
		world.playSound(
				player,
				pos,
				open ? this.blockSetType.trapdoorOpen() : this.blockSetType.trapdoorClose(),
				SoundCategory.BLOCKS,
				1.0F,
				world.getRandom().nextFloat() * 0.1F + 0.9F
		);
		world.emitGameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
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
			boolean bl = world.isReceivingRedstonePower(pos);
			if (bl != state.get(POWERED)) {
				if (state.get(OPEN) != bl) {
					state = state.with(OPEN, bl);
					this.playToggleSound(null, world, pos, bl);
				}

				world.setBlockState(pos, state.with(POWERED, bl), 2);
				if (state.get(WATERLOGGED)) {
					world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
				}
			}
		}
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState blockState = this.getDefaultState();
		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
		Direction direction = ctx.getSide();
		if (!ctx.canReplaceExisting() && direction.getAxis().isHorizontal()) {
			blockState =
					blockState
							.with(FACING, direction)
							.with(
									HALF,
									ctx.getHitPos().y - ctx.getBlockPos().getY() > 0.5 ? BlockHalf.TOP
									                                                   : BlockHalf.BOTTOM
							);
		}
		else {
			blockState = blockState.with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
			                       .with(HALF, direction == Direction.UP ? BlockHalf.BOTTOM : BlockHalf.TOP);
		}

		if (ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos())) {
			blockState = blockState.with(OPEN, true).with(POWERED, true);
		}

		return blockState.with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN, HALF, POWERED, WATERLOGGED);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
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

	protected BlockSetType getBlockSetType() {
		return this.blockSetType;
	}
}
