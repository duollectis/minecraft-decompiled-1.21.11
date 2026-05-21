package net.minecraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.function.IntFunction;

/**
 * {@code CopperGolemStatueBlock}.
 */
public class CopperGolemStatueBlock extends BlockWithEntity implements Waterloggable {

	public static final MapCodec<CopperGolemStatueBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Oxidizable.OxidationLevel.CODEC
							                    .fieldOf("weathering_state")
							                    .forGetter(CopperGolemStatueBlock::getOxidationLevel), createSettingsCodec()
			                    )
			                    .apply(instance, CopperGolemStatueBlock::new)
	);
	public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
	public static final EnumProperty<CopperGolemStatueBlock.Pose> POSE = Properties.COPPER_GOLEM_POSE;
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	private static final VoxelShape SHAPE = Block.createColumnShape(10.0, 0.0, 14.0);
	private final Oxidizable.OxidationLevel oxidationLevel;

	@Override
	public MapCodec<? extends CopperGolemStatueBlock> getCodec() {
		return CODEC;
	}

	public CopperGolemStatueBlock(Oxidizable.OxidationLevel oxidationLevel, AbstractBlock.Settings settings) {
		super(settings);
		this.oxidationLevel = oxidationLevel;
		this.setDefaultState(this
				.getDefaultState()
				.with(FACING, Direction.NORTH)
				.with(POSE, CopperGolemStatueBlock.Pose.STANDING)
				.with(WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(FACING, POSE, WATERLOGGED);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
		return this
				.getDefaultState()
				.with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
				.with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
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
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	public Oxidizable.OxidationLevel getOxidationLevel() {
		return this.oxidationLevel;
	}

	@Override
	protected ActionResult onUseWithItem(
			ItemStack stack,
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			BlockHitResult hit
	) {
		if (stack.isIn(ItemTags.AXES)) {
			return ActionResult.PASS;
		}
		else {
			this.changePose(world, state, pos, player);
			return ActionResult.SUCCESS;
		}
	}

	void changePose(World world, BlockState state, BlockPos pos, PlayerEntity player) {
		world.playSound(null, pos, SoundEvents.ENTITY_COPPER_GOLEM_BECOME_STATUE, SoundCategory.BLOCKS);
		world.setBlockState(pos, state.with(POSE, state.get(POSE).getNext()), 3);
		world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return type == NavigationType.WATER && state.getFluidState().isIn(FluidTags.WATER);
	}

	@Override
	public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new CopperGolemStatueBlockEntity(pos, state);
	}

	@Override
	public boolean keepBlockEntityWhenReplacedWith(BlockState state) {
		return state.isIn(BlockTags.COPPER_GOLEM_STATUES);
	}

	@Override
	protected boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
		return state.get(POSE).ordinal() + 1;
	}

	@Override
	protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
		return world.getBlockEntity(pos) instanceof CopperGolemStatueBlockEntity copperGolemStatueBlockEntity
		       ? copperGolemStatueBlockEntity.withComponents(this.asItem().getDefaultStack(), state.get(POSE))
		       : super.getPickStack(world, pos, state, includeData);
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		world.updateComparators(pos, state.getBlock());
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

	/**
	 * {@code Pose}.
	 */
	public static enum Pose implements StringIdentifiable {
		STANDING("standing"),
		SITTING("sitting"),
		RUNNING("running"),
		STAR("star");

		public static final IntFunction<CopperGolemStatueBlock.Pose>
				INDEX_MAPPER =
				ValueLists.createIndexToValueFunction(
						(CopperGolemStatueBlock.Pose pose) -> pose.ordinal(),
						values(),
						ValueLists.OutOfBoundsHandling.ZERO
				);
		public static final Codec<CopperGolemStatueBlock.Pose>
				CODEC =
				StringIdentifiable.createCodec(CopperGolemStatueBlock.Pose::values);
		private final String id;

		private Pose(final String id) {
			this.id = id;
		}

		@Override
		public String asString() {
			return this.id;
		}

		public CopperGolemStatueBlock.Pose getNext() {
			return INDEX_MAPPER.apply(this.ordinal() + 1);
		}
	}
}
