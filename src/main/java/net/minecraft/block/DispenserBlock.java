package net.minecraft.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.block.dispenser.EquippableDispenserBehavior;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.dispenser.ProjectileDispenserBehavior;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Диспенсер — блок, выбрасывающий предметы при получении сигнала редстоуна.
 * Поведение выброса зависит от типа предмета и регистрируется через {@link #BEHAVIORS}.
 */
public class DispenserBlock extends BlockWithEntity {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final MapCodec<DispenserBlock> CODEC = createCodec(DispenserBlock::new);
	public static final EnumProperty<Direction> FACING = FacingBlock.FACING;
	public static final BooleanProperty TRIGGERED = Properties.TRIGGERED;
	private static final ItemDispenserBehavior DEFAULT_BEHAVIOR = new ItemDispenserBehavior();
	public static final Map<Item, DispenserBehavior> BEHAVIORS = new IdentityHashMap<>();
	private static final int SCHEDULED_TICK_DELAY = 4;

	@Override
	public MapCodec<? extends DispenserBlock> getCodec() {
		return CODEC;
	}

	public static void registerBehavior(ItemConvertible provider, DispenserBehavior behavior) {
		BEHAVIORS.put(provider.asItem(), behavior);
	}

	public static void registerProjectileBehavior(ItemConvertible projectile) {
		BEHAVIORS.put(projectile.asItem(), new ProjectileDispenserBehavior(projectile.asItem()));
	}

	public DispenserBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(FACING, Direction.NORTH).with(TRIGGERED, false));
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient() == false && world.getBlockEntity(pos) instanceof DispenserBlockEntity dispenser) {
			player.openHandledScreen(dispenser);
			player.incrementStat(dispenser instanceof DropperBlockEntity ? Stats.INSPECT_DROPPER : Stats.INSPECT_DISPENSER);
		}

		return ActionResult.SUCCESS;
	}

	protected void dispense(ServerWorld world, BlockState state, BlockPos pos) {
		DispenserBlockEntity dispenser = world.getBlockEntity(pos, BlockEntityType.DISPENSER).orElse(null);

		if (dispenser == null) {
			LOGGER.warn("Ignoring dispensing attempt for Dispenser without matching block entity at {}", pos);
			return;
		}

		BlockPointer pointer = new BlockPointer(world, pos, state, dispenser);
		int slot = dispenser.chooseNonEmptySlot(world.random);

		if (slot < 0) {
			world.syncWorldEvent(1001, pos, 0);
			world.emitGameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Emitter.of(dispenser.getCachedState()));
			return;
		}

		ItemStack stack = dispenser.getStack(slot);
		DispenserBehavior behavior = getBehaviorForItem(world, stack);

		if (behavior != DispenserBehavior.NOOP) {
			dispenser.setStack(slot, behavior.dispense(pointer, stack));
		}
	}

	protected DispenserBehavior getBehaviorForItem(World world, ItemStack stack) {
		if (stack.isItemEnabled(world.getEnabledFeatures()) == false) {
			return DEFAULT_BEHAVIOR;
		}

		DispenserBehavior registered = BEHAVIORS.get(stack.getItem());

		return registered != null ? registered : getBehaviorForItem(stack);
	}

	private static DispenserBehavior getBehaviorForItem(ItemStack stack) {
		return stack.contains(DataComponentTypes.EQUIPPABLE)
			? EquippableDispenserBehavior.INSTANCE
			: DEFAULT_BEHAVIOR;
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
		boolean powered = world.isReceivingRedstonePower(pos) || world.isReceivingRedstonePower(pos.up());
		boolean wasTriggered = state.get(TRIGGERED);

		if (powered && wasTriggered == false) {
			world.scheduleBlockTick(pos, this, SCHEDULED_TICK_DELAY);
			world.setBlockState(pos, state.with(TRIGGERED, true), Block.NOTIFY_LISTENERS);
		} else if (powered == false && wasTriggered) {
			world.setBlockState(pos, state.with(TRIGGERED, false), Block.NOTIFY_LISTENERS);
		}
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		dispense(world, state, pos);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new DispenserBlockEntity(pos, state);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite());
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		ItemScatterer.onStateReplaced(state, world, pos);
	}

	public static Position getOutputLocation(BlockPointer pointer) {
		return getOutputLocation(pointer, 0.7, Vec3d.ZERO);
	}

	public static Position getOutputLocation(BlockPointer pointer, double facingOffset, Vec3d constantOffset) {
		Direction direction = pointer.state().get(FACING);
		return pointer.centerPos()
		              .add(
				              facingOffset * direction.getOffsetX() + constantOffset.getX(),
				              facingOffset * direction.getOffsetY() + constantOffset.getY(),
				              facingOffset * direction.getOffsetZ() + constantOffset.getZ()
		              );
	}

	@Override
	protected boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
		return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
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
		builder.add(FACING, TRIGGERED);
	}
}
