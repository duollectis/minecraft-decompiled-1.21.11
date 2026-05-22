package net.minecraft.block.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.List;

/**
 * Блок-сущность сундука. Управляет инвентарём, анимацией крышки и
 * синхронизацией количества просматривающих игроков через {@link ViewerCountManager}.
 */
public class ChestBlockEntity extends LootableContainerBlockEntity implements LidOpenable {

	private static final int INVENTORY_SIZE = 27;
	private static final int VIEWER_COUNT_UPDATE_EVENT_TYPE = 1;
	private static final Text CONTAINER_NAME_TEXT = Text.translatable("container.chest");

	private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
	private final ChestLidAnimator lidAnimator = new ChestLidAnimator();
	private final ViewerCountManager stateManager = new ViewerCountManager() {
		@Override
		protected void onContainerOpen(World world, BlockPos pos, BlockState state) {
			if (state.getBlock() instanceof ChestBlock chestBlock) {
				ChestBlockEntity.playSound(world, pos, state, chestBlock.getOpenSound());
			}
		}

		@Override
		protected void onContainerClose(World world, BlockPos pos, BlockState state) {
			if (state.getBlock() instanceof ChestBlock chestBlock) {
				ChestBlockEntity.playSound(world, pos, state, chestBlock.getCloseSound());
			}
		}

		@Override
		protected void onViewerCountUpdate(
				World world,
				BlockPos pos,
				BlockState state,
				int oldViewerCount,
				int newViewerCount
		) {
			ChestBlockEntity.this.onViewerCountUpdate(world, pos, state, oldViewerCount, newViewerCount);
		}

		@Override
		public boolean isPlayerViewing(PlayerEntity player) {
			if (player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
				Inventory inv = handler.getInventory();
				return inv == ChestBlockEntity.this
						|| inv instanceof DoubleInventory doubleInv && doubleInv.isPart(ChestBlockEntity.this);
			}

			return false;
		}
	};

	protected ChestBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
		super(blockEntityType, blockPos, blockState);
	}

	public ChestBlockEntity(BlockPos pos, BlockState state) {
		this(BlockEntityType.CHEST, pos, state);
	}

	@Override
	public int size() {
		return INVENTORY_SIZE;
	}

	@Override
	protected Text getContainerName() {
		return CONTAINER_NAME_TEXT;
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		inventory = DefaultedList.ofSize(size(), ItemStack.EMPTY);
		if (!readLootTable(view)) {
			Inventories.readData(view, inventory);
		}
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		if (!writeLootTable(view)) {
			Inventories.writeData(view, inventory);
		}
	}

	public static void clientTick(World world, BlockPos pos, BlockState state, ChestBlockEntity blockEntity) {
		blockEntity.lidAnimator.step();
	}

	static void playSound(World world, BlockPos pos, BlockState state, SoundEvent soundEvent) {
		ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
		if (chestType == ChestType.LEFT) {
			return;
		}

		double centerX = pos.getX() + 0.5;
		double centerY = pos.getY() + 0.5;
		double centerZ = pos.getZ() + 0.5;

		if (chestType == ChestType.RIGHT) {
			Direction direction = ChestBlock.getFacing(state);
			centerX += direction.getOffsetX() * 0.5;
			centerZ += direction.getOffsetZ() * 0.5;
		}

		world.playSound(
				null,
				centerX,
				centerY,
				centerZ,
				soundEvent,
				SoundCategory.BLOCKS,
				0.5F,
				world.random.nextFloat() * 0.1F + 0.9F
		);
	}

	@Override
	public boolean onSyncedBlockEvent(int type, int data) {
		if (type == VIEWER_COUNT_UPDATE_EVENT_TYPE) {
			lidAnimator.setOpen(data > 0);
			return true;
		}

		return super.onSyncedBlockEvent(type, data);
	}

	@Override
	public void onOpen(ContainerUser user) {
		if (removed || user.asLivingEntity().isSpectator()) {
			return;
		}

		stateManager.openContainer(
				user.asLivingEntity(),
				getWorld(),
				getPos(),
				getCachedState(),
				user.getContainerInteractionRange()
		);
	}

	@Override
	public void onClose(ContainerUser user) {
		if (removed || user.asLivingEntity().isSpectator()) {
			return;
		}

		stateManager.closeContainer(
				user.asLivingEntity(),
				getWorld(),
				getPos(),
				getCachedState()
		);
	}

	@Override
	public List<ContainerUser> getViewingUsers() {
		return stateManager.getViewingUsers(getWorld(), getPos());
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
	public float getAnimationProgress(float tickProgress) {
		return lidAnimator.getProgress(tickProgress);
	}

	public static int getPlayersLookingInChestCount(BlockView world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		if (blockState.hasBlockEntity() && world.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
			return chest.stateManager.getViewerCount();
		}

		return 0;
	}

	public static void copyInventory(ChestBlockEntity from, ChestBlockEntity to) {
		DefaultedList<ItemStack> fromStacks = from.getHeldStacks();
		from.setHeldStacks(to.getHeldStacks());
		to.setHeldStacks(fromStacks);
	}

	@Override
	protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this);
	}

	public void onScheduledTick() {
		if (!removed) {
			stateManager.updateViewerCount(getWorld(), getPos(), getCachedState());
		}
	}

	protected void onViewerCountUpdate(
			World world,
			BlockPos pos,
			BlockState state,
			int oldViewerCount,
			int newViewerCount
	) {
		world.addSyncedBlockEvent(pos, state.getBlock(), VIEWER_COUNT_UPDATE_EVENT_TYPE, newViewerCount);
	}
}
