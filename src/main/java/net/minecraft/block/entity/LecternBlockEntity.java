package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Clearable;
import net.minecraft.util.math.*;
import org.jspecify.annotations.Nullable;

/**
 * Блок-сущность кафедры. Хранит книгу, управляет текущей страницей и предоставляет
 * инвентарь для взаимодействия с воронками. Выбрасывает книгу при разрушении блока.
 */
public class LecternBlockEntity extends BlockEntity implements Clearable, NamedScreenHandlerFactory {

	public static final int BOOK_SLOT_INDEX = 0;
	public static final int INVENTORY_SIZE = 1;
	public static final int PAGE_PROPERTY_INDEX = 0;
	public static final int PROPERTY_COUNT = 1;
	private final Inventory inventory = new Inventory() {
		@Override
		public int size() {
			return 1;
		}

		@Override
		public boolean isEmpty() {
			return LecternBlockEntity.this.book.isEmpty();
		}

		@Override
		public ItemStack getStack(int slot) {
			return slot == 0 ? LecternBlockEntity.this.book : ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeStack(int slot, int amount) {
			if (slot != BOOK_SLOT_INDEX) {
				return ItemStack.EMPTY;
			}

			ItemStack split = LecternBlockEntity.this.book.split(amount);
			if (LecternBlockEntity.this.book.isEmpty()) {
				LecternBlockEntity.this.onBookRemoved();
			}

			return split;
		}

		@Override
		public ItemStack removeStack(int slot) {
			if (slot != BOOK_SLOT_INDEX) {
				return ItemStack.EMPTY;
			}

			ItemStack removed = LecternBlockEntity.this.book;
			LecternBlockEntity.this.book = ItemStack.EMPTY;
			LecternBlockEntity.this.onBookRemoved();
			return removed;
		}

		@Override
		public void setStack(int slot, ItemStack stack) {
		}

		@Override
		public int getMaxCountPerStack() {
			return 1;
		}

		@Override
		public void markDirty() {
			LecternBlockEntity.this.markDirty();
		}

		@Override
		public boolean canPlayerUse(PlayerEntity player) {
			return Inventory.canPlayerUse(LecternBlockEntity.this, player) && LecternBlockEntity.this.hasBook();
		}

		@Override
		public boolean isValid(int slot, ItemStack stack) {
			return false;
		}

		@Override
		public void clear() {
		}
	};
	private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
		@Override
		public int get(int index) {
			return index == 0 ? LecternBlockEntity.this.currentPage : 0;
		}

		@Override
		public void set(int index, int value) {
			if (index == 0) {
				LecternBlockEntity.this.setCurrentPage(value);
			}
		}

		@Override
		public int size() {
			return 1;
		}
	};
	ItemStack book = ItemStack.EMPTY;
	int currentPage;
	private int pageCount;

	public LecternBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.LECTERN, pos, state);
	}

	public ItemStack getBook() {
		return book;
	}

	public boolean hasBook() {
		return book.contains(DataComponentTypes.WRITABLE_BOOK_CONTENT)
				|| book.contains(DataComponentTypes.WRITTEN_BOOK_CONTENT);
	}

	public void setBook(ItemStack book) {
		setBook(book, null);
	}

	void onBookRemoved() {
		currentPage = 0;
		pageCount = 0;
		LecternBlock.setHasBook(null, getWorld(), getPos(), getCachedState(), false);
	}

	public void setBook(ItemStack book, @Nullable PlayerEntity player) {
		this.book = resolveBook(book, player);
		currentPage = 0;
		pageCount = getPageCount(this.book);
		markDirty();
	}

	void setCurrentPage(int page) {
		int clamped = MathHelper.clamp(page, 0, pageCount - 1);
		if (clamped == currentPage) {
			return;
		}

		currentPage = clamped;
		markDirty();
		LecternBlock.setPowered(getWorld(), getPos(), getCachedState());
	}

	public int getCurrentPage() {
		return currentPage;
	}

	/**
	 * Вычисляет выходной сигнал компаратора на основе текущей страницы.
	 * Формула: floor(прогресс * 14) + 1 если есть книга, иначе 0.
	 */
	public int getComparatorOutput() {
		float progress = pageCount > 1 ? getCurrentPage() / (pageCount - 1.0F) : 1.0F;
		return MathHelper.floor(progress * 14.0F) + (hasBook() ? 1 : 0);
	}

	private ItemStack resolveBook(ItemStack book, @Nullable PlayerEntity player) {
		if (world instanceof ServerWorld serverWorld) {
			WrittenBookContentComponent.resolveInStack(book, getCommandSource(player, serverWorld), player);
		}

		return book;
	}

	private ServerCommandSource getCommandSource(@Nullable PlayerEntity player, ServerWorld world) {
		String name = player == null ? "Lectern" : player.getStringifiedName();
		Text displayName = player == null ? Text.literal("Lectern") : player.getDisplayName();

		return new ServerCommandSource(
				CommandOutput.DUMMY,
				Vec3d.ofCenter(pos),
				Vec2f.ZERO,
				world,
				LeveledPermissionPredicate.GAMEMASTERS,
				name,
				displayName,
				world.getServer(),
				player
		);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		book = view.<ItemStack>read("Book", ItemStack.CODEC)
				.map(itemStack -> resolveBook(itemStack, null))
				.orElse(ItemStack.EMPTY);
		pageCount = getPageCount(book);
		currentPage = MathHelper.clamp(view.getInt("Page", 0), 0, pageCount - 1);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		if (!getBook().isEmpty()) {
			view.put("Book", ItemStack.CODEC, getBook());
			view.putInt("Page", currentPage);
		}
	}

	@Override
	public void clear() {
		setBook(ItemStack.EMPTY);
	}

	@Override
	public void onBlockReplaced(BlockPos pos, BlockState oldState) {
		if (!oldState.get(LecternBlock.HAS_BOOK) || world == null) {
			return;
		}

		Direction facing = oldState.get(LecternBlock.FACING);
		float offsetX = 0.25F * facing.getOffsetX();
		float offsetZ = 0.25F * facing.getOffsetZ();
		ItemEntity itemEntity = new ItemEntity(
				world,
				pos.getX() + 0.5 + offsetX,
				pos.getY() + 1,
				pos.getZ() + 0.5 + offsetZ,
				getBook().copy()
		);
		itemEntity.setToDefaultPickupDelay();
		world.spawnEntity(itemEntity);
	}

	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
		return new LecternScreenHandler(syncId, inventory, propertyDelegate);
	}

	@Override
	public Text getDisplayName() {
		return Text.translatable("container.lectern");
	}

	private static int getPageCount(ItemStack stack) {
		WrittenBookContentComponent writtenBook = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
		if (writtenBook != null) {
			return writtenBook.pages().size();
		}

		WritableBookContentComponent writableBook = stack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
		return writableBook != null ? writableBook.pages().size() : 0;
	}
}
