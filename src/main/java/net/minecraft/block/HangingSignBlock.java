package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.HangingSignItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Блок подвесной таблички. Крепится к потолку; поддерживает вращение по 16 направлениям
 * и режим ATTACHED (прикреплён к другой табличке или стене).
 */
public class HangingSignBlock extends AbstractSignBlock {

	public static final MapCodec<HangingSignBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(
							WoodType.CODEC.fieldOf("wood_type").forGetter(block -> block.getWoodType()),
							createSettingsCodec()
					)
					.apply(instance, HangingSignBlock::new)
	);
	public static final IntProperty ROTATION = Properties.ROTATION;
	public static final BooleanProperty ATTACHED = Properties.ATTACHED;
	private static final VoxelShape DEFAULT_SHAPE = Block.createColumnShape(10.0, 0.0, 16.0);
	private static final Map<Integer, VoxelShape>
			SHAPES_BY_ROTATION =
			VoxelShapes.createHorizontalFacingShapeMap(Block.createColumnShape(14.0, 2.0, 0.0, 10.0))
			           .entrySet()
			           .stream()
			           .collect(Collectors.toMap(
					           entry -> RotationPropertyHelper.fromDirection(entry.getKey()),
					           Entry::getValue
			           ));

	@Override
	public MapCodec<HangingSignBlock> getCodec() {
		return CODEC;
	}

	public HangingSignBlock(WoodType woodType, AbstractBlock.Settings settings) {
		super(woodType, settings.sounds(woodType.hangingSignSoundType()));
		setDefaultState(stateManager
				.getDefaultState()
				.with(ROTATION, 0)
				.with(ATTACHED, false)
				.with(WATERLOGGED, false));
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
		if (world.getBlockEntity(pos) instanceof SignBlockEntity sign && shouldTryAttaching(player, hit, sign, stack)) {
			return ActionResult.PASS;
		}

		return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
	}

	private boolean shouldTryAttaching(
			PlayerEntity player,
			BlockHitResult hitResult,
			SignBlockEntity sign,
			ItemStack stack
	) {
		return sign.canRunCommandClickEvent(sign.isPlayerFacingFront(player), player) == false
				&& stack.getItem() instanceof HangingSignItem
				&& hitResult.getSide().equals(Direction.DOWN);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return world.getBlockState(pos.up()).isSideSolid(world, pos.up(), Direction.DOWN, SideShapeType.CENTER);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		World world = ctx.getWorld();
		FluidState fluidState = world.getFluidState(ctx.getBlockPos());
		BlockPos above = ctx.getBlockPos().up();
		BlockState aboveState = world.getBlockState(above);
		boolean isHangingSign = aboveState.isIn(BlockTags.ALL_HANGING_SIGNS);
		Direction facing = Direction.fromHorizontalDegrees(ctx.getPlayerYaw());
		boolean attached = Block.isFaceFullSquare(aboveState.getCollisionShape(world, above), Direction.DOWN) == false
				|| ctx.shouldCancelInteraction();

		if (isHangingSign && ctx.shouldCancelInteraction() == false) {
			if (aboveState.contains(WallHangingSignBlock.FACING)) {
				Direction wallFacing = aboveState.get(WallHangingSignBlock.FACING);
				if (wallFacing.getAxis().test(facing)) {
					attached = false;
				}
			} else if (aboveState.contains(ROTATION)) {
				Optional<Direction> rotDir = RotationPropertyHelper.toDirection(aboveState.get(ROTATION));
				if (rotDir.isPresent() && rotDir.get().getAxis().test(facing)) {
					attached = false;
				}
			}
		}

		int rotation = attached
				? RotationPropertyHelper.fromYaw(ctx.getPlayerYaw() + 180.0F)
				: RotationPropertyHelper.fromDirection(facing.getOpposite());
		return getDefaultState()
				.with(ATTACHED, attached)
				.with(ROTATION, rotation)
				.with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_ROTATION.getOrDefault(state.get(ROTATION), DEFAULT_SHAPE);
	}

	@Override
	protected VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
		return getOutlineShape(state, world, pos, ShapeContext.absent());
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
		if (direction == Direction.UP && canPlaceAt(state, world, pos) == false) {
			return Blocks.AIR.getDefaultState();
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
	public float getRotationDegrees(BlockState state) {
		return RotationPropertyHelper.toDegrees(state.get(ROTATION));
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(ROTATION, rotation.rotate(state.get(ROTATION), 16));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.with(ROTATION, mirror.mirror(state.get(ROTATION), 16));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(ROTATION, ATTACHED, WATERLOGGED);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new HangingSignBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
			World world,
			BlockState state,
			BlockEntityType<T> type
	) {
		return validateTicker(type, BlockEntityType.HANGING_SIGN, SignBlockEntity::tick);
	}
}
