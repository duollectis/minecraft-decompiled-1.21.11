package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Задача мозга, реализующая перенос предметов между сундуками в заданном радиусе.
 * Управляет тремя фазами: движение к хранилищу, ожидание в очереди, взаимодействие с инвентарём.
 */
public class MoveItemsTask extends MultiTickTask<PathAwareEntity> {

	public static final int INTERACTION_TICKS = 60;
	private static final int VISITED_POSITION_EXPIRY = 6000;
	private static final int MAX_STACK_SIZE_AT_ONCE = 16;
	private static final int VISITS_UNTIL_COOLDOWN = 10;
	private static final int MAX_UNREACHABLE_POSITIONS = 50;
	private static final int MIN_INTERACTION_DISTANCE = 1;
	private static final int COOLDOWN_EXPIRY = 140;
	private static final double QUEUING_RANGE = 3.0;
	private static final double INTERACTION_RANGE = 0.5;
	private static final double FINISHED_NAVIGATION_SIGHT_RANGE = 1.0;
	private static final double INTERACTION_SIGHT_RANGE = 2.0;
	private final float speed;
	private final int horizontalRange;
	private final int verticalRange;
	private final Predicate<BlockState> inputContainerPredicate;
	private final Predicate<BlockState> outputContainerPredicate;
	private final Predicate<MoveItemsTask.Storage> storagePredicate;
	private final Consumer<PathAwareEntity> travellingCallback;
	private final Map<MoveItemsTask.InteractionState, MoveItemsTask.InteractionCallback> interactionCallbacks;
	private MoveItemsTask.@Nullable Storage targetStorage = null;
	private MoveItemsTask.NavigationState navigationState;
	private MoveItemsTask.@Nullable InteractionState interactionState;
	private int interactionTicks;

	public MoveItemsTask(
			float speed,
			Predicate<BlockState> inputContainerPredicate,
			Predicate<BlockState> outputChestPredicate,
			int horizontalRange,
			int verticalRange,
			Map<MoveItemsTask.InteractionState, MoveItemsTask.InteractionCallback> interactionCallbacks,
			Consumer<PathAwareEntity> travellingCallback,
			Predicate<MoveItemsTask.Storage> storagePredicate
	) {
		super(
				ImmutableMap.of(
						MemoryModuleType.VISITED_BLOCK_POSITIONS,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.IS_PANICKING,
						MemoryModuleState.VALUE_ABSENT
				)
		);
		this.speed = speed;
		this.inputContainerPredicate = inputContainerPredicate;
		this.outputContainerPredicate = outputChestPredicate;
		this.horizontalRange = horizontalRange;
		this.verticalRange = verticalRange;
		this.travellingCallback = travellingCallback;
		this.storagePredicate = storagePredicate;
		this.interactionCallbacks = interactionCallbacks;
		this.navigationState = MoveItemsTask.NavigationState.TRAVELLING;
	}

	@Override
	protected void run(ServerWorld world, PathAwareEntity entity, long time) {
		if (entity.getNavigation() instanceof MobNavigation mobNavigation) {
			mobNavigation.setSkipRetarget(true);
		}
	}

	@Override
	protected boolean shouldRun(ServerWorld world, PathAwareEntity entity) {
		return !entity.isLeashed();
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, PathAwareEntity entity, long time) {
		return entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS).isEmpty()
				&& !entity.isPanicking()
				&& !entity.isLeashed();
	}

	@Override
	protected boolean isTimeLimitExceeded(long time) {
		return false;
	}

	@Override
	protected void keepRunning(ServerWorld world, PathAwareEntity entity, long time) {
		boolean storageRefreshed = tick(world, entity);

		if (targetStorage == null) {
			finishRunning(world, entity, time);
			return;
		}

		if (storageRefreshed) {
			return;
		}

		if (navigationState.equals(NavigationState.QUEUING)) {
			tickQueuing(targetStorage, world, entity);
		}

		if (navigationState.equals(NavigationState.TRAVELLING)) {
			tickTravelling(targetStorage, world, entity);
		}

		if (navigationState.equals(NavigationState.INTERACTING)) {
			tickInteracting(targetStorage, world, entity);
		}
	}

	private boolean tick(ServerWorld world, PathAwareEntity entity) {
		if (hasValidTargetStorage(world, entity)) {
			return false;
		}

		invalidateTargetStorage(entity);
		Optional<Storage> found = findStorage(world, entity);

		if (found.isPresent()) {
			targetStorage = found.get();
			transitionToTravelling(entity);
			markVisited(entity, world, targetStorage.pos);
		} else {
			cooldown(entity);
		}

		return true;
	}

	private void tickQueuing(MoveItemsTask.Storage storage, World world, PathAwareEntity entity) {
		if (!this.matchesStoragePredicate(storage, world)) {
			this.onCannotUseStorage(entity);
		}
	}

	protected void tickTravelling(Storage storage, World world, PathAwareEntity entity) {
		Vec3d centerY = atCenterY(entity);

		if (isWithinRange(QUEUING_RANGE, storage, world, entity, centerY) && matchesStoragePredicate(storage, world)) {
			transitionToQueuing(entity);
		} else if (isWithinRange(getSightRange(entity), storage, world, entity, centerY)) {
			transitionToInteracting(storage, entity);
		} else {
			walkTowardsTargetStorage(entity);
		}
	}

	private Vec3d atCenterY(PathAwareEntity entity) {
		return this.atCenterY(entity, entity.getEntityPos());
	}

	protected void tickInteracting(Storage storage, World world, PathAwareEntity entity) {
		if (!isWithinRange(INTERACTION_SIGHT_RANGE, storage, world, entity, atCenterY(entity))) {
			transitionToTravelling(entity);
			return;
		}

		interactionTicks++;
		setLookTarget(storage, entity);

		if (interactionTicks >= INTERACTION_TICKS) {
			selectInteractionState(
					entity,
					storage.inventory,
					this::takeStack,
					(e, inv) -> invalidateTargetStorage(entity),
					this::placeStack,
					(e, inv) -> invalidateTargetStorage(entity)
			);
			transitionToTravelling(entity);
		}
	}

	private void transitionToQueuing(PathAwareEntity entity) {
		this.resetNavigation(entity);
		this.setNavigationState(MoveItemsTask.NavigationState.QUEUING);
	}

	private void onCannotUseStorage(PathAwareEntity entity) {
		this.setNavigationState(MoveItemsTask.NavigationState.TRAVELLING);
		this.walkTowardsTargetStorage(entity);
	}

	private void walkTowardsTargetStorage(PathAwareEntity entity) {
		if (targetStorage != null) {
			TargetUtil.walkTowards(entity, targetStorage.pos, speed, 0);
		}
	}

	private void transitionToInteracting(Storage storage, PathAwareEntity entity) {
		selectInteractionState(
				entity,
				storage.inventory,
				createSetInteractionStateCallback(InteractionState.PICKUP_ITEM),
				createSetInteractionStateCallback(InteractionState.PICKUP_NO_ITEM),
				createSetInteractionStateCallback(InteractionState.PLACE_ITEM),
				createSetInteractionStateCallback(InteractionState.PLACE_NO_ITEM)
		);
		setNavigationState(NavigationState.INTERACTING);
	}

	private void transitionToTravelling(PathAwareEntity entity) {
		travellingCallback.accept(entity);
		setNavigationState(NavigationState.TRAVELLING);
		interactionState = null;
		interactionTicks = 0;
	}

	private BiConsumer<PathAwareEntity, Inventory> createSetInteractionStateCallback(InteractionState state) {
		return (entity, inventory) -> setInteractionState(state);
	}

	private void setNavigationState(NavigationState state) {
		navigationState = state;
	}

	private void setInteractionState(InteractionState state) {
		interactionState = state;
	}

	private void setLookTarget(Storage storage, PathAwareEntity entity) {
		entity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new BlockPosLookTarget(storage.pos));
		resetNavigation(entity);

		if (interactionState != null) {
			Optional.ofNullable(interactionCallbacks.get(interactionState))
			        .ifPresent(consumer -> consumer.accept(entity, storage, interactionTicks));
		}
	}

	private void selectInteractionState(
			PathAwareEntity entity,
			Inventory inventory,
			BiConsumer<PathAwareEntity, Inventory> pickupItemCallback,
			BiConsumer<PathAwareEntity, Inventory> pickupNoItemCallback,
			BiConsumer<PathAwareEntity, Inventory> placeItemCallback,
			BiConsumer<PathAwareEntity, Inventory> placeNoItemCallback
	) {
		if (canPickUpItem(entity)) {
			if (hasItem(inventory)) {
				pickupItemCallback.accept(entity, inventory);
			}
			else {
				pickupNoItemCallback.accept(entity, inventory);
			}
		}
		else if (canInsert(entity, inventory)) {
			placeItemCallback.accept(entity, inventory);
		}
		else {
			placeNoItemCallback.accept(entity, inventory);
		}
	}

	private Optional<Storage> findStorage(ServerWorld world, PathAwareEntity entity) {
		Box searchBox = getSearchBoundingBox(entity);
		Set<GlobalPos> visited = getVisitedPositions(entity);
		Set<GlobalPos> unreachable = getUnreachablePositions(entity);
		List<ChunkPos> chunks = ChunkPos.stream(
				new ChunkPos(entity.getBlockPos()),
				Math.floorDiv(getHorizontalRange(entity), MAX_STACK_SIZE_AT_ONCE) + 1
		).toList();

		Storage nearest = null;
		double nearestDistSq = Float.MAX_VALUE;

		for (ChunkPos chunkPos : chunks) {
			WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z);

			if (chunk == null) {
				continue;
			}

			for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
				if (!(blockEntity instanceof ChestBlockEntity)) {
					continue;
				}

				double distSq = blockEntity.getPos().getSquaredDistance(entity.getEntityPos());

				if (distSq >= nearestDistSq) {
					continue;
				}

				Storage candidate = getStorageFor(entity, world, blockEntity, visited, unreachable, searchBox);

				if (candidate != null) {
					nearest = candidate;
					nearestDistSq = distSq;
				}
			}
		}

		return nearest == null ? Optional.empty() : Optional.of(nearest);
	}

	private @Nullable Storage getStorageFor(
			PathAwareEntity entity,
			World world,
			BlockEntity blockEntity,
			Set<GlobalPos> visitedPositions,
			Set<GlobalPos> unreachablePositions,
			Box box
	) {
		BlockPos pos = blockEntity.getPos();

		if (!box.contains(pos.getX(), pos.getY(), pos.getZ())) {
			return null;
		}

		Storage storage = Storage.forContainer(blockEntity, world);

		if (storage == null) {
			return null;
		}

		boolean usable = testContainer(entity, storage.state)
				&& !hasVisited(visitedPositions, unreachablePositions, storage, world)
				&& !isLocked(storage);

		return usable ? storage : null;
	}

	private boolean isLocked(Storage storage) {
		return storage.blockEntity instanceof LockableContainerBlockEntity lockable && lockable.isLocked();
	}

	private boolean hasValidTargetStorage(World world, PathAwareEntity entity) {
		boolean valid = targetStorage != null
				&& testContainer(entity, targetStorage.state)
				&& isUnchanged(world, targetStorage);

		if (!valid || isChestBlocked(world, targetStorage)) {
			return false;
		}

		if (!navigationState.equals(NavigationState.TRAVELLING)) {
			return true;
		}

		if (canNavigateTo(world, targetStorage, entity)) {
			return true;
		}

		markUnreachable(entity, world, targetStorage.pos);
		return false;
	}

	private boolean canNavigateTo(World world, Storage storage, PathAwareEntity entity) {
		Path path = entity.getNavigation().getCurrentPath() == null
				? entity.getNavigation().findPathTo(storage.pos, 0)
				: entity.getNavigation().getCurrentPath();
		Vec3d targetPos = getTargetPos(path, entity);
		boolean withinSight = isWithinRange(getSightRange(entity), storage, world, entity, targetPos);
		boolean noPathAndNotVisible = path == null && !withinSight;
		return noPathAndNotVisible || isVisible(world, withinSight, targetPos, storage, entity);
	}

	private Vec3d getTargetPos(@Nullable Path path, PathAwareEntity entity) {
		boolean noPath = path == null || path.getEnd() == null;
		Vec3d pos = noPath ? entity.getEntityPos() : path.getEnd().getBlockPos().toBottomCenterPos();
		return atCenterY(entity, pos);
	}

	private Vec3d atCenterY(PathAwareEntity entity, Vec3d pos) {
		return pos.add(0.0, entity.getBoundingBox().getLengthY() / 2.0, 0.0);
	}

	private boolean isChestBlocked(World world, MoveItemsTask.Storage storage) {
		return ChestBlock.isChestBlocked(world, storage.pos);
	}

	private boolean isUnchanged(World world, MoveItemsTask.Storage storage) {
		return storage.blockEntity.equals(world.getBlockEntity(storage.pos));
	}

	private Stream<Storage> getContainerStorages(Storage storage, World world) {
		if (storage.state.get(ChestBlock.CHEST_TYPE, ChestType.SINGLE) == ChestType.SINGLE) {
			return Stream.of(storage);
		}

		Storage adjacent = Storage.forContainer(ChestBlock.getPosInFrontOf(storage.pos, storage.state), world);
		return adjacent != null ? Stream.of(storage, adjacent) : Stream.of(storage);
	}

	private Box getSearchBoundingBox(PathAwareEntity entity) {
		int i = this.getHorizontalRange(entity);
		return new Box(entity.getBlockPos()).expand(i, this.getVerticalRange(entity), i);
	}

	private int getHorizontalRange(PathAwareEntity entity) {
		return entity.hasVehicle() ? 1 : this.horizontalRange;
	}

	private int getVerticalRange(PathAwareEntity entity) {
		return entity.hasVehicle() ? 1 : this.verticalRange;
	}

	private static Set<GlobalPos> getVisitedPositions(PathAwareEntity entity) {
		return entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS).orElse(Set.of());
	}

	private static Set<GlobalPos> getUnreachablePositions(PathAwareEntity entity) {
		return entity
				.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS)
				.orElse(Set.of());
	}

	private boolean hasVisited(
			Set<GlobalPos> visitedPositions,
			Set<GlobalPos> checkedPositions,
			MoveItemsTask.Storage storage,
			World visited
	) {
		return this.getContainerStorages(storage, visited)
		           .map(checkedStorage -> new GlobalPos(visited.getRegistryKey(), checkedStorage.pos))
		           .anyMatch(pos -> visitedPositions.contains(pos) || checkedPositions.contains(pos));
	}

	private static boolean hasFinishedNavigation(PathAwareEntity entity) {
		return entity.getNavigation().getCurrentPath() != null && entity.getNavigation().getCurrentPath().isFinished();
	}

	protected void markVisited(PathAwareEntity entity, World world, BlockPos pos) {
		Set<GlobalPos> visited = new HashSet<>(getVisitedPositions(entity));
		visited.add(new GlobalPos(world.getRegistryKey(), pos));

		if (visited.size() > VISITS_UNTIL_COOLDOWN) {
			cooldown(entity);
		} else {
			entity.getBrain().remember(MemoryModuleType.VISITED_BLOCK_POSITIONS, visited, VISITED_POSITION_EXPIRY);
		}
	}

	protected void markUnreachable(PathAwareEntity entity, World world, BlockPos pos) {
		GlobalPos globalPos = new GlobalPos(world.getRegistryKey(), pos);
		Set<GlobalPos> visited = new HashSet<>(getVisitedPositions(entity));
		visited.remove(globalPos);
		Set<GlobalPos> unreachable = new HashSet<>(getUnreachablePositions(entity));
		unreachable.add(globalPos);

		if (unreachable.size() > MAX_UNREACHABLE_POSITIONS) {
			cooldown(entity);
		} else {
			entity.getBrain().remember(MemoryModuleType.VISITED_BLOCK_POSITIONS, visited, VISITED_POSITION_EXPIRY);
			entity.getBrain().remember(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS, unreachable, VISITED_POSITION_EXPIRY);
		}
	}

	private boolean testContainer(PathAwareEntity entity, BlockState state) {
		return canPickUpItem(entity) ? this.inputContainerPredicate.test(state)
		                             : this.outputContainerPredicate.test(state);
	}

	private static double getSightRange(PathAwareEntity entity) {
		return hasFinishedNavigation(entity) ? 1.0 : 0.5;
	}

	private boolean isWithinRange(
			double range,
			MoveItemsTask.Storage storage,
			World world,
			PathAwareEntity entity,
			Vec3d pos
	) {
		Box box = entity.getBoundingBox();
		Box box2 = Box.of(pos, box.getLengthX(), box.getLengthY(), box.getLengthZ());
		return storage.state
				.getCollisionShape(world, storage.pos)
				.getBoundingBox()
				.expand(range, 0.5, range)
				.offset(storage.pos)
				.intersects(box2);
	}

	private boolean isVisible(
			World world,
			boolean nextToStorage,
			Vec3d pos,
			MoveItemsTask.Storage storage,
			PathAwareEntity entity
	) {
		return nextToStorage && this.isVisible(storage, world, entity, pos);
	}

	private boolean isVisible(MoveItemsTask.Storage storage, World world, PathAwareEntity entity, Vec3d pos) {
		Vec3d vec3d = storage.pos.toCenterPos();
		return Direction.stream()
		                .map(direction -> vec3d.add(
				                0.5 * direction.getOffsetX(),
				                0.5 * direction.getOffsetY(),
				                0.5 * direction.getOffsetZ()
		                ))
		                .map(storagePos -> world.raycast(new RaycastContext(
				                pos,
				                storagePos,
				                RaycastContext.ShapeType.COLLIDER,
				                RaycastContext.FluidHandling.NONE,
				                entity
		                )))
		                .anyMatch(hitResult -> hitResult.getType() == HitResult.Type.BLOCK && hitResult
				                .getBlockPos()
				                .equals(storage.pos));
	}

	private boolean matchesStoragePredicate(MoveItemsTask.Storage storage, World world) {
		return this.getContainerStorages(storage, world).anyMatch(this.storagePredicate);
	}

	private static boolean canPickUpItem(PathAwareEntity entity) {
		return entity.getMainHandStack().isEmpty();
	}

	private static boolean hasItem(Inventory inventory) {
		return !inventory.isEmpty();
	}

	private static boolean canInsert(PathAwareEntity entity, Inventory inventory) {
		return inventory.isEmpty() || hasExistingStack(entity, inventory);
	}

	private static boolean hasExistingStack(PathAwareEntity entity, Inventory inventory) {
		ItemStack itemStack = entity.getMainHandStack();

		for (ItemStack itemStack2 : inventory) {
			if (ItemStack.areItemsEqual(itemStack2, itemStack)) {
				return true;
			}
		}

		return false;
	}

	private void takeStack(PathAwareEntity entity, Inventory inventory) {
		entity.equipStack(EquipmentSlot.MAINHAND, extractStack(inventory));
		entity.setDropGuaranteed(EquipmentSlot.MAINHAND);
		inventory.markDirty();
		this.resetVisitedPositions(entity);
	}

	private void placeStack(PathAwareEntity entity, Inventory inventory) {
		ItemStack itemStack = insertStack(entity, inventory);
		inventory.markDirty();
		entity.equipStack(EquipmentSlot.MAINHAND, itemStack);
		if (itemStack.isEmpty()) {
			this.resetVisitedPositions(entity);
		}
		else {
			this.invalidateTargetStorage(entity);
		}
	}

	private static ItemStack extractStack(Inventory inventory) {
		int slot = 0;

		for (ItemStack stack : inventory) {
			if (!stack.isEmpty()) {
				int amount = Math.min(stack.getCount(), MAX_STACK_SIZE_AT_ONCE);
				return inventory.removeStack(slot, amount);
			}

			slot++;
		}

		return ItemStack.EMPTY;
	}

	private static ItemStack insertStack(PathAwareEntity entity, Inventory inventory) {
		int slot = 0;
		ItemStack held = entity.getMainHandStack();

		for (ItemStack existing : inventory) {
			if (existing.isEmpty()) {
				inventory.setStack(slot, held);
				return ItemStack.EMPTY;
			}

			if (ItemStack.areItemsAndComponentsEqual(existing, held) && existing.getCount() < existing.getMaxCount()) {
				int space = existing.getMaxCount() - existing.getCount();
				int toInsert = Math.min(space, held.getCount());
				existing.setCount(existing.getCount() + toInsert);
				held.setCount(held.getCount() - space);
				inventory.setStack(slot, existing);

				if (held.isEmpty()) {
					return ItemStack.EMPTY;
				}
			}

			slot++;
		}

		return held;
	}

	protected void invalidateTargetStorage(PathAwareEntity entity) {
		interactionTicks = 0;
		targetStorage = null;
		entity.getNavigation().stop();
		entity.getBrain().forget(MemoryModuleType.WALK_TARGET);
	}

	protected void resetVisitedPositions(PathAwareEntity entity) {
		invalidateTargetStorage(entity);
		entity.getBrain().forget(MemoryModuleType.VISITED_BLOCK_POSITIONS);
		entity.getBrain().forget(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
	}

	private void cooldown(PathAwareEntity entity) {
		invalidateTargetStorage(entity);
		entity.getBrain().remember(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, COOLDOWN_EXPIRY);
		entity.getBrain().forget(MemoryModuleType.VISITED_BLOCK_POSITIONS);
		entity.getBrain().forget(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
	}

	@Override
	protected void finishRunning(ServerWorld world, PathAwareEntity entity, long time) {
		transitionToTravelling(entity);

		if (entity.getNavigation() instanceof MobNavigation mobNavigation) {
			mobNavigation.setSkipRetarget(false);
		}
	}

	private void resetNavigation(PathAwareEntity entity) {
		entity.getNavigation().stop();
		entity.setSidewaysSpeed(0.0F);
		entity.setUpwardSpeed(0.0F);
		entity.setMovementSpeed(0.0F);
		entity.setVelocity(0.0, entity.getVelocity().y, 0.0);
	}

	@FunctionalInterface
	public interface InteractionCallback extends TriConsumer<PathAwareEntity, MoveItemsTask.Storage, Integer> {
	}

	public enum InteractionState {
		PICKUP_ITEM,
		PICKUP_NO_ITEM,
		PLACE_ITEM,
		PLACE_NO_ITEM
	}

	public enum NavigationState {
		TRAVELLING,
		QUEUING,
		INTERACTING
	}

	public record Storage(BlockPos pos, Inventory inventory, BlockEntity blockEntity, BlockState state) {

		public static MoveItemsTask.@Nullable Storage forContainer(BlockEntity blockEntity, World world) {
			BlockPos blockPos = blockEntity.getPos();
			BlockState blockState = blockEntity.getCachedState();
			Inventory inv = getInventory(blockEntity, blockState, world, blockPos);
			return inv != null ? new MoveItemsTask.Storage(blockPos, inv, blockEntity, blockState) : null;
		}

		public static MoveItemsTask.@Nullable Storage forContainer(BlockPos pos, World world) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			return blockEntity == null ? null : forContainer(blockEntity, world);
		}

		private static @Nullable Inventory getInventory(
				BlockEntity blockEntity,
				BlockState state,
				World world,
				BlockPos pos
		) {
			if (state.getBlock() instanceof ChestBlock chestBlock) {
				return ChestBlock.getInventory(chestBlock, state, world, pos, false);
			}

			return blockEntity instanceof Inventory inventory ? inventory : null;
		}
	}
}
