package net.minecraft.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.RecipeBookType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.slot.FurnaceFuelSlot;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;

/**
 * Базовый обработчик экрана печи (обычная, доменная, коптильня).
 * <p>
 * Управляет тремя слотами: ингредиент (0), топливо (1), результат (2).
 * Синхронизирует прогресс плавки и уровень топлива через {@link PropertyDelegate}.
 */
public abstract class AbstractFurnaceScreenHandler extends AbstractRecipeScreenHandler {

	public static final int INGREDIENT_SLOT = 0;
	public static final int FUEL_SLOT = 1;
	public static final int RESULT_SLOT = 2;
	public static final int INVENTORY_START = 3;
	public static final int FURNACE_SLOT_COUNT = 4;

	private static final int INVENTORY_SLOT_COUNT = 3;
	private static final int INVENTORY_END = 30;
	private static final int HOTBAR_START = 30;
	private static final int HOTBAR_END = 39;

	/** Индекс свойства: текущее время горения топлива. */
	private static final int PROP_FUEL_TIME = 0;
	/** Индекс свойства: максимальное время горения текущего топлива. */
	private static final int PROP_FUEL_MAX = 1;
	/** Индекс свойства: текущий прогресс плавки. */
	private static final int PROP_COOK_PROGRESS = 2;
	/** Индекс свойства: полное время плавки текущего рецепта. */
	private static final int PROP_COOK_TOTAL = 3;
	/** Значение по умолчанию для максимального времени горения (если не задано). */
	private static final int DEFAULT_FUEL_MAX = 200;

	final Inventory inventory;
	private final PropertyDelegate propertyDelegate;
	protected final World world;
	private final RecipeType<? extends AbstractCookingRecipe> recipeType;
	private final RecipePropertySet recipePropertySet;
	private final RecipeBookType category;

	protected AbstractFurnaceScreenHandler(
			ScreenHandlerType<?> type,
			RecipeType<? extends AbstractCookingRecipe> recipeType,
			RegistryKey<RecipePropertySet> recipePropertySetKey,
			RecipeBookType category,
			int syncId,
			PlayerInventory playerInventory
	) {
		this(
				type,
				recipeType,
				recipePropertySetKey,
				category,
				syncId,
				playerInventory,
				new SimpleInventory(INVENTORY_SLOT_COUNT),
				new ArrayPropertyDelegate(FURNACE_SLOT_COUNT)
		);
	}

	protected AbstractFurnaceScreenHandler(
			ScreenHandlerType<?> type,
			RecipeType<? extends AbstractCookingRecipe> recipeType,
			RegistryKey<RecipePropertySet> recipePropertySetKey,
			RecipeBookType category,
			int syncId,
			PlayerInventory playerInventory,
			Inventory inventory,
			PropertyDelegate propertyDelegate
	) {
		super(type, syncId);
		checkSize(inventory, INVENTORY_SLOT_COUNT);
		checkDataCount(propertyDelegate, FURNACE_SLOT_COUNT);
		this.recipeType = recipeType;
		this.category = category;
		this.inventory = inventory;
		this.propertyDelegate = propertyDelegate;
		this.world = playerInventory.player.getEntityWorld();
		this.recipePropertySet = world.getRecipeManager().getPropertySet(recipePropertySetKey);
		addSlot(new Slot(inventory, INGREDIENT_SLOT, 56, 17));
		addSlot(new FurnaceFuelSlot(this, inventory, FUEL_SLOT, 56, 53));
		addSlot(new FurnaceOutputSlot(playerInventory.player, inventory, RESULT_SLOT, 116, 35));
		addPlayerSlots(playerInventory, 8, 84);
		addProperties(propertyDelegate);
	}

	@Override
	public void populateRecipeFinder(RecipeFinder finder) {
		if (inventory instanceof RecipeInputProvider provider) {
			provider.provideRecipeInputs(finder);
		}
	}

	public Slot getOutputSlot() {
		return slots.get(RESULT_SLOT);
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

		if (slot == RESULT_SLOT) {
			if (!insertItem(stack, INVENTORY_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}

			sourceSlot.onQuickTransfer(stack, original);
		} else if (slot != FUEL_SLOT && slot != INGREDIENT_SLOT) {
			if (isSmeltable(stack)) {
				if (!insertItem(stack, INGREDIENT_SLOT, INGREDIENT_SLOT + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (isFuel(stack)) {
				if (!insertItem(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
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
			}
		} else {
			if (!insertItem(stack, INVENTORY_START, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
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

		sourceSlot.onTakeItem(player, stack);
		return original;
	}

	protected boolean isSmeltable(ItemStack stack) {
		return recipePropertySet.canUse(stack);
	}

	public boolean isFuel(ItemStack item) {
		return world.getFuelRegistry().isFuel(item);
	}

	/**
	 * Возвращает нормализованный прогресс плавки в диапазоне [0, 1].
	 * Используется клиентом для отрисовки анимации стрелки.
	 */
	public float getCookProgress() {
		int progress = propertyDelegate.get(PROP_COOK_PROGRESS);
		int total = propertyDelegate.get(PROP_COOK_TOTAL);
		return total != 0 && progress != 0
				? MathHelper.clamp((float) progress / total, 0.0F, 1.0F)
				: 0.0F;
	}

	/**
	 * Возвращает нормализованный уровень топлива в диапазоне [0, 1].
	 * Используется клиентом для отрисовки анимации огня.
	 */
	public float getFuelProgress() {
		int fuelMax = propertyDelegate.get(PROP_FUEL_MAX);
		if (fuelMax == 0) {
			fuelMax = DEFAULT_FUEL_MAX;
		}

		return MathHelper.clamp((float) propertyDelegate.get(PROP_FUEL_TIME) / fuelMax, 0.0F, 1.0F);
	}

	public boolean isBurning() {
		return propertyDelegate.get(PROP_FUEL_TIME) > 0;
	}

	@Override
	public RecipeBookType getCategory() {
		return category;
	}

	@Override
	public PostFillAction fillInputSlots(
			boolean craftAll, boolean creative, RecipeEntry<?> recipe, ServerWorld world, PlayerInventory inventory
	) {
		List<Slot> slots = List.of(getSlot(INGREDIENT_SLOT), getSlot(RESULT_SLOT));
		return InputSlotFiller.fill(
				new InputSlotFiller.Handler<AbstractCookingRecipe>() {
					@Override
					public void populateRecipeFinder(RecipeFinder finder) {
						AbstractFurnaceScreenHandler.this.populateRecipeFinder(finder);
					}

					@Override
					public void clear() {
						slots.forEach(slot -> slot.setStackNoCallbacks(ItemStack.EMPTY));
					}

					@Override
					public boolean matches(RecipeEntry<AbstractCookingRecipe> entry) {
						return entry.value()
								.matches(
										new SingleStackRecipeInput(
												AbstractFurnaceScreenHandler.this.inventory.getStack(INGREDIENT_SLOT)
										),
										world
								);
					}
				},
				1,
				1,
				List.of(getSlot(INGREDIENT_SLOT)),
				slots,
				inventory,
				(RecipeEntry<AbstractCookingRecipe>) recipe,
				craftAll,
				creative
		);
	}
}
