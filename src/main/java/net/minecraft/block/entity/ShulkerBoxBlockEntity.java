package net.minecraft.block.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Блок-сущность ящика шалкера. Управляет анимацией открытия/закрытия крышки,
 * выталкивает сущности при открытии и хранит инвентарь из {@link #INVENTORY_SIZE} слотов.
 * Запрещает вкладывать другие ящики шалкеров внутрь себя.
 */
public class ShulkerBoxBlockEntity extends LootableContainerBlockEntity implements SidedInventory {

	public static final int COLUMNS = 9;
	public static final int ROWS = 3;
	public static final int INVENTORY_SIZE = 27;
	public static final int OPEN_SPEED = 1;
	public static final int MAX_OPEN_PROGRESS = 10;
	public static final float OPEN_THRESHOLD = 0.5F;
	public static final float OPEN_ANGLE = 270.0F;
	private static final int[] AVAILABLE_SLOTS = IntStream.range(0, INVENTORY_SIZE).toArray();
	private static final Text CONTAINER_NAME_TEXT = Text.translatable("container.shulkerBox");
	private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
	private int viewerCount;
	private ShulkerBoxBlockEntity.AnimationStage animationStage = ShulkerBoxBlockEntity.AnimationStage.CLOSED;
	private float animationProgress;
	private float lastAnimationProgress;
	private final @Nullable DyeColor cachedColor;

	public ShulkerBoxBlockEntity(@Nullable DyeColor color, BlockPos pos, BlockState state) {
		super(BlockEntityType.SHULKER_BOX, pos, state);
		this.cachedColor = color;
	}

	public ShulkerBoxBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.SHULKER_BOX, pos, state);
		this.cachedColor =
				state.getBlock() instanceof ShulkerBoxBlock shulkerBoxBlock ? shulkerBoxBlock.getColor() : null;
	}

	public static void tick(World world, BlockPos pos, BlockState state, ShulkerBoxBlockEntity blockEntity) {
		blockEntity.updateAnimation(world, pos, state);
	}

	private void updateAnimation(World world, BlockPos pos, BlockState state) {
		lastAnimationProgress = animationProgress;
		switch (animationStage) {
			case CLOSED:
				animationProgress = 0.0F;
				break;
			case OPENING:
				animationProgress += 0.1F;
				if (lastAnimationProgress == 0.0F) {
					updateNeighborStates(world, pos, state);
				}

				if (animationProgress >= 1.0F) {
					animationStage = AnimationStage.OPENED;
					animationProgress = 1.0F;
					updateNeighborStates(world, pos, state);
				}

				pushEntities(world, pos, state);
				break;
			case OPENED:
				animationProgress = 1.0F;
				break;
			case CLOSING:
				animationProgress -= 0.1F;
				if (lastAnimationProgress == 1.0F) {
					updateNeighborStates(world, pos, state);
				}

				if (animationProgress <= 0.0F) {
					animationStage = AnimationStage.CLOSED;
					animationProgress = 0.0F;
					updateNeighborStates(world, pos, state);
				}
		}
	}

	public AnimationStage getAnimationStage() {
		return animationStage;
	}

	public Box getBoundingBox(BlockState state) {
		Vec3d center = new Vec3d(0.5, 0.0, 0.5);
		return ShulkerEntity.calculateBoundingBox(
			1.0F,
			state.get(ShulkerBoxBlock.FACING),
			0.5F * getAnimationProgress(1.0F),
			center
		);
	}

	private void pushEntities(World world, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
			return;
		}

		Direction direction = state.get(ShulkerBoxBlock.FACING);
		Box box = ShulkerEntity.calculateBoundingBox(
			1.0F,
			direction,
			lastAnimationProgress,
			animationProgress,
			pos.toBottomCenterPos()
		);
		List<Entity> entities = world.getOtherEntities(null, box);

		for (Entity entity : entities) {
			if (entity.getPistonBehavior() == PistonBehavior.IGNORE) {
				continue;
			}

			entity.move(
				MovementType.SHULKER_BOX,
				new Vec3d(
					(box.getLengthX() + 0.01) * direction.getOffsetX(),
					(box.getLengthY() + 0.01) * direction.getOffsetY(),
					(box.getLengthZ() + 0.01) * direction.getOffsetZ()
				)
			);
		}
	}

	@Override
	public int size() {
		return inventory.size();
	}

	@Override
	public boolean onSyncedBlockEvent(int type, int data) {
		if (type != 1) {
			return super.onSyncedBlockEvent(type, data);
		}

		viewerCount = data;
		if (data == 0) {
			animationStage = AnimationStage.CLOSING;
		}

		if (data == 1) {
			animationStage = AnimationStage.OPENING;
		}

		return true;
	}

	private static void updateNeighborStates(World world, BlockPos pos, BlockState state) {
		state.updateNeighbors(world, pos, 3);
		world.updateNeighbors(pos, state.getBlock());
	}

	@Override
	public void onBlockReplaced(BlockPos pos, BlockState oldState) {
	}

	@Override
	public void onOpen(ContainerUser user) {
		if (removed || user.asLivingEntity().isSpectator()) {
			return;
		}

		if (viewerCount < 0) {
			viewerCount = 0;
		}

		viewerCount++;
		world.addSyncedBlockEvent(pos, getCachedState().getBlock(), 1, viewerCount);

		if (viewerCount == 1) {
			world.emitGameEvent(user.asLivingEntity(), GameEvent.CONTAINER_OPEN, pos);
			world.playSound(
				null,
				pos,
				SoundEvents.BLOCK_SHULKER_BOX_OPEN,
				SoundCategory.BLOCKS,
				0.5F,
				world.random.nextFloat() * 0.1F + 0.9F
			);
		}
	}

	@Override
	public void onClose(ContainerUser user) {
		if (removed || user.asLivingEntity().isSpectator()) {
			return;
		}

		viewerCount--;
		world.addSyncedBlockEvent(pos, getCachedState().getBlock(), 1, viewerCount);

		if (viewerCount <= 0) {
			world.emitGameEvent(user.asLivingEntity(), GameEvent.CONTAINER_CLOSE, pos);
			world.playSound(
				null,
				pos,
				SoundEvents.BLOCK_SHULKER_BOX_CLOSE,
				SoundCategory.BLOCKS,
				0.5F,
				world.random.nextFloat() * 0.1F + 0.9F
			);
		}
	}

	@Override
	protected Text getContainerName() {
		return CONTAINER_NAME_TEXT;
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		readInventoryNbt(view);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		if (!writeLootTable(view)) {
			Inventories.writeData(view, inventory, false);
		}
	}

	public void readInventoryNbt(ReadView readView) {
		inventory = DefaultedList.ofSize(size(), ItemStack.EMPTY);
		if (!readLootTable(readView)) {
			Inventories.readData(readView, inventory);
		}
	}

	@Override
	protected DefaultedList<ItemStack> getHeldStacks() {
		return inventory;
	}

	@Override
	protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
		this.inventory = inventory;
	}

	@Override
	public int[] getAvailableSlots(Direction side) {
		return AVAILABLE_SLOTS;
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
		return !(Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock);
	}

	@Override
	public boolean canExtract(int slot, ItemStack stack, Direction dir) {
		return true;
	}

	public float getAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastAnimationProgress, animationProgress);
	}

	public @Nullable DyeColor getColor() {
		return cachedColor;
	}

	@Override
	protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return new ShulkerBoxScreenHandler(syncId, playerInventory, this);
	}

	public boolean suffocates() {
		return animationStage == AnimationStage.CLOSED;
	}

	public enum AnimationStage {
		CLOSED,
		OPENING,
		OPENED,
		CLOSING;
	}
}
