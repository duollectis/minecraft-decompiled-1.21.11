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
 * {@code ChestBlockEntity}.
 */
public class ChestBlockEntity extends LootableContainerBlockEntity implements LidOpenable {

	private static final int VIEWER_COUNT_UPDATE_EVENT_TYPE = 1;
	private static final Text CONTAINER_NAME_TEXT = Text.translatable("container.chest");
	private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(27, ItemStack.EMPTY);
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
			if (!(player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
				return false;
			}
			else {
				Inventory inventory = ((GenericContainerScreenHandler) player.currentScreenHandler).getInventory();
				return inventory == ChestBlockEntity.this || inventory instanceof DoubleInventory
						&& ((DoubleInventory) inventory).isPart(ChestBlockEntity.this);
			}
		}
	};
	private final ChestLidAnimator lidAnimator = new ChestLidAnimator();

	protected ChestBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
		super(blockEntityType, blockPos, blockState);
	}

	public ChestBlockEntity(BlockPos pos, BlockState state) {
		this(BlockEntityType.CHEST, pos, state);
	}

	@Override
	public int size() {
		return 27;
	}

	@Override
	protected Text getContainerName() {
		return CONTAINER_NAME_TEXT;
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
		if (!this.readLootTable(view)) {
			Inventories.readData(view, this.inventory);
		}
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		if (!this.writeLootTable(view)) {
			Inventories.writeData(view, this.inventory);
		}
	}

	/**
	 * Client tick.
	 *
	 * @param world world
	 * @param pos pos
	 * @param state state
	 * @param blockEntity block entity
	 */
	public static void clientTick(World world, BlockPos pos, BlockState state, ChestBlockEntity blockEntity) {
		blockEntity.lidAnimator.step();
	}

	static void playSound(World world, BlockPos pos, BlockState state, SoundEvent soundEvent) {
		ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
		if (chestType != ChestType.LEFT) {
			double d = pos.getX() + 0.5;
			double e = pos.getY() + 0.5;
			double f = pos.getZ() + 0.5;
			if (chestType == ChestType.RIGHT) {
				Direction direction = ChestBlock.getFacing(state);
				d += direction.getOffsetX() * 0.5;
				f += direction.getOffsetZ() * 0.5;
			}

			world.playSound(
					null,
					d,
					e,
					f,
					soundEvent,
					SoundCategory.BLOCKS,
					0.5F,
					world.random.nextFloat() * 0.1F + 0.9F
			);
		}
	}

	@Override
	public boolean onSyncedBlockEvent(int type, int data) {
		if (type == 1) {
			this.lidAnimator.setOpen(data > 0);
			return true;
		}
		else {
			return super.onSyncedBlockEvent(type, data);
		}
	}

	@Override
	public void onOpen(ContainerUser user) {
		if (!this.removed && !user.asLivingEntity().isSpectator()) {
			this.stateManager.openContainer(
					user.asLivingEntity(),
					this.getWorld(),
					this.getPos(),
					this.getCachedState(),
					user.getContainerInteractionRange()
			);
		}
	}

	@Override
	public void onClose(ContainerUser user) {
		if (!this.removed && !user.asLivingEntity().isSpectator()) {
			this.stateManager.closeContainer(
					user.asLivingEntity(),
					this.getWorld(),
					this.getPos(),
					this.getCachedState()
			);
		}
	}

	@Override
	public List<ContainerUser> getViewingUsers() {
		return this.stateManager.getViewingUsers(this.getWorld(), this.getPos());
	}

	@Override
	protected DefaultedList<ItemStack> getHeldStacks() {
		return this.inventory;
	}

	@Override
	protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
		this.inventory = inventory;
	}

	@Override
	public float getAnimationProgress(float tickProgress) {
		return this.lidAnimator.getProgress(tickProgress);
	}

	public static int getPlayersLookingInChestCount(BlockView world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		if (blockState.hasBlockEntity()) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof ChestBlockEntity) {
				return ((ChestBlockEntity) blockEntity).stateManager.getViewerCount();
			}
		}

		return 0;
	}

	/**
	 * Создаёт копию inventory.
	 *
	 * @param from from
	 * @param to to
	 */
	public static void copyInventory(ChestBlockEntity from, ChestBlockEntity to) {
		DefaultedList<ItemStack> defaultedList = from.getHeldStacks();
		from.setHeldStacks(to.getHeldStacks());
		to.setHeldStacks(defaultedList);
	}

	@Override
	protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this);
	}

	/**
	 * Обрабатывает событие scheduled tick.
	 */
	public void onScheduledTick() {
		if (!this.removed) {
			this.stateManager.updateViewerCount(this.getWorld(), this.getPos(), this.getCachedState());
		}
	}

	protected void onViewerCountUpdate(
			World world,
			BlockPos pos,
			BlockState state,
			int oldViewerCount,
			int newViewerCount
	) {
		Block block = state.getBlock();
		world.addSyncedBlockEvent(pos, block, 1, newViewerCount);
	}
}
