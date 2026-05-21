package net.minecraft.entity.player;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityEquipment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackWithSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;

import java.util.Map;
import java.util.function.Predicate;

/**
 * {@code PlayerInventory}.
 */
public class PlayerInventory implements Inventory, Nameable {

	public static final int ITEM_USAGE_COOLDOWN = 5;
	public static final int MAIN_SIZE = 36;
	public static final int HOTBAR_SIZE = 9;
	public static final int OFF_HAND_SLOT = 40;
	public static final int BODY_SLOT = 41;
	public static final int SADDLE_SLOT = 42;
	public static final int NOT_FOUND = -1;
	public static final Int2ObjectMap<EquipmentSlot> EQUIPMENT_SLOTS = new Int2ObjectArrayMap(
			Map.of(
					EquipmentSlot.FEET.getOffsetEntitySlotId(36),
					EquipmentSlot.FEET,
					EquipmentSlot.LEGS.getOffsetEntitySlotId(36),
					EquipmentSlot.LEGS,
					EquipmentSlot.CHEST.getOffsetEntitySlotId(36),
					EquipmentSlot.CHEST,
					EquipmentSlot.HEAD.getOffsetEntitySlotId(36),
					EquipmentSlot.HEAD,
					40,
					EquipmentSlot.OFFHAND,
					41,
					EquipmentSlot.BODY,
					42,
					EquipmentSlot.SADDLE
			)
	);
	private static final Text NAME = Text.translatable("container.inventory");
	private final DefaultedList<ItemStack> main = DefaultedList.ofSize(36, ItemStack.EMPTY);
	private int selectedSlot;
	public final PlayerEntity player;
	private final EntityEquipment equipment;
	private int changeCount;

	public PlayerInventory(PlayerEntity player, EntityEquipment equipment) {
		this.player = player;
		this.equipment = equipment;
	}

	public int getSelectedSlot() {
		return this.selectedSlot;
	}

	public void setSelectedSlot(int slot) {
		if (!isValidHotbarIndex(slot)) {
			throw new IllegalArgumentException("Invalid selected slot");
		}
		else {
			this.selectedSlot = slot;
		}
	}

	public ItemStack getSelectedStack() {
		return this.main.get(this.selectedSlot);
	}

	public ItemStack setSelectedStack(ItemStack stack) {
		return this.main.set(this.selectedSlot, stack);
	}

	public static int getHotbarSize() {
		return 9;
	}

	public DefaultedList<ItemStack> getMainStacks() {
		return this.main;
	}

	private boolean canStackAddMore(ItemStack existingStack, ItemStack stack) {
		return !existingStack.isEmpty()
				&& ItemStack.areItemsAndComponentsEqual(existingStack, stack)
				&& existingStack.isStackable()
				&& existingStack.getCount() < this.getMaxCount(existingStack);
	}

	public int getEmptySlot() {
		for (int i = 0; i < this.main.size(); i++) {
			if (this.main.get(i).isEmpty()) {
				return i;
			}
		}

		return -1;
	}

	public void swapStackWithHotbar(ItemStack stack) {
		this.setSelectedSlot(this.getSwappableHotbarSlot());
		if (!this.main.get(this.selectedSlot).isEmpty()) {
			int i = this.getEmptySlot();
			if (i != -1) {
				this.main.set(i, this.main.get(this.selectedSlot));
			}
		}

		this.main.set(this.selectedSlot, stack);
	}

	public void swapSlotWithHotbar(int slot) {
		this.setSelectedSlot(this.getSwappableHotbarSlot());
		ItemStack itemStack = this.main.get(this.selectedSlot);
		this.main.set(this.selectedSlot, this.main.get(slot));
		this.main.set(slot, itemStack);
	}

	public static boolean isValidHotbarIndex(int slot) {
		return slot >= 0 && slot < 9;
	}

	public int getSlotWithStack(ItemStack stack) {
		for (int i = 0; i < this.main.size(); i++) {
			if (!this.main.get(i).isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, this.main.get(i))) {
				return i;
			}
		}

		return -1;
	}

	public static boolean usableWhenFillingSlot(ItemStack stack) {
		return !stack.isDamaged() && !stack.hasEnchantments() && !stack.contains(DataComponentTypes.CUSTOM_NAME);
	}

	public int getMatchingSlot(RegistryEntry<Item> item, ItemStack stack) {
		for (int i = 0; i < this.main.size(); i++) {
			ItemStack itemStack = this.main.get(i);
			if (!itemStack.isEmpty()
					&& itemStack.itemMatches(item)
					&& usableWhenFillingSlot(itemStack)
					&& (stack.isEmpty() || ItemStack.areItemsAndComponentsEqual(stack, itemStack))) {
				return i;
			}
		}

		return -1;
	}

	public int getSwappableHotbarSlot() {
		for (int i = 0; i < 9; i++) {
			int j = (this.selectedSlot + i) % 9;
			if (this.main.get(j).isEmpty()) {
				return j;
			}
		}

		for (int ix = 0; ix < 9; ix++) {
			int j = (this.selectedSlot + ix) % 9;
			if (!this.main.get(j).hasEnchantments()) {
				return j;
			}
		}

		return this.selectedSlot;
	}

	public int remove(Predicate<ItemStack> shouldRemove, int maxCount, Inventory craftingInventory) {
		int i = 0;
		boolean bl = maxCount == 0;
		i += Inventories.remove(this, shouldRemove, maxCount - i, bl);
		i += Inventories.remove(craftingInventory, shouldRemove, maxCount - i, bl);
		ItemStack itemStack = this.player.currentScreenHandler.getCursorStack();
		i += Inventories.remove(itemStack, shouldRemove, maxCount - i, bl);
		if (itemStack.isEmpty()) {
			this.player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
		}

		return i;
	}

	private int addStack(ItemStack stack) {
		int i = this.getOccupiedSlotWithRoomForStack(stack);
		if (i == -1) {
			i = this.getEmptySlot();
		}

		return i == -1 ? stack.getCount() : this.addStack(i, stack);
	}

	private int addStack(int slot, ItemStack stack) {
		int i = stack.getCount();
		ItemStack itemStack = this.getStack(slot);
		if (itemStack.isEmpty()) {
			itemStack = stack.copyWithCount(0);
			this.setStack(slot, itemStack);
		}

		int j = this.getMaxCount(itemStack) - itemStack.getCount();
		int k = Math.min(i, j);
		if (k == 0) {
			return i;
		}
		else {
			i -= k;
			itemStack.increment(k);
			itemStack.setBobbingAnimationTime(5);
			return i;
		}
	}

	public int getOccupiedSlotWithRoomForStack(ItemStack stack) {
		if (this.canStackAddMore(this.getStack(this.selectedSlot), stack)) {
			return this.selectedSlot;
		}
		else if (this.canStackAddMore(this.getStack(40), stack)) {
			return 40;
		}
		else {
			for (int i = 0; i < this.main.size(); i++) {
				if (this.canStackAddMore(this.main.get(i), stack)) {
					return i;
				}
			}

			return -1;
		}
	}

	public void updateItems() {
		for (int i = 0; i < this.main.size(); i++) {
			ItemStack itemStack = this.getStack(i);
			if (!itemStack.isEmpty()) {
				itemStack.inventoryTick(
						this.player.getEntityWorld(),
						this.player,
						i == this.selectedSlot ? EquipmentSlot.MAINHAND : null
				);
			}
		}
	}

	public boolean insertStack(ItemStack stack) {
		return this.insertStack(-1, stack);
	}

	public boolean insertStack(int slot, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		else {
			try {
				if (stack.isDamaged()) {
					if (slot == -1) {
						slot = this.getEmptySlot();
					}

					if (slot >= 0) {
						this.main.set(slot, stack.copyAndEmpty());
						this.main.get(slot).setBobbingAnimationTime(5);
						return true;
					}
					else if (this.player.isInCreativeMode()) {
						stack.setCount(0);
						return true;
					}
					else {
						return false;
					}
				}
				else {
					int i;
					do {
						i = stack.getCount();
						if (slot == -1) {
							stack.setCount(this.addStack(stack));
						}
						else {
							stack.setCount(this.addStack(slot, stack));
						}
					}
					while (!stack.isEmpty() && stack.getCount() < i);

					if (stack.getCount() == i && this.player.isInCreativeMode()) {
						stack.setCount(0);
						return true;
					}
					else {
						return stack.getCount() < i;
					}
				}
			}
			catch (Throwable var6) {
				CrashReport crashReport = CrashReport.create(var6, "Adding item to inventory");
				CrashReportSection crashReportSection = crashReport.addElement("Item being added");
				crashReportSection.add("Item ID", Item.getRawId(stack.getItem()));
				crashReportSection.add("Item data", stack.getDamage());
				crashReportSection.add("Item name", () -> stack.getName().getString());
				throw new CrashException(crashReport);
			}
		}
	}

	public void offerOrDrop(ItemStack stack) {
		this.offer(stack, true);
	}

	public void offer(ItemStack stack, boolean notifiesClient) {
		while (!stack.isEmpty()) {
			int i = this.getOccupiedSlotWithRoomForStack(stack);
			if (i == -1) {
				i = this.getEmptySlot();
			}

			if (i == -1) {
				this.player.dropItem(stack, false);
				break;
			}

			int j = stack.getMaxCount() - this.getStack(i).getCount();
			if (this.insertStack(i, stack.split(j)) && notifiesClient
					&& this.player instanceof ServerPlayerEntity serverPlayerEntity) {
				serverPlayerEntity.networkHandler.sendPacket(this.createSlotSetPacket(i));
			}
		}
	}

	public SetPlayerInventoryS2CPacket createSlotSetPacket(int slot) {
		return new SetPlayerInventoryS2CPacket(slot, this.getStack(slot).copy());
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		if (slot < this.main.size()) {
			return Inventories.splitStack(this.main, slot, amount);
		}
		else {
			EquipmentSlot equipmentSlot = (EquipmentSlot) EQUIPMENT_SLOTS.get(slot);
			if (equipmentSlot != null) {
				ItemStack itemStack = this.equipment.get(equipmentSlot);
				if (!itemStack.isEmpty()) {
					return itemStack.split(amount);
				}
			}

			return ItemStack.EMPTY;
		}
	}

	public void removeOne(ItemStack stack) {
		for (int i = 0; i < this.main.size(); i++) {
			if (this.main.get(i) == stack) {
				this.main.set(i, ItemStack.EMPTY);
				return;
			}
		}

		ObjectIterator var5 = EQUIPMENT_SLOTS.values().iterator();

		while (var5.hasNext()) {
			EquipmentSlot equipmentSlot = (EquipmentSlot) var5.next();
			ItemStack itemStack = this.equipment.get(equipmentSlot);
			if (itemStack == stack) {
				this.equipment.put(equipmentSlot, ItemStack.EMPTY);
				return;
			}
		}
	}

	@Override
	public ItemStack removeStack(int slot) {
		if (slot < this.main.size()) {
			ItemStack itemStack = this.main.get(slot);
			this.main.set(slot, ItemStack.EMPTY);
			return itemStack;
		}
		else {
			EquipmentSlot equipmentSlot = (EquipmentSlot) EQUIPMENT_SLOTS.get(slot);
			return equipmentSlot != null ? this.equipment.put(equipmentSlot, ItemStack.EMPTY) : ItemStack.EMPTY;
		}
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		if (slot < this.main.size()) {
			this.main.set(slot, stack);
		}

		EquipmentSlot equipmentSlot = (EquipmentSlot) EQUIPMENT_SLOTS.get(slot);
		if (equipmentSlot != null) {
			this.equipment.put(equipmentSlot, stack);
		}
	}

	public void writeData(WriteView.ListAppender<StackWithSlot> list) {
		for (int i = 0; i < this.main.size(); i++) {
			ItemStack itemStack = this.main.get(i);
			if (!itemStack.isEmpty()) {
				list.add(new StackWithSlot(i, itemStack));
			}
		}
	}

	public void readData(ReadView.TypedListReadView<StackWithSlot> list) {
		this.main.clear();

		for (StackWithSlot stackWithSlot : list) {
			if (stackWithSlot.isValidSlot(this.main.size())) {
				this.setStack(stackWithSlot.slot(), stackWithSlot.stack());
			}
		}
	}

	@Override
	public int size() {
		return this.main.size() + EQUIPMENT_SLOTS.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemStack : this.main) {
			if (!itemStack.isEmpty()) {
				return false;
			}
		}

		ObjectIterator var3 = EQUIPMENT_SLOTS.values().iterator();

		while (var3.hasNext()) {
			EquipmentSlot equipmentSlot = (EquipmentSlot) var3.next();
			if (!this.equipment.get(equipmentSlot).isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		if (slot < this.main.size()) {
			return this.main.get(slot);
		}
		else {
			EquipmentSlot equipmentSlot = (EquipmentSlot) EQUIPMENT_SLOTS.get(slot);
			return equipmentSlot != null ? this.equipment.get(equipmentSlot) : ItemStack.EMPTY;
		}
	}

	@Override
	public Text getName() {
		return NAME;
	}

	public void dropAll() {
		for (int i = 0; i < this.main.size(); i++) {
			ItemStack itemStack = this.main.get(i);
			if (!itemStack.isEmpty()) {
				this.player.dropItem(itemStack, true, false);
				this.main.set(i, ItemStack.EMPTY);
			}
		}

		this.equipment.dropAll(this.player);
	}

	@Override
	public void markDirty() {
		this.changeCount++;
	}

	public int getChangeCount() {
		return this.changeCount;
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}

	public boolean contains(ItemStack stack) {
		for (ItemStack itemStack : this) {
			if (!itemStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(itemStack, stack)) {
				return true;
			}
		}

		return false;
	}

	public boolean contains(TagKey<Item> tag) {
		for (ItemStack itemStack : this) {
			if (!itemStack.isEmpty() && itemStack.isIn(tag)) {
				return true;
			}
		}

		return false;
	}

	public boolean contains(Predicate<ItemStack> predicate) {
		for (ItemStack itemStack : this) {
			if (predicate.test(itemStack)) {
				return true;
			}
		}

		return false;
	}

	public void clone(PlayerInventory other) {
		for (int i = 0; i < this.size(); i++) {
			this.setStack(i, other.getStack(i));
		}

		this.setSelectedSlot(other.getSelectedSlot());
	}

	@Override
	public void clear() {
		this.main.clear();
		this.equipment.clear();
	}

	public void populateRecipeFinder(RecipeFinder finder) {
		for (ItemStack itemStack : this.main) {
			finder.addInputIfUsable(itemStack);
		}
	}

	public ItemStack dropSelectedItem(boolean entireStack) {
		ItemStack itemStack = this.getSelectedStack();
		return itemStack.isEmpty() ? ItemStack.EMPTY
		                           : this.removeStack(this.selectedSlot, entireStack ? itemStack.getCount() : 1);
	}
}
