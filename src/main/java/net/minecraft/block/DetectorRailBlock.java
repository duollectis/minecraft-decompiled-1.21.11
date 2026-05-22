package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.CommandBlockMinecartEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Predicate;

/**
 * Рельс-детектор — выдаёт сигнал редстоуна при проезде вагонетки.
 * Мощность компаратора зависит от содержимого вагонетки с сундуком или командного блока.
 */
public class DetectorRailBlock extends AbstractRailBlock {

	public static final MapCodec<DetectorRailBlock> CODEC = createCodec(DetectorRailBlock::new);
	public static final EnumProperty<RailShape> SHAPE = Properties.STRAIGHT_RAIL_SHAPE;
	public static final BooleanProperty POWERED = Properties.POWERED;
	private static final int SCHEDULED_TICK_DELAY = 20;

	@Override
	public MapCodec<DetectorRailBlock> getCodec() {
		return CODEC;
	}

	public DetectorRailBlock(AbstractBlock.Settings settings) {
		super(true, settings);
		setDefaultState(
			stateManager.getDefaultState()
				.with(POWERED, false)
				.with(SHAPE, RailShape.NORTH_SOUTH)
				.with(WATERLOGGED, false)
		);
	}

	@Override
	protected boolean emitsRedstonePower(BlockState state) {
		return true;
	}

	@Override
	protected void onEntityCollision(
			BlockState state,
			World world,
			BlockPos pos,
			Entity entity,
			EntityCollisionHandler handler,
			boolean firstCollision
	) {
		if (world.isClient() || state.get(POWERED)) {
			return;
		}

		updatePoweredStatus(world, pos, state);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (state.get(POWERED)) {
			updatePoweredStatus(world, pos, state);
		}
	}

	@Override
	protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return state.get(POWERED) ? 15 : 0;
	}

	@Override
	protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		if (state.get(POWERED) == false) {
			return 0;
		}

		return direction == Direction.UP ? 15 : 0;
	}

	private void updatePoweredStatus(World world, BlockPos pos, BlockState state) {
		if (canPlaceAt(state, world, pos) == false) {
			return;
		}

		boolean wasPowered = state.get(POWERED);
		boolean hasCart = getCarts(world, pos, AbstractMinecartEntity.class, entity -> true).isEmpty() == false;

		if (hasCart && wasPowered == false) {
			BlockState powered = state.with(POWERED, true);
			world.setBlockState(pos, powered, Block.NOTIFY_ALL);
			updateNearbyRails(world, pos, powered, true);
			world.updateNeighbors(pos, this);
			world.updateNeighbors(pos.down(), this);
			world.scheduleBlockRerenderIfNeeded(pos, state, powered);
		}

		if (hasCart == false && wasPowered) {
			BlockState unpowered = state.with(POWERED, false);
			world.setBlockState(pos, unpowered, Block.NOTIFY_ALL);
			updateNearbyRails(world, pos, unpowered, false);
			world.updateNeighbors(pos, this);
			world.updateNeighbors(pos.down(), this);
			world.scheduleBlockRerenderIfNeeded(pos, state, unpowered);
		}

		if (hasCart) {
			world.scheduleBlockTick(pos, this, SCHEDULED_TICK_DELAY);
		}

		world.updateComparators(pos, this);
	}

	protected void updateNearbyRails(World world, BlockPos pos, BlockState state, boolean unpowering) {
		RailPlacementHelper helper = new RailPlacementHelper(world, pos, state);

		for (BlockPos neighborPos : helper.getNeighbors()) {
			BlockState neighborState = world.getBlockState(neighborPos);
			world.updateNeighbor(neighborState, neighborPos, neighborState.getBlock(), null, false);
		}
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if (oldState.isOf(state.getBlock())) {
			return;
		}

		BlockState updated = updateCurves(state, world, pos, notify);
		updatePoweredStatus(world, pos, updated);
	}

	@Override
	public Property<RailShape> getShapeProperty() {
		return SHAPE;
	}

	@Override
	protected boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
		if (state.get(POWERED) == false) {
			return 0;
		}

		List<CommandBlockMinecartEntity> commandCarts = getCarts(world, pos, CommandBlockMinecartEntity.class, cart -> true);

		if (commandCarts.isEmpty() == false) {
			return commandCarts.get(0).getCommandExecutor().getSuccessCount();
		}

		List<AbstractMinecartEntity> inventoryCarts = getCarts(world, pos, AbstractMinecartEntity.class, EntityPredicates.VALID_INVENTORIES);

		if (inventoryCarts.isEmpty() == false) {
			return ScreenHandler.calculateComparatorOutput((Inventory) inventoryCarts.get(0));
		}

		return 0;
	}

	private <T extends AbstractMinecartEntity> List<T> getCarts(
			World world,
			BlockPos pos,
			Class<T> entityClass,
			Predicate<Entity> entityPredicate
	) {
		return world.getEntitiesByClass(entityClass, getCartDetectionBox(pos), entityPredicate);
	}

	private Box getCartDetectionBox(BlockPos pos) {
		double inset = 0.2;

		return new Box(
			pos.getX() + inset,
			pos.getY(),
			pos.getZ() + inset,
			pos.getX() + 1 - inset,
			pos.getY() + 1 - inset,
			pos.getZ() + 1 - inset
		);
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(SHAPE, rotateShape(state.get(SHAPE), rotation));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.with(SHAPE, mirrorShape(state.get(SHAPE), mirror));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(SHAPE, POWERED, WATERLOGGED);
	}
}
