package net.minecraft.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChiseledBookshelfBlock;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.ListInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.GameEvent;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * Блок-сущность резного книжного шкафа. Хранит до {@link #MAX_BOOKS} книг
 * и обновляет состояние блока при каждом изменении содержимого слота.
 */
public class ChiseledBookshelfBlockEntity extends BlockEntity implements ListInventory {

	public static final int MAX_BOOKS = 6;
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int DEFAULT_LAST_INTERACTED_SLOT = -1;

	private final DefaultedList<ItemStack> heldStacks = DefaultedList.ofSize(MAX_BOOKS, ItemStack.EMPTY);
	private int lastInteractedSlot = DEFAULT_LAST_INTERACTED_SLOT;

	public ChiseledBookshelfBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.CHISELED_BOOKSHELF, pos, state);
	}

	private void updateState(int interactedSlot) {
		if (interactedSlot < 0 || interactedSlot >= MAX_BOOKS) {
			LOGGER.error("Expected slot 0-{}, got {}", MAX_BOOKS - 1, interactedSlot);
			return;
		}

		lastInteractedSlot = interactedSlot;
		BlockState blockState = getCachedState();

		for (int slot = 0; slot < ChiseledBookshelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); slot++) {
			boolean hasBook = !getStack(slot).isEmpty();
			BooleanProperty slotProperty = ChiseledBookshelfBlock.SLOT_OCCUPIED_PROPERTIES.get(slot);
			blockState = blockState.with(slotProperty, hasBook);
		}

		Objects.requireNonNull(world).setBlockState(pos, blockState, 3);
		world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(blockState));
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		heldStacks.clear();
		Inventories.readData(view, heldStacks);
		lastInteractedSlot = view.getInt("last_interacted_slot", DEFAULT_LAST_INTERACTED_SLOT);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		Inventories.writeData(view, heldStacks, true);
		view.putInt("last_interacted_slot", lastInteractedSlot);
	}

	@Override
	public int getMaxCountPerStack() {
		return 1;
	}

	@Override
	public boolean canAccept(ItemStack stack) {
		return stack.isIn(ItemTags.BOOKSHELF_BOOKS);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack itemStack = Objects.requireNonNullElse(getHeldStacks().get(slot), ItemStack.EMPTY);
		getHeldStacks().set(slot, ItemStack.EMPTY);
		if (!itemStack.isEmpty()) {
			updateState(slot);
		}

		return itemStack;
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		if (canAccept(stack)) {
			getHeldStacks().set(slot, stack);
			updateState(slot);
		} else if (stack.isEmpty()) {
			removeStack(slot, getMaxCountPerStack());
		}
	}

	@Override
	public boolean canTransferTo(Inventory hopperInventory, int slot, ItemStack stack) {
		return hopperInventory.containsAny(
				candidate -> candidate.isEmpty()
						|| ItemStack.areItemsAndComponentsEqual(stack, candidate)
						&& candidate.getCount() + stack.getCount() <= hopperInventory.getMaxCount(candidate)
		);
	}

	@Override
	public DefaultedList<ItemStack> getHeldStacks() {
		return heldStacks;
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return Inventory.canPlayerUse(this, player);
	}

	public int getLastInteractedSlot() {
		return lastInteractedSlot;
	}

	@Override
	protected void readComponents(ComponentsAccess components) {
		super.readComponents(components);
		components.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyTo(heldStacks);
	}

	@Override
	protected void addComponents(ComponentMap.Builder builder) {
		super.addComponents(builder);
		builder.add(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(heldStacks));
	}

	@Override
	public void removeFromCopiedStackData(WriteView view) {
		view.remove("Items");
	}
}
