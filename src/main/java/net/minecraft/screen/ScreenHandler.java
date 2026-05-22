package net.minecraft.screen;

import com.google.common.base.Suppliers;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.screen.sync.TrackedSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ClickType;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Базовый класс для всех экранов-обработчиков инвентаря.
 * <p>
 * Управляет слотами, свойствами, синхронизацией между клиентом и сервером,
 * а также обработкой кликов по слотам (включая быстрое перемещение и быстрое крафтение).
 */
public abstract class ScreenHandler {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Индекс слота, обозначающий пустое пространство вне инвентаря (бросить предмет). */
	public static final int EMPTY_SPACE_SLOT_INDEX = -999;

	public static final int CLICK_TYPE_LEFT = 0;
	public static final int CLICK_TYPE_RIGHT = 1;
	public static final int CLICK_TYPE_MIDDLE = 2;

	public static final int CLICK_MODE_NORMAL = 0;
	public static final int CLICK_MODE_QUICK_MOVE = 1;
	public static final int CLICK_MODE_QUICK_CRAFT = 2;

	public static final int MAX_STACK_SIZE = Integer.MAX_VALUE;

	/** Смещение слотов инвентаря игрока (строки 2–4) от начала хотбара. */
	public static final int PLAYER_INVENTORY_START = 9;

	/** Шаг в пикселях между слотами хотбара/инвентаря (18px). */
	public static final int PLAYER_HOTBAR_START = 18;

	/** Максимальный уровень ревизии синхронизации (15-битное значение). */
	private static final int MAX_REVISION = 32767;

	/** Количество строк инвентаря игрока (без хотбара). */
	private static final int PLAYER_INVENTORY_ROWS = 3;

	/** Количество слотов в одной строке инвентаря. */
	private static final int PLAYER_INVENTORY_COLS = 9;

	/** Смещение по Y от инвентаря до хотбара (в пикселях). */
	private static final int HOTBAR_Y_OFFSET = 58;

	/** Максимальный уровень компаратора. */
	private static final int MAX_COMPARATOR_LEVEL = 15;

	private final DefaultedList<ItemStack> trackedStacks = DefaultedList.of();
	public final DefaultedList<Slot> slots = DefaultedList.of();
	private final List<Property> properties = Lists.newArrayList();
	private ItemStack cursorStack = ItemStack.EMPTY;
	private final DefaultedList<TrackedSlot> trackedSlots = DefaultedList.of();
	private final IntList trackedPropertyValues = new IntArrayList();
	private TrackedSlot trackedCursorSlot = TrackedSlot.ALWAYS_IN_SYNC;
	private int revision;
	private final @Nullable ScreenHandlerType<?> type;
	public final int syncId;
	private int quickCraftButton = -1;
	private int quickCraftStage;
	private final Set<Slot> quickCraftSlots = Sets.newHashSet();
	private final List<ScreenHandlerListener> listeners = Lists.newArrayList();
	private @Nullable ScreenHandlerSyncHandler syncHandler;
	private boolean disableSync;

	protected ScreenHandler(@Nullable ScreenHandlerType<?> type, int syncId) {
		this.type = type;
		this.syncId = syncId;
	}

	/**
	 * Добавляет слоты хотбара игрока (9 слотов) в обработчик.
	 *
	 * @param playerInventory инвентарь игрока
	 * @param left            X-координата первого слота
	 * @param y               Y-координата слотов
	 */
	protected void addPlayerHotbarSlots(Inventory playerInventory, int left, int y) {
		for (int index = 0; index < PLAYER_INVENTORY_COLS; index++) {
			addSlot(new Slot(playerInventory, index, left + index * PLAYER_HOTBAR_START, y));
		}
	}

	/**
	 * Добавляет слоты основного инвентаря игрока (3 строки по 9 слотов) в обработчик.
	 *
	 * @param playerInventory инвентарь игрока
	 * @param left            X-координата первого слота
	 * @param top             Y-координата первой строки
	 */
	protected void addPlayerInventorySlots(Inventory playerInventory, int left, int top) {
		for (int row = 0; row < PLAYER_INVENTORY_ROWS; row++) {
			for (int col = 0; col < PLAYER_INVENTORY_COLS; col++) {
				addSlot(new Slot(
						playerInventory,
						col + (row + 1) * PLAYER_INVENTORY_COLS,
						left + col * PLAYER_HOTBAR_START,
						top + row * PLAYER_HOTBAR_START
				));
			}
		}
	}

	/**
	 * Добавляет все слоты инвентаря и хотбара игрока.
	 *
	 * @param playerInventory инвентарь игрока
	 * @param left            X-координата
	 * @param top             Y-координата инвентаря
	 */
	protected void addPlayerSlots(Inventory playerInventory, int left, int top) {
		addPlayerInventorySlots(playerInventory, left, top);
		addPlayerHotbarSlots(playerInventory, left, top + HOTBAR_Y_OFFSET);
	}

	/**
	 * Проверяет, может ли игрок использовать блок в заданном контексте.
	 * Блок должен присутствовать в мире и игрок должен находиться в радиусе 4 блоков.
	 *
	 * @param context контекст экрана (мир + позиция блока)
	 * @param player  игрок
	 * @param block   ожидаемый блок
	 * @return {@code true} если взаимодействие разрешено
	 */
	protected static boolean canUse(ScreenHandlerContext context, PlayerEntity player, Block block) {
		return context.get(
				(world, pos) -> world.getBlockState(pos).isOf(block)
						&& player.canInteractWithBlockAt(pos, 4.0),
				true
		);
	}

	/**
	 * Возвращает тип обработчика экрана.
	 *
	 * @throws UnsupportedOperationException если тип не был задан (например, для инвентаря игрока)
	 */
	public ScreenHandlerType<?> getType() {
		if (type == null) {
			throw new UnsupportedOperationException("Unable to construct this menu by type");
		}

		return type;
	}

	/**
	 * Проверяет, что инвентарь содержит не менее {@code expectedSize} слотов.
	 *
	 * @param inventory    проверяемый инвентарь
	 * @param expectedSize минимально ожидаемый размер
	 * @throws IllegalArgumentException если размер меньше ожидаемого
	 */
	protected static void checkSize(Inventory inventory, int expectedSize) {
		int actualSize = inventory.size();
		if (actualSize < expectedSize) {
			throw new IllegalArgumentException(
					"Container size " + actualSize + " is smaller than expected " + expectedSize
			);
		}
	}

	/**
	 * Проверяет, что делегат свойств содержит не менее {@code expectedCount} значений.
	 *
	 * @param data          делегат свойств
	 * @param expectedCount минимально ожидаемое количество
	 * @throws IllegalArgumentException если количество меньше ожидаемого
	 */
	protected static void checkDataCount(PropertyDelegate data, int expectedCount) {
		int actualCount = data.size();
		if (actualCount < expectedCount) {
			throw new IllegalArgumentException(
					"Container data count " + actualCount + " is smaller than expected " + expectedCount
			);
		}
	}

	public boolean isValid(int slot) {
		return slot == -1 || slot == EMPTY_SPACE_SLOT_INDEX || slot < slots.size();
	}

	/**
	 * Регистрирует слот в обработчике и назначает ему порядковый идентификатор.
	 *
	 * @param slot добавляемый слот
	 * @return тот же слот (для цепочки вызовов)
	 */
	protected Slot addSlot(Slot slot) {
		slot.id = slots.size();
		slots.add(slot);
		trackedStacks.add(ItemStack.EMPTY);
		trackedSlots.add(
				syncHandler != null ? syncHandler.createTrackedSlot() : TrackedSlot.ALWAYS_IN_SYNC
		);
		return slot;
	}

	/**
	 * Регистрирует свойство (синхронизируемое целочисленное значение) в обработчике.
	 *
	 * @param property добавляемое свойство
	 * @return тот же объект свойства (для цепочки вызовов)
	 */
	protected Property addProperty(Property property) {
		properties.add(property);
		trackedPropertyValues.add(0);
		return property;
	}

	/**
	 * Регистрирует все свойства из делегата.
	 *
	 * @param propertyDelegate делегат, содержащий набор свойств
	 */
	protected void addProperties(PropertyDelegate propertyDelegate) {
		for (int index = 0; index < propertyDelegate.size(); index++) {
			addProperty(Property.create(propertyDelegate, index));
		}
	}

	/**
	 * Добавляет слушателя изменений и немедленно отправляет ему текущее состояние.
	 *
	 * @param listener слушатель для регистрации
	 */
	public void addListener(ScreenHandlerListener listener) {
		if (listeners.contains(listener)) {
			return;
		}

		listeners.add(listener);
		sendContentUpdates();
	}

	/**
	 * Устанавливает обработчик синхронизации и пересоздаёт все отслеживаемые слоты.
	 * Вызывается при открытии экрана на сервере.
	 *
	 * @param handler новый обработчик синхронизации
	 */
	public void updateSyncHandler(ScreenHandlerSyncHandler handler) {
		syncHandler = handler;
		trackedCursorSlot = handler.createTrackedSlot();
		trackedSlots.replaceAll(slot -> handler.createTrackedSlot());
		syncState();
	}

	/**
	 * Выполняет полную синхронизацию состояния экрана с клиентом.
	 * Снимает снимок всех стеков и свойств, затем отправляет их через {@link ScreenHandlerSyncHandler}.
	 */
	public void syncState() {
		List<ItemStack> stackSnapshot = new ArrayList<>(slots.size());

		for (int index = 0; index < slots.size(); index++) {
			ItemStack stack = slots.get(index).getStack();
			stackSnapshot.add(stack.copy());
			trackedSlots.get(index).setReceivedStack(stack);
		}

		ItemStack cursorSnapshot = getCursorStack();
		trackedCursorSlot.setReceivedStack(cursorSnapshot);

		for (int index = 0; index < properties.size(); index++) {
			trackedPropertyValues.set(index, properties.get(index).get());
		}

		if (syncHandler != null) {
			syncHandler.updateState(this, stackSnapshot, cursorSnapshot.copy(), trackedPropertyValues.toIntArray());
		}
	}

	public void removeListener(ScreenHandlerListener listener) {
		listeners.remove(listener);
	}

	public DefaultedList<ItemStack> getStacks() {
		DefaultedList<ItemStack> result = DefaultedList.of();

		for (Slot slot : slots) {
			result.add(slot.getStack());
		}

		return result;
	}

	/**
	 * Проверяет изменения в слотах и свойствах и уведомляет слушателей о дельте.
	 * Также отправляет обновления через {@link ScreenHandlerSyncHandler} при необходимости.
	 */
	public void sendContentUpdates() {
		for (int index = 0; index < slots.size(); index++) {
			ItemStack stack = slots.get(index).getStack();
			Supplier<ItemStack> copySupplier = Suppliers.memoize(stack::copy);
			updateTrackedSlot(index, stack, copySupplier);
			checkSlotUpdates(index, stack, copySupplier);
		}

		checkCursorStackUpdates();

		for (int index = 0; index < properties.size(); index++) {
			Property property = properties.get(index);
			int value = property.get();
			if (property.hasChanged()) {
				notifyPropertyUpdate(index, value);
			}

			checkPropertyUpdates(index, value);
		}
	}

	/**
	 * Принудительно обновляет все отслеживаемые слоты и свойства на клиенте,
	 * затем выполняет полную синхронизацию состояния.
	 */
	public void updateToClient() {
		for (int index = 0; index < slots.size(); index++) {
			ItemStack stack = slots.get(index).getStack();
			updateTrackedSlot(index, stack, stack::copy);
		}

		for (int index = 0; index < properties.size(); index++) {
			Property property = properties.get(index);
			if (property.hasChanged()) {
				notifyPropertyUpdate(index, property.get());
			}
		}

		syncState();
	}

	private void notifyPropertyUpdate(int index, int value) {
		for (ScreenHandlerListener listener : listeners) {
			listener.onPropertyUpdate(this, index, value);
		}
	}

	private void updateTrackedSlot(int slot, ItemStack stack, Supplier<ItemStack> copySupplier) {
		ItemStack tracked = trackedStacks.get(slot);
		if (ItemStack.areEqual(tracked, stack)) {
			return;
		}

		ItemStack copy = copySupplier.get();
		trackedStacks.set(slot, copy);

		for (ScreenHandlerListener listener : listeners) {
			listener.onSlotUpdate(this, slot, copy);
		}
	}

	private void checkSlotUpdates(int slot, ItemStack stack, Supplier<ItemStack> copySupplier) {
		if (disableSync) {
			return;
		}

		TrackedSlot tracked = trackedSlots.get(slot);
		if (tracked.isInSync(stack)) {
			return;
		}

		tracked.setReceivedStack(stack);
		if (syncHandler != null) {
			syncHandler.updateSlot(this, slot, copySupplier.get());
		}
	}

	private void checkPropertyUpdates(int id, int value) {
		if (disableSync) {
			return;
		}

		int tracked = trackedPropertyValues.getInt(id);
		if (tracked == value) {
			return;
		}

		trackedPropertyValues.set(id, value);
		if (syncHandler != null) {
			syncHandler.updateProperty(this, id, value);
		}
	}

	private void checkCursorStackUpdates() {
		if (disableSync) {
			return;
		}

		ItemStack cursor = getCursorStack();
		if (trackedCursorSlot.isInSync(cursor)) {
			return;
		}

		trackedCursorSlot.setReceivedStack(cursor);
		if (syncHandler != null) {
			syncHandler.updateCursorStack(this, cursor.copy());
		}
	}

	public void setReceivedStack(int slot, ItemStack stack) {
		trackedSlots.get(slot).setReceivedStack(stack);
	}

	public void setReceivedHash(int slot, ItemStackHash hash) {
		if (slot < 0 || slot >= trackedSlots.size()) {
			LOGGER.debug("Incorrect slot index: {} available slots: {}", slot, trackedSlots.size());
			return;
		}

		trackedSlots.get(slot).setReceivedHash(hash);
	}

	public void setReceivedCursorHash(ItemStackHash cursorStackHash) {
		trackedCursorSlot.setReceivedHash(cursorStackHash);
	}

	public boolean onButtonClick(PlayerEntity player, int id) {
		return false;
	}

	public Slot getSlot(int index) {
		return slots.get(index);
	}

	/**
	 * Быстро перемещает стек из указанного слота в подходящее место инвентаря.
	 * Реализация зависит от конкретного типа экрана.
	 *
	 * @param player игрок
	 * @param slot   индекс слота-источника
	 * @return оставшийся стек после перемещения, или {@link ItemStack#EMPTY}
	 */
	public abstract ItemStack quickMove(PlayerEntity player, int slot);

	public void selectBundleStack(int slot, int selectedStack) {
		if (slot < 0 || slot >= slots.size()) {
			return;
		}

		ItemStack stack = slots.get(slot).getStack();
		BundleItem.setSelectedStackIndex(stack, selectedStack);
	}

	/**
	 * Обрабатывает клик по слоту инвентаря, оборачивая логику в crash-report при ошибке.
	 *
	 * @param slotIndex  индекс слота (или {@link #EMPTY_SPACE_SLOT_INDEX})
	 * @param button     кнопка мыши или идентификатор действия
	 * @param actionType тип действия
	 * @param player     игрок
	 */
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		try {
			internalOnSlotClick(slotIndex, button, actionType, player);
		} catch (Exception exception) {
			CrashReport crashReport = CrashReport.create(exception, "Container click");
			CrashReportSection section = crashReport.addElement("Click info");
			section.add(
					"Menu Type",
					() -> type != null ? Registries.SCREEN_HANDLER.getId(type).toString() : "<no type>"
			);
			section.add("Menu Class", () -> getClass().getCanonicalName());
			section.add("Slot Count", slots.size());
			section.add("Slot", slotIndex);
			section.add("Button", button);
			section.add("Type", actionType);
			throw new CrashException(crashReport);
		}
	}

	/**
	 * Внутренняя реализация обработки клика по слоту.
	 * Содержит всю логику: PICKUP, QUICK_MOVE, SWAP, CLONE, THROW, QUICK_CRAFT, PICKUP_ALL.
	 */
	private void internalOnSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		PlayerInventory playerInventory = player.getInventory();

		if (actionType == SlotActionType.QUICK_CRAFT) {
			handleQuickCraft(slotIndex, button, player);
			return;
		}

		if (quickCraftStage != 0) {
			endQuickCraft();
			return;
		}

		if ((actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE)
				&& (button == CLICK_TYPE_LEFT || button == CLICK_TYPE_RIGHT)) {
			handlePickupOrQuickMove(slotIndex, button, actionType, player);
			return;
		}

		if (actionType == SlotActionType.SWAP && (button >= 0 && button < 9 || button == 40)) {
			handleSwap(slotIndex, button, player, playerInventory);
			return;
		}

		if (actionType == SlotActionType.CLONE && player.isInCreativeMode()
				&& getCursorStack().isEmpty() && slotIndex >= 0) {
			handleClone(slotIndex);
			return;
		}

		if (actionType == SlotActionType.THROW && getCursorStack().isEmpty() && slotIndex >= 0) {
			handleThrow(slotIndex, button, player);
			return;
		}

		if (actionType == SlotActionType.PICKUP_ALL && slotIndex >= 0) {
			handlePickupAll(slotIndex, button, player);
		}
	}

	/**
	 * Обрабатывает режим быстрого крафтения (drag-распределение стека по слотам).
	 * Стадии: 0 — начало, 1 — добавление слотов, 2 — завершение и распределение.
	 */
	private void handleQuickCraft(int slotIndex, int button, PlayerEntity player) {
		int previousStage = quickCraftStage;
		quickCraftStage = unpackQuickCraftStage(button);

		if ((previousStage != 1 || quickCraftStage != 2) && previousStage != quickCraftStage) {
			endQuickCraft();
			return;
		}

		if (getCursorStack().isEmpty()) {
			endQuickCraft();
			return;
		}

		if (quickCraftStage == 0) {
			quickCraftButton = unpackQuickCraftButton(button);
			if (shouldQuickCraftContinue(quickCraftButton, player)) {
				quickCraftStage = 1;
				quickCraftSlots.clear();
			} else {
				endQuickCraft();
			}

			return;
		}

		if (quickCraftStage == 1) {
			Slot slot = slots.get(slotIndex);
			ItemStack cursor = getCursorStack();
			if (canInsertItemIntoSlot(slot, cursor, true)
					&& slot.canInsert(cursor)
					&& (quickCraftButton == CLICK_TYPE_MIDDLE || cursor.getCount() > quickCraftSlots.size())
					&& canInsertIntoSlot(slot)) {
				quickCraftSlots.add(slot);
			}

			return;
		}

		if (quickCraftStage == 2) {
			if (!quickCraftSlots.isEmpty()) {
				if (quickCraftSlots.size() == 1) {
					int singleSlotId = quickCraftSlots.iterator().next().id;
					endQuickCraft();
					internalOnSlotClick(singleSlotId, quickCraftButton, SlotActionType.PICKUP, player);
					return;
				}

				ItemStack cursorCopy = getCursorStack().copy();
				if (cursorCopy.isEmpty()) {
					endQuickCraft();
					return;
				}

				int remaining = getCursorStack().getCount();

				for (Slot targetSlot : quickCraftSlots) {
					ItemStack cursor = getCursorStack();
					if (targetSlot == null
							|| !canInsertItemIntoSlot(targetSlot, cursor, true)
							|| !targetSlot.canInsert(cursor)
							|| (quickCraftButton != CLICK_TYPE_MIDDLE && cursor.getCount() < quickCraftSlots.size())
							|| !canInsertIntoSlot(targetSlot)) {
						continue;
					}

					int existingCount = targetSlot.hasStack() ? targetSlot.getStack().getCount() : 0;
					int maxCount = Math.min(cursorCopy.getMaxCount(), targetSlot.getMaxItemCount(cursorCopy));
					int targetCount = Math.min(
							calculateStackSize(quickCraftSlots, quickCraftButton, cursorCopy) + existingCount,
							maxCount
					);
					remaining -= targetCount - existingCount;
					targetSlot.setStack(cursorCopy.copyWithCount(targetCount));
				}

				cursorCopy.setCount(remaining);
				setCursorStack(cursorCopy);
			}

			endQuickCraft();
			return;
		}

		endQuickCraft();
	}

	private void handlePickupOrQuickMove(
			int slotIndex,
			int button,
			SlotActionType actionType,
			PlayerEntity player
	) {
		ClickType clickType = button == CLICK_TYPE_LEFT ? ClickType.LEFT : ClickType.RIGHT;

		if (slotIndex == EMPTY_SPACE_SLOT_INDEX) {
			if (getCursorStack().isEmpty()) {
				return;
			}

			if (clickType == ClickType.LEFT) {
				player.dropItem(getCursorStack(), true);
				setCursorStack(ItemStack.EMPTY);
			} else {
				player.dropItem(getCursorStack().split(1), true);
			}

			return;
		}

		if (actionType == SlotActionType.QUICK_MOVE) {
			if (slotIndex < 0) {
				return;
			}

			Slot slot = slots.get(slotIndex);
			if (!slot.canTakeItems(player)) {
				return;
			}

			ItemStack moved = quickMove(player, slotIndex);
			while (!moved.isEmpty() && ItemStack.areItemsEqual(slot.getStack(), moved)) {
				moved = quickMove(player, slotIndex);
			}

			return;
		}

		if (slotIndex < 0) {
			return;
		}

		Slot slot = slots.get(slotIndex);
		ItemStack slotStack = slot.getStack();
		ItemStack cursor = getCursorStack();
		player.onPickupSlotClick(cursor, slot.getStack(), clickType);

		if (handleSlotClick(player, clickType, slot, slotStack, cursor)) {
			slot.markDirty();
			return;
		}

		if (slotStack.isEmpty()) {
			if (!cursor.isEmpty()) {
				int insertCount = clickType == ClickType.LEFT ? cursor.getCount() : 1;
				setCursorStack(slot.insertStack(cursor, insertCount));
			}
		} else if (slot.canTakeItems(player)) {
			if (cursor.isEmpty()) {
				int takeCount = clickType == ClickType.LEFT
						? slotStack.getCount()
						: (slotStack.getCount() + 1) / 2;
				Optional<ItemStack> taken = slot.tryTakeStackRange(takeCount, Integer.MAX_VALUE, player);
				taken.ifPresent(takenStack -> {
					setCursorStack(takenStack);
					slot.onTakeItem(player, takenStack);
				});
			} else if (slot.canInsert(cursor)) {
				if (ItemStack.areItemsAndComponentsEqual(slotStack, cursor)) {
					int insertCount = clickType == ClickType.LEFT ? cursor.getCount() : 1;
					setCursorStack(slot.insertStack(cursor, insertCount));
				} else if (cursor.getCount() <= slot.getMaxItemCount(cursor)) {
					setCursorStack(slotStack);
					slot.setStack(cursor);
				}
			} else if (ItemStack.areItemsAndComponentsEqual(slotStack, cursor)) {
				Optional<ItemStack> taken = slot.tryTakeStackRange(
						slotStack.getCount(),
						cursor.getMaxCount() - cursor.getCount(),
						player
				);
				taken.ifPresent(takenStack -> {
					cursor.increment(takenStack.getCount());
					slot.onTakeItem(player, takenStack);
				});
			}
		}

		slot.markDirty();
	}

	private void handleSwap(int slotIndex, int button, PlayerEntity player, PlayerInventory playerInventory) {
		ItemStack hotbarStack = playerInventory.getStack(button);
		Slot slot = slots.get(slotIndex);
		ItemStack slotStack = slot.getStack();

		if (hotbarStack.isEmpty() && slotStack.isEmpty()) {
			return;
		}

		if (hotbarStack.isEmpty()) {
			if (!slot.canTakeItems(player)) {
				return;
			}

			playerInventory.setStack(button, slotStack);
			slot.onTake(slotStack.getCount());
			slot.setStack(ItemStack.EMPTY);
			slot.onTakeItem(player, slotStack);
			return;
		}
if (slotStack.isEmpty()) {
	if (!slot.canInsert(hotbarStack)) {
		return;
	}

	int maxCount = slot.getMaxItemCount(hotbarStack);
	if (hotbarStack.getCount() > maxCount) {
		slot.setStack(hotbarStack.split(maxCount));
	} else {
		playerInventory.setStack(button, ItemStack.EMPTY);
		slot.setStack(hotbarStack);
	}

	return;
}

if (slot.canTakeItems(player) && slot.canInsert(hotbarStack)) {
	int maxCount = slot.getMaxItemCount(hotbarStack);
	if (hotbarStack.getCount() > maxCount) {
		slot.setStack(hotbarStack.split(maxCount));
		slot.onTakeItem(player, slotStack);
		if (!playerInventory.insertStack(slotStack)) {
			player.dropItem(slotStack, true);
		}
	} else {
		playerInventory.setStack(button, slotStack);
		slot.setStack(hotbarStack);
		slot.onTakeItem(player, slotStack);
	}
}
}

private void handleClone(int slotIndex) {
Slot slot = slots.get(slotIndex);
if (!slot.hasStack()) {
	return;
}

ItemStack stack = slot.getStack();
setCursorStack(stack.copyWithCount(stack.getMaxCount()));
}

private void handleThrow(int slotIndex, int button, PlayerEntity player) {
if (!player.canDropItems()) {
	return;
}

Slot slot = slots.get(slotIndex);
int throwCount = button == CLICK_TYPE_LEFT ? 1 : slot.getStack().getCount();
ItemStack thrown = slot.takeStackRange(throwCount, Integer.MAX_VALUE, player);
player.dropItem(thrown, true);
player.dropCreativeStack(thrown);

if (button != CLICK_TYPE_RIGHT) {
	return;
}

while (!thrown.isEmpty() && ItemStack.areItemsEqual(slot.getStack(), thrown)) {
	if (!player.canDropItems()) {
		return;
	}

	thrown = slot.takeStackRange(throwCount, Integer.MAX_VALUE, player);
	player.dropItem(thrown, true);
	player.dropCreativeStack(thrown);
}
}

/**
* Собирает все стеки того же типа, что и курсор, из всех слотов инвентаря.
* Сначала собирает неполные стеки (проход 0), затем полные (проход 1).
*/
private void handlePickupAll(int slotIndex, int button, PlayerEntity player) {
Slot targetSlot = slots.get(slotIndex);
ItemStack cursor = getCursorStack();
if (cursor.isEmpty() || (targetSlot.hasStack() && targetSlot.canTakeItems(player))) {
	return;
}

int startIndex = button == CLICK_TYPE_LEFT ? 0 : slots.size() - 1;
int step = button == CLICK_TYPE_LEFT ? 1 : -1;

for (int pass = 0; pass < 2; pass++) {
	for (int index = startIndex;
			index >= 0 && index < slots.size() && cursor.getCount() < cursor.getMaxCount();
			index += step) {
		Slot slot = slots.get(index);
		if (!slot.hasStack()
				|| !canInsertItemIntoSlot(slot, cursor, true)
				|| !slot.canTakeItems(player)
				|| !canInsertIntoSlot(cursor, slot)) {
			continue;
		}

		ItemStack slotStack = slot.getStack();
		if (pass != 0 || slotStack.getCount() != slotStack.getMaxCount()) {
			ItemStack taken = slot.takeStackRange(
					slotStack.getCount(),
					cursor.getMaxCount() - cursor.getCount(),
					player
			);
			cursor.increment(taken.getCount());
		}
	}
}
}

private boolean handleSlotClick(
	PlayerEntity player,
	ClickType clickType,
	Slot slot,
	ItemStack stack,
	ItemStack cursorStack
) {
FeatureSet featureSet = player.getEntityWorld().getEnabledFeatures();
if (cursorStack.isItemEnabled(featureSet) && cursorStack.onStackClicked(slot, clickType, player)) {
	return true;
}

return stack.isItemEnabled(featureSet) && stack.onClicked(
		cursorStack,
		slot,
		clickType,
		player,
		getCursorStackReference()
);
}

private StackReference getCursorStackReference() {
return new StackReference() {
	@Override
	public ItemStack get() {
		return ScreenHandler.this.getCursorStack();
	}

	@Override
	public boolean set(ItemStack stack) {
		ScreenHandler.this.setCursorStack(stack);
		return true;
	}
};
}

public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
return true;
}

public void onClosed(PlayerEntity player) {
if (player instanceof ServerPlayerEntity) {
	ItemStack cursor = getCursorStack();
	if (!cursor.isEmpty()) {
		offerOrDropStack(player, cursor);
		setCursorStack(ItemStack.EMPTY);
	}
}
}

private static void offerOrDropStack(PlayerEntity player, ItemStack stack) {
boolean removed = player.isRemoved()
		&& player.getRemovalReason() != Entity.RemovalReason.CHANGED_DIMENSION;
boolean disconnected = player instanceof ServerPlayerEntity serverPlayer
		&& serverPlayer.isDisconnected();

if (removed || disconnected) {
	player.dropItem(stack, false);
} else if (player instanceof ServerPlayerEntity) {
	player.getInventory().offerOrDrop(stack);
}
}

protected void dropInventory(PlayerEntity player, Inventory inventory) {
for (int index = 0; index < inventory.size(); index++) {
	offerOrDropStack(player, inventory.removeStack(index));
}
}

public void onContentChanged(Inventory inventory) {
sendContentUpdates();
}

public void setStackInSlot(int slot, int revision, ItemStack stack) {
getSlot(slot).setStackNoCallbacks(stack);
this.revision = revision;
}

/**
* Обновляет все слоты и курсор из пакета синхронизации (клиентская сторона).
*
* @param revision   новая ревизия состояния
* @param stacks     список стеков для всех слотов
* @param cursorStack стек на курсоре
*/
public void updateSlotStacks(int revision, List<ItemStack> stacks, ItemStack cursorStack) {
for (int index = 0; index < stacks.size(); index++) {
	getSlot(index).setStackNoCallbacks(stacks.get(index));
}

this.cursorStack = cursorStack;
this.revision = revision;
}

public void setProperty(int id, int value) {
properties.get(id).set(value);
}

/**
* Проверяет, может ли игрок использовать данный экран.
*
* @param player игрок
* @return {@code true} если использование разрешено
*/
public abstract boolean canUse(PlayerEntity player);

/**
* Вставляет стек в диапазон слотов, сначала пытаясь объединить с существующими стеками,
* затем — найти пустой слот.
*
* @param stack      вставляемый стек (изменяется in-place)
* @param startIndex начальный индекс диапазона (включительно)
* @param endIndex   конечный индекс диапазона (исключительно)
* @param fromLast   если {@code true}, обход идёт с конца диапазона
* @return {@code true} если хотя бы часть стека была вставлена
*/
protected boolean insertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast) {
boolean inserted = false;
int index = fromLast ? endIndex - 1 : startIndex;

if (stack.isStackable()) {
	while (!stack.isEmpty() && (fromLast ? index >= startIndex : index < endIndex)) {
		Slot slot = slots.get(index);
		ItemStack slotStack = slot.getStack();
		if (!slotStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, slotStack)) {
			int combined = slotStack.getCount() + stack.getCount();
			int maxCount = slot.getMaxItemCount(slotStack);
			if (combined <= maxCount) {
				stack.setCount(0);
				slotStack.setCount(combined);
				slot.markDirty();
				inserted = true;
			} else if (slotStack.getCount() < maxCount) {
				stack.decrement(maxCount - slotStack.getCount());
				slotStack.setCount(maxCount);
				slot.markDirty();
				inserted = true;
			}
		}

		index += fromLast ? -1 : 1;
	}
}

if (!stack.isEmpty()) {
	index = fromLast ? endIndex - 1 : startIndex;

	while (fromLast ? index >= startIndex : index < endIndex) {
		Slot slot = slots.get(index);
		ItemStack slotStack = slot.getStack();
		if (slotStack.isEmpty() && slot.canInsert(stack)) {
			int maxCount = slot.getMaxItemCount(stack);
			slot.setStack(stack.split(Math.min(stack.getCount(), maxCount)));
			slot.markDirty();
			inserted = true;
			break;
		}

		index += fromLast ? -1 : 1;
	}
}

return inserted;
}

/**
* Распаковывает кнопку (тип распределения) из упакованных данных быстрого крафтения.
* Биты 2–3 кодируют кнопку: 0 = левая, 1 = правая, 2 = средняя.
*
* @param quickCraftData упакованные данные
* @return тип кнопки
*/
public static int unpackQuickCraftButton(int quickCraftData) {
return quickCraftData >> 2 & 3;
}

/**
* Распаковывает стадию из упакованных данных быстрого крафтения.
* Биты 0–1 кодируют стадию: 0 = начало, 1 = добавление, 2 = завершение.
*
* @param quickCraftData упакованные данные
* @return стадия
*/
public static int unpackQuickCraftStage(int quickCraftData) {
return quickCraftData & 3;
}

/**
* Упаковывает стадию и кнопку в единое целое для передачи по сети.
*
* @param quickCraftStage стадия (0–2)
* @param buttonId        кнопка (0–2)
* @return упакованное значение
*/
public static int packQuickCraftData(int quickCraftStage, int buttonId) {
return quickCraftStage & 3 | (buttonId & 3) << 2;
}

/**
* Определяет, может ли данная кнопка продолжить быстрое крафтение.
* Средняя кнопка (режим клонирования) доступна только в креативном режиме.
*
* @param stage  тип кнопки (0 = левая, 1 = правая, 2 = средняя)
* @param player игрок
* @return {@code true} если режим разрешён
*/
public static boolean shouldQuickCraftContinue(int stage, PlayerEntity player) {
if (stage == CLICK_TYPE_LEFT) {
	return true;
}

return stage == CLICK_TYPE_RIGHT ? true : stage == CLICK_TYPE_MIDDLE && player.isInCreativeMode();
}

protected void endQuickCraft() {
quickCraftStage = 0;
quickCraftSlots.clear();
}

/**
* Проверяет, можно ли вставить предмет в слот с учётом переполнения.
*
* @param slot          целевой слот (может быть null)
* @param stack         вставляемый стек
* @param allowOverflow если {@code true}, не проверяет превышение максимума
* @return {@code true} если вставка возможна
*/
public static boolean canInsertItemIntoSlot(@Nullable Slot slot, ItemStack stack, boolean allowOverflow) {
boolean slotEmpty = slot == null || !slot.hasStack();
if (!slotEmpty && ItemStack.areItemsAndComponentsEqual(stack, slot.getStack())) {
	return slot.getStack().getCount() + (allowOverflow ? 0 : stack.getCount()) <= stack.getMaxCount();
}

return slotEmpty;
}

/**
* Вычисляет размер стека для каждого слота при быстром крафтении.
* Режим 0 (левая кнопка): равномерное деление; 1 (правая): по 1; 2 (средняя): максимум.
*
* @param slots набор целевых слотов
* @param mode  режим распределения
* @param stack распределяемый стек
* @return количество предметов для каждого слота
*/
public static int calculateStackSize(Set<Slot> slots, int mode, ItemStack stack) {
return switch (mode) {
	case CLICK_TYPE_LEFT -> MathHelper.floor((float) stack.getCount() / slots.size());
	case CLICK_TYPE_RIGHT -> 1;
	case CLICK_TYPE_MIDDLE -> stack.getMaxCount();
	default -> stack.getCount();
};
}

public boolean canInsertIntoSlot(Slot slot) {
return true;
}

/**
* Вычисляет выходной сигнал компаратора для блока-инвентаря.
*
* @param entity блок-сущность (может быть null или не инвентарём)
* @return уровень сигнала от 0 до 15
*/
public static int calculateComparatorOutput(@Nullable BlockEntity entity) {
return entity instanceof Inventory inventory ? calculateComparatorOutput(inventory) : 0;
}

/**
* Вычисляет выходной сигнал компаратора для инвентаря.
* Формула: среднее заполнение слотов, линейно отображённое на диапазон 0–15.
*
* @param inventory инвентарь (может быть null)
* @return уровень сигнала от 0 до 15
*/
public static int calculateComparatorOutput(@Nullable Inventory inventory) {
if (inventory == null) {
	return 0;
}

float fillRatio = 0.0F;

for (int index = 0; index < inventory.size(); index++) {
	ItemStack stack = inventory.getStack(index);
	if (!stack.isEmpty()) {
		fillRatio += (float) stack.getCount() / inventory.getMaxCount(stack);
	}
}

fillRatio /= inventory.size();
return MathHelper.lerpPositive(fillRatio, 0, MAX_COMPARATOR_LEVEL);
}

public void setCursorStack(ItemStack stack) {
cursorStack = stack;
}

public ItemStack getCursorStack() {
return cursorStack;
}

public void disableSyncing() {
disableSync = true;
}

public void enableSyncing() {
disableSync = false;
}

/**
* Копирует состояние отслеживаемых слотов из другого обработчика для слотов,
* ссылающихся на один и тот же инвентарь и индекс.
* Используется при переоткрытии экрана без потери синхронизации.
*
* @param handler источник состояния
*/
public void copySharedSlots(ScreenHandler handler) {
Table<Inventory, Integer, Integer> table = HashBasedTable.create();

for (int index = 0; index < handler.slots.size(); index++) {
	Slot slot = handler.slots.get(index);
	table.put(slot.inventory, slot.getIndex(), index);
}

for (int index = 0; index < slots.size(); index++) {
	Slot slot = slots.get(index);
	Integer sourceIndex = table.get(slot.inventory, slot.getIndex());
	if (sourceIndex == null) {
		continue;
	}

	trackedStacks.set(index, handler.trackedStacks.get(sourceIndex));
	TrackedSlot sourceTracked = handler.trackedSlots.get(sourceIndex);
	TrackedSlot targetTracked = trackedSlots.get(index);
	if (sourceTracked instanceof TrackedSlot.Impl sourceImpl
			&& targetTracked instanceof TrackedSlot.Impl targetImpl) {
		targetImpl.copyFrom(sourceImpl);
	}
}
}

public OptionalInt getSlotIndex(Inventory inventory, int index) {
for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
	Slot slot = slots.get(slotIndex);
	if (slot.inventory == inventory && index == slot.getIndex()) {
		return OptionalInt.of(slotIndex);
	}
}

return OptionalInt.empty();
}

public int getRevision() {
return revision;
}

/**
* Увеличивает ревизию синхронизации и возвращает новое значение.
* Ревизия циклически ограничена значением {@link #MAX_REVISION}.
*
* @return новая ревизия
*/
public int nextRevision() {
revision = revision + 1 & MAX_REVISION;
return revision;
}
}
			
