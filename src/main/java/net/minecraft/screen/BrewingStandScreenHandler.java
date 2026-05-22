package net.minecraft.screen;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Обработчик экрана стойки для зелий.
 * <p>
 * Управляет пятью слотами: три слота зелий (0–2), слот ингредиента (3), слот топлива (4).
 * Синхронизирует время варки и уровень топлива через {@link PropertyDelegate}.
 */
public class BrewingStandScreenHandler extends ScreenHandler {

	static final Identifier EMPTY_BREWING_FUEL_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/brewing_fuel");
	static final Identifier EMPTY_POTION_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/potion");

	private static final int FIRST_POTION_SLOT = 0;
	private static final int LAST_POTION_SLOT = 2;
	private static final int INGREDIENT_SLOT = 3;
	private static final int FUEL_SLOT = 4;
	private static final int INVENTORY_SIZE = 5;
	private static final int PROPERTY_COUNT = 2;
	private static final int INVENTORY_START = 5;
	private static final int INVENTORY_END = 32;
	private static final int HOTBAR_START = 32;
	private static final int HOTBAR_END = 41;
	private static final int PROP_BREW_TIME = 0;
	private static final int PROP_FUEL = 1;

	private final Inventory inventory;
	private final PropertyDelegate propertyDelegate;
	private final Slot ingredientSlot;

	public BrewingStandScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new SimpleInventory(INVENTORY_SIZE), new ArrayPropertyDelegate(PROPERTY_COUNT));
	}

	public BrewingStandScreenHandler(
			int syncId,
			PlayerInventory playerInventory,
			Inventory inventory,
			PropertyDelegate propertyDelegate
	) {
		super(ScreenHandlerType.BREWING_STAND, syncId);
		checkSize(inventory, INVENTORY_SIZE);
		checkDataCount(propertyDelegate, PROPERTY_COUNT);
		this.inventory = inventory;
		this.propertyDelegate = propertyDelegate;
		BrewingRecipeRegistry brewingRegistry = playerInventory.player.getEntityWorld().getBrewingRecipeRegistry();
		addSlot(new PotionSlot(inventory, FIRST_POTION_SLOT, 56, 51));
		addSlot(new PotionSlot(inventory, 1, 79, 58));
		addSlot(new PotionSlot(inventory, LAST_POTION_SLOT, 102, 51));
		ingredientSlot = addSlot(new IngredientSlot(brewingRegistry, inventory, INGREDIENT_SLOT, 79, 17));
		addSlot(new FuelSlot(inventory, FUEL_SLOT, 17, 17));
		addProperties(propertyDelegate);
		addPlayerSlots(playerInventory, 8, 84);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return inventory.canPlayerUse(player);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		ItemStack original = ItemStack.EMPTY;
		Slot sourceSlot = slots.get(slot);

		if (sourceSlot == null || !sourceSlot.hasStack()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = sourceSlot.getStack();
		original = stack.copy();

		boolean isBrewingSlot = slot >= FIRST_POTION_SLOT && slot <= LAST_POTION_SLOT
				|| slot == INGREDIENT_SLOT
				|| slot == FUEL_SLOT;

		if (isBrewingSlot) {
			if (!insertItem(stack, INVENTORY_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}

			sourceSlot.onQuickTransfer(stack, original);
		} else {
			if (FuelSlot.matches(stack)) {
				if (!insertItem(stack, FUEL_SLOT, FUEL_SLOT + 1, false)
						&& ingredientSlot.canInsert(stack)
						&& !insertItem(stack, INGREDIENT_SLOT, INGREDIENT_SLOT + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (ingredientSlot.canInsert(stack)) {
				if (!insertItem(stack, INGREDIENT_SLOT, INGREDIENT_SLOT + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (PotionSlot.matches(stack)) {
				if (!insertItem(stack, FIRST_POTION_SLOT, LAST_POTION_SLOT + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (slot >= INVENTORY_START && slot < INVENTORY_END) {
				if (!insertItem(stack, HOTBAR_START, HOTBAR_END, false)) {
					return ItemStack.EMPTY;
				}
			} else if (slot >= HOTBAR_START && slot < HOTBAR_END) {
				if (!insertItem(stack, INVENTORY_START, INVENTORY_END, false)) {
					return ItemStack.EMPTY;
				}
			} else {
				if (!insertItem(stack, INVENTORY_START, HOTBAR_END, false)) {
					return ItemStack.EMPTY;
				}
			}
		}

		if (stack.isEmpty()) {
			sourceSlot.setStack(ItemStack.EMPTY);
		} else {
			sourceSlot.markDirty();
		}

		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}

		sourceSlot.onTakeItem(player, original);
		return original;
	}

	public int getFuel() {
		return propertyDelegate.get(PROP_FUEL);
	}

	public int getBrewTime() {
		return propertyDelegate.get(PROP_BREW_TIME);
	}

	static class FuelSlot extends Slot {

		public FuelSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return matches(stack);
		}

		public static boolean matches(ItemStack stack) {
			return stack.isIn(ItemTags.BREWING_FUEL);
		}

		@Override
		public Identifier getBackgroundSprite() {
			return BrewingStandScreenHandler.EMPTY_BREWING_FUEL_SLOT_TEXTURE;
		}
	}

	static class IngredientSlot extends Slot {

		private final BrewingRecipeRegistry brewingRegistry;

		public IngredientSlot(
				BrewingRecipeRegistry brewingRegistry,
				Inventory inventory,
				int index,
				int x,
				int y
		) {
			super(inventory, index, x, y);
			this.brewingRegistry = brewingRegistry;
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return brewingRegistry.isValidIngredient(stack);
		}
	}

	static class PotionSlot extends Slot {

		public PotionSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return matches(stack);
		}

		@Override
		public int getMaxItemCount() {
			return 1;
		}

		@Override
		public void onTakeItem(PlayerEntity player, ItemStack stack) {
			Optional<RegistryEntry<Potion>> potion = stack
					.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
					.potion();

			if (potion.isPresent() && player instanceof ServerPlayerEntity serverPlayer) {
				Criteria.BREWED_POTION.trigger(serverPlayer, potion.get());
			}

			super.onTakeItem(player, stack);
		}

		public static boolean matches(ItemStack stack) {
			return stack.isOf(Items.POTION)
					|| stack.isOf(Items.SPLASH_POTION)
					|| stack.isOf(Items.LINGERING_POTION)
					|| stack.isOf(Items.GLASS_BOTTLE);
		}

		@Override
		public Identifier getBackgroundSprite() {
			return BrewingStandScreenHandler.EMPTY_POTION_SLOT_TEXTURE;
		}
	}
}
