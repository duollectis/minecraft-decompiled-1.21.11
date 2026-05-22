package net.minecraft.screen;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.RecipeBookType;
import net.minecraft.screen.slot.ArmorSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

/**
 * Обработчик экрана инвентаря игрока.
 *
 * <p>Управляет 2×2 сеткой крафта, четырьмя слотами брони, слотом
 * второй руки и стандартным инвентарём. Слоты расположены в следующем
 * порядке: результат крафта (0), ячейки крафта (1–4), броня (5–8),
 * инвентарь (9–35), хотбар (36–44), вторая рука (45).</p>
 */
public class PlayerScreenHandler extends AbstractCraftingScreenHandler {

	public static final int CRAFTING_GRID_RESULT_SLOT = 0;
	public static final int CRAFTING_RESULT_ID = 0;
	private static final int CRAFTING_GRID_WIDTH = 2;
	private static final int CRAFTING_GRID_HEIGHT = 2;
	public static final int CRAFTING_INPUT_START = 1;
	public static final int CRAFTING_INPUT_COUNT = 4;
	public static final int CRAFTING_INPUT_END = 5;
	public static final int EQUIPMENT_START = 5;
	public static final int EQUIPMENT_COUNT = 4;
	public static final int EQUIPMENT_END = 9;
	public static final int INVENTORY_START = 9;
	public static final int INVENTORY_END = 36;
	public static final int HOTBAR_START = 36;
	public static final int HOTBAR_END = 45;
	public static final int OFFHAND_ID = 45;
	private static final int OFFHAND_SLOT_BOUND = 46;
	private static final int ARMOR_SLOT_GUI_INDEX = 8;
	private static final int ARMOR_SLOT_X = 8;
	private static final int ARMOR_SLOT_Y_START = 8;
	private static final int ARMOR_SLOT_STEP = 18;
	private static final int RESULT_SLOT_X = 154;
	private static final int RESULT_SLOT_Y = 28;
	private static final int INPUT_SLOTS_X = 98;
	private static final int INPUT_SLOTS_Y = 18;
	private static final int PLAYER_SLOTS_X = 8;
	private static final int PLAYER_SLOTS_Y = 84;
	private static final int OFFHAND_SLOT_X = 77;
	private static final int OFFHAND_SLOT_Y = 62;
	private static final int OFFHAND_INVENTORY_INDEX = 40;

	public static final Identifier EMPTY_HELMET_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/helmet");
	public static final Identifier EMPTY_CHESTPLATE_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/chestplate");
	public static final Identifier EMPTY_LEGGINGS_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/leggings");
	public static final Identifier EMPTY_BOOTS_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/boots");
	public static final Identifier EMPTY_OFF_HAND_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/shield");

	private static final Map<EquipmentSlot, Identifier> EMPTY_ARMOR_SLOT_TEXTURES = Map.of(
		EquipmentSlot.FEET, EMPTY_BOOTS_SLOT_TEXTURE,
		EquipmentSlot.LEGS, EMPTY_LEGGINGS_SLOT_TEXTURE,
		EquipmentSlot.CHEST, EMPTY_CHESTPLATE_SLOT_TEXTURE,
		EquipmentSlot.HEAD, EMPTY_HELMET_SLOT_TEXTURE
	);

	private static final EquipmentSlot[] EQUIPMENT_SLOT_ORDER = {
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	public final boolean onServer;
	private final PlayerEntity owner;

	public PlayerScreenHandler(PlayerInventory inventory, boolean onServer, PlayerEntity owner) {
		super(null, 0, CRAFTING_GRID_WIDTH, CRAFTING_GRID_HEIGHT);
		this.onServer = onServer;
		this.owner = owner;

		addResultSlot(owner, RESULT_SLOT_X, RESULT_SLOT_Y);
		addInputSlots(INPUT_SLOTS_X, INPUT_SLOTS_Y);

		for (int armorIndex = 0; armorIndex < EQUIPMENT_COUNT; armorIndex++) {
			EquipmentSlot equipmentSlot = EQUIPMENT_SLOT_ORDER[armorIndex];
			Identifier texture = EMPTY_ARMOR_SLOT_TEXTURES.get(equipmentSlot);
			addSlot(new ArmorSlot(
				inventory,
				owner,
				equipmentSlot,
				equipmentSlot.getOffsetEntitySlotId(PlayerInventory.MAIN_SIZE),
				ARMOR_SLOT_X,
				ARMOR_SLOT_Y_START + armorIndex * ARMOR_SLOT_STEP,
				texture
			));
		}

		addPlayerSlots(inventory, PLAYER_SLOTS_X, PLAYER_SLOTS_Y);
		addSlot(new Slot(inventory, OFFHAND_INVENTORY_INDEX, OFFHAND_SLOT_X, OFFHAND_SLOT_Y) {
			@Override
			public void setStack(ItemStack stack, ItemStack previousStack) {
				owner.onEquipStack(EquipmentSlot.OFFHAND, previousStack, stack);
				super.setStack(stack, previousStack);
			}

			@Override
			public Identifier getBackgroundSprite() {
				return EMPTY_OFF_HAND_SLOT_TEXTURE;
			}
		});
	}

	public static boolean isInHotbar(int slot) {
		return slot >= INVENTORY_END && slot < HOTBAR_END || slot == OFFHAND_ID;
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		if (owner.getEntityWorld() instanceof ServerWorld serverWorld) {
			CraftingScreenHandler.updateResult(
				this,
				serverWorld,
				owner,
				craftingInventory,
				craftingResultInventory,
				null
			);
		}
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		craftingResultInventory.clear();

		if (player.getEntityWorld().isClient()) {
			return;
		}

		dropInventory(player, craftingInventory);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return true;
	}

	/**
	 * Быстрое перемещение предмета (Shift+Click) с учётом всех зон инвентаря игрока.
	 *
	 * <p>Логика приоритетов:
	 * <ul>
	 *   <li>Результат крафта (слот 0) → инвентарь+хотбар, с вызовом {@code onQuickTransfer}</li>
	 *   <li>Ячейки крафта (1–4) и броня (5–8) → инвентарь+хотбар</li>
	 *   <li>Предмет брони → соответствующий слот брони</li>
	 *   <li>Предмет второй руки → слот второй руки</li>
	 *   <li>Инвентарь (9–35) → хотбар</li>
	 *   <li>Хотбар (36–44) → инвентарь</li>
	 * </ul></p>
	 */
	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		Slot sourceSlot = slots.get(slot);

		if (sourceSlot == null || !sourceSlot.hasStack()) {
			return ItemStack.EMPTY;
		}

		ItemStack slotStack = sourceSlot.getStack();
		ItemStack original = slotStack.copy();
		EquipmentSlot equipmentSlot = player.getPreferredEquipmentSlot(original);

		if (slot == 0) {
			if (!insertItem(slotStack, INVENTORY_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}

			sourceSlot.onQuickTransfer(slotStack, original);
		} else if (slot >= CRAFTING_INPUT_START && slot < EQUIPMENT_END) {
			if (!insertItem(slotStack, INVENTORY_START, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR
			&& !slots.get(ARMOR_SLOT_GUI_INDEX - equipmentSlot.getEntitySlotId()).hasStack()
		) {
			int armorSlotIndex = ARMOR_SLOT_GUI_INDEX - equipmentSlot.getEntitySlotId();

			if (!insertItem(slotStack, armorSlotIndex, armorSlotIndex + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (equipmentSlot == EquipmentSlot.OFFHAND && !slots.get(OFFHAND_ID).hasStack()) {
			if (!insertItem(slotStack, HOTBAR_END, OFFHAND_SLOT_BOUND, false)) {
				return ItemStack.EMPTY;
			}
		} else if (slot >= INVENTORY_START && slot < INVENTORY_END) {
			if (!insertItem(slotStack, INVENTORY_END, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (slot >= INVENTORY_END && slot < HOTBAR_END) {
			if (!insertItem(slotStack, INVENTORY_START, INVENTORY_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!insertItem(slotStack, INVENTORY_START, HOTBAR_END, false)) {
			return ItemStack.EMPTY;
		}

		if (slotStack.isEmpty()) {
			sourceSlot.setStack(ItemStack.EMPTY, original);
		} else {
			sourceSlot.markDirty();
		}

		if (slotStack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}

		sourceSlot.onTakeItem(player, slotStack);

		if (slot == 0) {
			player.dropItem(slotStack, false);
		}

		return original;
	}

	@Override
	public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
		return slot.inventory != craftingResultInventory && super.canInsertIntoSlot(stack, slot);
	}

	@Override
	public Slot getOutputSlot() {
		return slots.get(0);
	}

	@Override
	public List<Slot> getInputSlots() {
		return slots.subList(CRAFTING_INPUT_START, CRAFTING_INPUT_END);
	}

	public RecipeInputInventory getCraftingInput() {
		return craftingInventory;
	}

	@Override
	public RecipeBookType getCategory() {
		return RecipeBookType.CRAFTING;
	}

	@Override
	protected PlayerEntity getPlayer() {
		return owner;
	}
}
