package net.minecraft.entity.player;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
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
 * Инвентарь игрока: 36 основных слотов, хотбар (первые 9), слот внеруки и слоты экипировки.
 * Является реализацией {@link Inventory} и делегирует операции с экипировкой в {@link EntityEquipment}.
 */
public class PlayerInventory implements Inventory, Nameable {

	public static final int ITEM_USAGE_COOLDOWN = 5;
	public static final int MAIN_SIZE = 36;
	public static final int HOTBAR_SIZE = 9;
	public static final int OFF_HAND_SLOT = 40;
	public static final int BODY_SLOT = 41;
	public static final int SADDLE_SLOT = 42;
	public static final int NOT_FOUND = -1;

	public static final Int2ObjectMap<EquipmentSlot> EQUIPMENT_SLOTS = new Int2ObjectArrayMap<>(
		Map.of(
			EquipmentSlot.FEET.getOffsetEntitySlotId(MAIN_SIZE), EquipmentSlot.FEET,
			EquipmentSlot.LEGS.getOffsetEntitySlotId(MAIN_SIZE), EquipmentSlot.LEGS,
			EquipmentSlot.CHEST.getOffsetEntitySlotId(MAIN_SIZE), EquipmentSlot.CHEST,
			EquipmentSlot.HEAD.getOffsetEntitySlotId(MAIN_SIZE), EquipmentSlot.HEAD,
			OFF_HAND_SLOT, EquipmentSlot.OFFHAND,
			BODY_SLOT, EquipmentSlot.BODY,
			SADDLE_SLOT, EquipmentSlot.SADDLE
		)
	);

	private static final Text NAME = Text.translatable("container.inventory");

	private final DefaultedList<ItemStack> main = DefaultedList.ofSize(MAIN_SIZE, ItemStack.EMPTY);
	private int selectedSlot;
	public final PlayerEntity player;
	private final EntityEquipment equipment;
	private int changeCount;

	public PlayerInventory(PlayerEntity player, EntityEquipment equipment) {
		this.player = player;
		this.equipment = equipment;
	}

	public int getSelectedSlot() {
		return selectedSlot;
	}

	/**
	 * Устанавливает выбранный слот хотбара.
	 *
	 * @param slot индекс слота (0–8)
	 * @throws IllegalArgumentException если индекс вне диапазона хотбара
	 */
	public void setSelectedSlot(int slot) {
		if (!isValidHotbarIndex(slot)) {
			throw new IllegalArgumentException("Invalid selected slot");
		}

		selectedSlot = slot;
	}

	public ItemStack getSelectedStack() {
		return main.get(selectedSlot);
	}

	public ItemStack setSelectedStack(ItemStack stack) {
		return main.set(selectedSlot, stack);
	}

	public static int getHotbarSize() {
		return HOTBAR_SIZE;
	}

	public DefaultedList<ItemStack> getMainStacks() {
		return main;
	}

	private boolean canStackAddMore(ItemStack existingStack, ItemStack stack) {
		return !existingStack.isEmpty()
			&& ItemStack.areItemsAndComponentsEqual(existingStack, stack)
			&& existingStack.isStackable()
			&& existingStack.getCount() < getMaxCount(existingStack);
	}

	/**
	 * Возвращает первый пустой слот в основном инвентаре, или {@link #NOT_FOUND}.
	 */
	public int getEmptySlot() {
		for (int slot = 0; slot < main.size(); slot++) {
			if (main.get(slot).isEmpty()) {
				return slot;
			}
		}

		return NOT_FOUND;
	}

	/**
	 * Перемещает предмет в хотбар, освобождая место при необходимости.
	 * Выбирает подходящий слот через {@link #getSwappableHotbarSlot()}.
	 *
	 * @param stack предмет для помещения в хотбар
	 */
	public void swapStackWithHotbar(ItemStack stack) {
		setSelectedSlot(getSwappableHotbarSlot());

		if (!main.get(selectedSlot).isEmpty()) {
			int emptySlot = getEmptySlot();

			if (emptySlot != NOT_FOUND) {
				main.set(emptySlot, main.get(selectedSlot));
			}
		}

		main.set(selectedSlot, stack);
	}

	/**
	 * Меняет местами содержимое указанного слота и выбранного слота хотбара.
	 *
	 * @param slot слот для обмена с хотбаром
	 */
	public void swapSlotWithHotbar(int slot) {
		setSelectedSlot(getSwappableHotbarSlot());
		ItemStack temp = main.get(selectedSlot);
		main.set(selectedSlot, main.get(slot));
		main.set(slot, temp);
	}

	public static boolean isValidHotbarIndex(int slot) {
		return slot >= 0 && slot < HOTBAR_SIZE;
	}

	/**
	 * Ищет слот с предметом, идентичным по типу и компонентам.
	 *
	 * @param stack эталонный предмет
	 * @return индекс слота или {@link #NOT_FOUND}
	 */
	public int getSlotWithStack(ItemStack stack) {
		for (int slot = 0; slot < main.size(); slot++) {
			if (!main.get(slot).isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, main.get(slot))) {
				return slot;
			}
		}

		return NOT_FOUND;
	}

	/**
	 * Проверяет, пригоден ли предмет для автозаполнения слота (не повреждён, без зачарований и кастомного имени).
	 *
	 * @param stack проверяемый предмет
	 * @return {@code true} если предмет можно использовать для заполнения слота
	 */
	public static boolean usableWhenFillingSlot(ItemStack stack) {
		return !stack.isDamaged() && !stack.hasEnchantments() && !stack.contains(DataComponentTypes.CUSTOM_NAME);
	}

	/**
	 * Ищет слот с предметом указанного типа, пригодным для заполнения.
	 *
	 * @param item  тип предмета
	 * @param stack опциональный эталон для сравнения компонентов (может быть пустым)
	 * @return индекс слота или {@link #NOT_FOUND}
	 */
	public int getMatchingSlot(RegistryEntry<Item> item, ItemStack stack) {
		for (int slot = 0; slot < main.size(); slot++) {
			ItemStack candidate = main.get(slot);

			if (!candidate.isEmpty()
				&& candidate.itemMatches(item)
				&& usableWhenFillingSlot(candidate)
				&& (stack.isEmpty() || ItemStack.areItemsAndComponentsEqual(stack, candidate))) {
				return slot;
			}
		}

		return NOT_FOUND;
	}

	/**
	 * Находит наиболее подходящий слот хотбара для замены.
	 * Приоритет: пустой слот → слот без зачарований → текущий выбранный слот.
	 * Поиск начинается с текущего выбранного слота по кругу.
	 *
	 * @return индекс слота хотбара
	 */
	public int getSwappableHotbarSlot() {
		for (int offset = 0; offset < HOTBAR_SIZE; offset++) {
			int slot = (selectedSlot + offset) % HOTBAR_SIZE;

			if (main.get(slot).isEmpty()) {
				return slot;
			}
		}

		for (int offset = 0; offset < HOTBAR_SIZE; offset++) {
			int slot = (selectedSlot + offset) % HOTBAR_SIZE;

			if (!main.get(slot).hasEnchantments()) {
				return slot;
			}
		}

		return selectedSlot;
	}

	/**
	 * Удаляет предметы из инвентаря, курсора и крафтового инвентаря по предикату.
	 *
	 * @param shouldRemove     предикат отбора предметов для удаления
	 * @param maxCount         максимальное количество для удаления (0 = без ограничений)
	 * @param craftingInventory крафтовый инвентарь для дополнительного поиска
	 * @return суммарное количество удалённых предметов
	 */
	public int remove(Predicate<ItemStack> shouldRemove, int maxCount, Inventory craftingInventory) {
		boolean unlimited = maxCount == 0;
		int removed = 0;
		removed += Inventories.remove(this, shouldRemove, maxCount - removed, unlimited);
		removed += Inventories.remove(craftingInventory, shouldRemove, maxCount - removed, unlimited);

		ItemStack cursorStack = player.currentScreenHandler.getCursorStack();
		removed += Inventories.remove(cursorStack, shouldRemove, maxCount - removed, unlimited);

		if (cursorStack.isEmpty()) {
			player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
		}

		return removed;
	}

	/**
	 * Добавляет предмет в первый подходящий слот (с местом или пустой).
	 *
	 * @param stack предмет для добавления
	 * @return оставшееся количество, которое не удалось добавить
	 */
	private int addStack(ItemStack stack) {
		int slot = getOccupiedSlotWithRoomForStack(stack);

		if (slot == NOT_FOUND) {
			slot = getEmptySlot();
		}

		return slot == NOT_FOUND ? stack.getCount() : addStack(slot, stack);
	}

	/**
	 * Добавляет предмет в конкретный слот, учитывая максимальный стак.
	 *
	 * @param slot  целевой слот
	 * @param stack предмет для добавления
	 * @return оставшееся количество после добавления
	 */
	private int addStack(int slot, ItemStack stack) {
		int incoming = stack.getCount();
		ItemStack existing = getStack(slot);

		if (existing.isEmpty()) {
			existing = stack.copyWithCount(0);
			setStack(slot, existing);
		}

		int available = getMaxCount(existing) - existing.getCount();
		int toAdd = Math.min(incoming, available);

		if (toAdd == 0) {
			return incoming;
		}

		existing.increment(toAdd);
		existing.setBobbingAnimationTime(ITEM_USAGE_COOLDOWN);
		return incoming - toAdd;
	}

	/**
	 * Ищет слот с предметом того же типа, в котором есть место для добавления.
	 * Проверяет сначала выбранный слот, затем внеруку, затем весь основной инвентарь.
	 *
	 * @param stack предмет для добавления
	 * @return индекс слота или {@link #NOT_FOUND}
	 */
	public int getOccupiedSlotWithRoomForStack(ItemStack stack) {
		if (canStackAddMore(getStack(selectedSlot), stack)) {
			return selectedSlot;
		}

		if (canStackAddMore(getStack(OFF_HAND_SLOT), stack)) {
			return OFF_HAND_SLOT;
		}

		for (int slot = 0; slot < main.size(); slot++) {
			if (canStackAddMore(main.get(slot), stack)) {
				return slot;
			}
		}

		return NOT_FOUND;
	}

	/**
	 * Выполняет тиковое обновление всех предметов в основном инвентаре.
	 * Передаёт слот экипировки {@link EquipmentSlot#MAINHAND} только для выбранного слота.
	 */
	public void updateItems() {
		for (int slot = 0; slot < main.size(); slot++) {
			ItemStack stack = getStack(slot);

			if (!stack.isEmpty()) {
				stack.inventoryTick(
					player.getEntityWorld(),
					player,
					slot == selectedSlot ? EquipmentSlot.MAINHAND : null
				);
			}
		}
	}

	/**
	 * Вставляет предмет в инвентарь, выбирая слот автоматически.
	 *
	 * @param stack предмет для вставки
	 * @return {@code true} если хотя бы часть предмета была добавлена
	 */
	public boolean insertStack(ItemStack stack) {
		return insertStack(NOT_FOUND, stack);
	}

	/**
	 * Вставляет предмет в указанный слот (или автоматически при {@code slot == -1}).
	 * Повреждённые предметы помещаются целиком в один слот.
	 *
	 * @param slot  целевой слот или {@link #NOT_FOUND} для автовыбора
	 * @param stack предмет для вставки
	 * @return {@code true} если хотя бы часть предмета была добавлена
	 */
	public boolean insertStack(int slot, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		try {
			if (stack.isDamaged()) {
				int targetSlot = slot == NOT_FOUND ? getEmptySlot() : slot;

				if (targetSlot >= 0) {
					main.set(targetSlot, stack.copyAndEmpty());
					main.get(targetSlot).setBobbingAnimationTime(ITEM_USAGE_COOLDOWN);
					return true;
				}

				if (player.isInCreativeMode()) {
					stack.setCount(0);
					return true;
				}

				return false;
			}

			int prevCount;

			do {
				prevCount = stack.getCount();
				stack.setCount(slot == NOT_FOUND ? addStack(stack) : addStack(slot, stack));
			} while (!stack.isEmpty() && stack.getCount() < prevCount);

			if (stack.getCount() == prevCount && player.isInCreativeMode()) {
				stack.setCount(0);
				return true;
			}

			return stack.getCount() < prevCount;
		} catch (Throwable throwable) {
			CrashReport crashReport = CrashReport.create(throwable, "Adding item to inventory");
			CrashReportSection section = crashReport.addElement("Item being added");
			section.add("Item ID", Item.getRawId(stack.getItem()));
			section.add("Item data", stack.getDamage());
			section.add("Item name", () -> stack.getName().getString());
			throw new CrashException(crashReport);
		}
	}

	/**
	 * Предлагает предмет инвентарю, при невозможности добавить — бросает его на землю.
	 *
	 * @param stack предмет для добавления или выброса
	 */
	public void offerOrDrop(ItemStack stack) {
		offer(stack, true);
	}

	/**
	 * Добавляет предмет в инвентарь, при переполнении — бросает остаток на землю.
	 * При {@code notifiesClient == true} отправляет пакет обновления слота серверному игроку.
	 *
	 * @param stack           предмет для добавления
	 * @param notifiesClient  отправлять ли пакет клиенту
	 */
	public void offer(ItemStack stack, boolean notifiesClient) {
		while (!stack.isEmpty()) {
			int slot = getOccupiedSlotWithRoomForStack(stack);

			if (slot == NOT_FOUND) {
				slot = getEmptySlot();
			}

			if (slot == NOT_FOUND) {
				player.dropItem(stack, false);
				break;
			}

			int available = stack.getMaxCount() - getStack(slot).getCount();

			if (insertStack(slot, stack.split(available)) && notifiesClient
				&& player instanceof ServerPlayerEntity serverPlayer) {
				serverPlayer.networkHandler.sendPacket(createSlotSetPacket(slot));
			}
		}
	}

	/**
	 * Создаёт пакет синхронизации слота инвентаря для отправки клиенту.
	 *
	 * @param slot индекс слота
	 * @return пакет с копией содержимого слота
	 */
	public SetPlayerInventoryS2CPacket createSlotSetPacket(int slot) {
		return new SetPlayerInventoryS2CPacket(slot, getStack(slot).copy());
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		if (slot < main.size()) {
			return Inventories.splitStack(main, slot, amount);
		}

		EquipmentSlot equipmentSlot = EQUIPMENT_SLOTS.get(slot);

		if (equipmentSlot != null) {
			ItemStack stack = equipment.get(equipmentSlot);

			if (!stack.isEmpty()) {
				return stack.split(amount);
			}
		}

		return ItemStack.EMPTY;
	}

	/**
	 * Удаляет конкретный экземпляр предмета из инвентаря (по ссылке).
	 * Ищет сначала в основном инвентаре, затем в слотах экипировки.
	 *
	 * @param stack предмет для удаления (сравнение по ссылке {@code ==})
	 */
	public void removeOne(ItemStack stack) {
		for (int slot = 0; slot < main.size(); slot++) {
			if (main.get(slot) == stack) {
				main.set(slot, ItemStack.EMPTY);
				return;
			}
		}

		for (EquipmentSlot equipmentSlot : EQUIPMENT_SLOTS.values()) {
			if (equipment.get(equipmentSlot) == stack) {
				equipment.put(equipmentSlot, ItemStack.EMPTY);
				return;
			}
		}
	}

	@Override
	public ItemStack removeStack(int slot) {
		if (slot < main.size()) {
			ItemStack stack = main.get(slot);
			main.set(slot, ItemStack.EMPTY);
			return stack;
		}

		EquipmentSlot equipmentSlot = EQUIPMENT_SLOTS.get(slot);
		return equipmentSlot != null
			? equipment.put(equipmentSlot, ItemStack.EMPTY)
			: ItemStack.EMPTY;
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		if (slot < main.size()) {
			main.set(slot, stack);
		}

		EquipmentSlot equipmentSlot = EQUIPMENT_SLOTS.get(slot);

		if (equipmentSlot != null) {
			equipment.put(equipmentSlot, stack);
		}
	}

	/**
	 * Записывает непустые предметы основного инвентаря в список NBT.
	 *
	 * @param list приёмник данных
	 */
	public void writeData(WriteView.ListAppender<StackWithSlot> list) {
		for (int slot = 0; slot < main.size(); slot++) {
			ItemStack stack = main.get(slot);

			if (!stack.isEmpty()) {
				list.add(new StackWithSlot(slot, stack));
			}
		}
	}

	/**
	 * Читает предметы инвентаря из NBT-списка, игнорируя невалидные слоты.
	 *
	 * @param list источник данных
	 */
	public void readData(ReadView.TypedListReadView<StackWithSlot> list) {
		main.clear();

		for (StackWithSlot stackWithSlot : list) {
			if (stackWithSlot.isValidSlot(main.size())) {
				setStack(stackWithSlot.slot(), stackWithSlot.stack());
			}
		}
	}

	@Override
	public int size() {
		return main.size() + EQUIPMENT_SLOTS.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : main) {
			if (!stack.isEmpty()) {
				return false;
			}
		}

		for (EquipmentSlot equipmentSlot : EQUIPMENT_SLOTS.values()) {
			if (!equipment.get(equipmentSlot).isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		if (slot < main.size()) {
			return main.get(slot);
		}

		EquipmentSlot equipmentSlot = EQUIPMENT_SLOTS.get(slot);
		return equipmentSlot != null ? equipment.get(equipmentSlot) : ItemStack.EMPTY;
	}

	@Override
	public Text getName() {
		return NAME;
	}

	/**
	 * Выбрасывает все предметы из основного инвентаря и экипировки на землю.
	 */
	public void dropAll() {
		for (int slot = 0; slot < main.size(); slot++) {
			ItemStack stack = main.get(slot);

			if (!stack.isEmpty()) {
				player.dropItem(stack, true, false);
				main.set(slot, ItemStack.EMPTY);
			}
		}

		equipment.dropAll(player);
	}

	@Override
	public void markDirty() {
		changeCount++;
	}

	public int getChangeCount() {
		return changeCount;
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}

	/**
	 * Проверяет наличие предмета с идентичными типом и компонентами.
	 *
	 * @param stack эталонный предмет
	 * @return {@code true} если такой предмет найден
	 */
	public boolean contains(ItemStack stack) {
		for (ItemStack candidate : this) {
			if (!candidate.isEmpty() && ItemStack.areItemsAndComponentsEqual(candidate, stack)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Проверяет наличие предмета с указанным тегом.
	 *
	 * @param tag тег предмета
	 * @return {@code true} если найден предмет с данным тегом
	 */
	public boolean contains(TagKey<Item> tag) {
		for (ItemStack stack : this) {
			if (!stack.isEmpty() && stack.isIn(tag)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Проверяет наличие предмета, удовлетворяющего предикату.
	 *
	 * @param predicate условие поиска
	 * @return {@code true} если найден подходящий предмет
	 */
	public boolean contains(Predicate<ItemStack> predicate) {
		for (ItemStack stack : this) {
			if (predicate.test(stack)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Копирует содержимое другого инвентаря в этот, включая выбранный слот.
	 *
	 * @param other исходный инвентарь
	 */
	public void clone(PlayerInventory other) {
		for (int slot = 0; slot < size(); slot++) {
			setStack(slot, other.getStack(slot));
		}

		setSelectedSlot(other.getSelectedSlot());
	}

	@Override
	public void clear() {
		main.clear();
		equipment.clear();
	}

	/**
	 * Заполняет {@link RecipeFinder} предметами из основного инвентаря для поиска рецептов.
	 *
	 * @param finder объект поиска рецептов
	 */
	public void populateRecipeFinder(RecipeFinder finder) {
		for (ItemStack stack : main) {
			finder.addInputIfUsable(stack);
		}
	}

	/**
	 * Выбрасывает выбранный предмет из хотбара.
	 *
	 * @param entireStack {@code true} — выбросить весь стак, {@code false} — только один предмет
	 * @return выброшенный предмет или {@link ItemStack#EMPTY}
	 */
	public ItemStack dropSelectedItem(boolean entireStack) {
		ItemStack stack = getSelectedStack();
		return stack.isEmpty()
			? ItemStack.EMPTY
			: removeStack(selectedSlot, entireStack ? stack.getCount() : 1);
	}
}
