package net.minecraft.block.entity;

import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.List;

/**
 * Блок-сущность бочки. Поддерживает 27 слотов инвентаря, лут-таблицы и анимацию открытия крышки.
 */
public class BarrelBlockEntity extends LootableContainerBlockEntity {

	private static final int INVENTORY_SIZE = 27;
	private static final Text CONTAINER_NAME_TEXT = Text.translatable("container.barrel");
	private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
	private final ViewerCountManager stateManager = new ViewerCountManager() {
		@Override
		protected void onContainerOpen(World world, BlockPos pos, BlockState state) {
			BarrelBlockEntity.this.playSound(state, SoundEvents.BLOCK_BARREL_OPEN);
			BarrelBlockEntity.this.setOpen(state, true);
		}

		@Override
		protected void onContainerClose(World world, BlockPos pos, BlockState state) {
			BarrelBlockEntity.this.playSound(state, SoundEvents.BLOCK_BARREL_CLOSE);
			BarrelBlockEntity.this.setOpen(state, false);
		}

		@Override
		protected void onViewerCountUpdate(
				World world,
				BlockPos pos,
				BlockState state,
				int oldViewerCount,
				int newViewerCount
		) {
		}

		@Override
		public boolean isPlayerViewing(PlayerEntity player) {
			if (player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
				return handler.getInventory() == BarrelBlockEntity.this;
			}

			return false;
		}
	};

	public BarrelBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.BARREL, pos, state);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);

		if (!writeLootTable(view)) {
			Inventories.writeData(view, inventory);
		}
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
	public int size() {
		return INVENTORY_SIZE;
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
	protected Text getContainerName() {
		return CONTAINER_NAME_TEXT;
	}

	@Override
	protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this);
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

		stateManager.closeContainer(user.asLivingEntity(), getWorld(), getPos(), getCachedState());
	}

	@Override
	public List<ContainerUser> getViewingUsers() {
		return stateManager.getViewingUsers(getWorld(), getPos());
	}

	public void tick() {
		if (!removed) {
			stateManager.updateViewerCount(getWorld(), getPos(), getCachedState());
		}
	}

	void setOpen(BlockState state, boolean open) {
		world.setBlockState(getPos(), state.with(BarrelBlock.OPEN, open), 3);
	}

	void playSound(BlockState state, SoundEvent soundEvent) {
		Vec3i facingVector = state.get(BarrelBlock.FACING).getVector();
		double x = pos.getX() + 0.5 + facingVector.getX() / 2.0;
		double y = pos.getY() + 0.5 + facingVector.getY() / 2.0;
		double z = pos.getZ() + 0.5 + facingVector.getZ() / 2.0;
		world.playSound(null, x, y, z, soundEvent, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
	}
}
