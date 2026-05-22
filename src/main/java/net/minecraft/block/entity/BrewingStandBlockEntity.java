package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Блок-сущность стойки для зелий. Управляет процессом варки зелий,
 * расходом топлива (blaze powder) и синхронизацией состояния слотов с блоком.
 */
public class BrewingStandBlockEntity extends LockableContainerBlockEntity implements SidedInventory {

	private static final int INPUT_SLOT_INDEX = 3;
	private static final int FUEL_SLOT_INDEX = 4;
	private static final int[] TOP_SLOTS = new int[]{3};
	private static final int[] BOTTOM_SLOTS = new int[]{0, 1, 2, 3};
	private static final int[] SIDE_SLOTS = new int[]{0, 1, 2, 4};
	public static final int MAX_FUEL_USES = 20;
	public static final int BREW_TIME_PROPERTY_INDEX = 0;
	public static final int FUEL_PROPERTY_INDEX = 1;
	public static final int PROPERTY_COUNT = 2;
	private static final short DEFAULT_BREW_TIME = 0;
	private static final byte DEFAULT_FUEL = 0;
	private static final Text CONTAINER_NAME_TEXT = Text.translatable("container.brewing");
	private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);
	int brewTime;
	private boolean[] slotsEmptyLastTick;
	private Item itemBrewing;
	int fuel;
	protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
		@Override
		public int get(int index) {
			return switch (index) {
				case BREW_TIME_PROPERTY_INDEX -> brewTime;
				case FUEL_PROPERTY_INDEX -> fuel;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			switch (index) {
				case BREW_TIME_PROPERTY_INDEX -> brewTime = value;
				case FUEL_PROPERTY_INDEX -> fuel = value;
			}
		}

		@Override
		public int size() {
			return PROPERTY_COUNT;
		}
	};

	public BrewingStandBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.BREWING_STAND, pos, state);
	}

	@Override
	protected Text getContainerName() {
		return CONTAINER_NAME_TEXT;
	}

	@Override
	public int size() {
		return inventory.size();
	}

	@Override
	protected DefaultedList<ItemStack> getHeldStacks() {
		return inventory;
	}

	@Override
	protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
		this.inventory = inventory;
	}

	public static void tick(World world, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity) {
		ItemStack fuelStack = blockEntity.inventory.get(FUEL_SLOT_INDEX);

		if (blockEntity.fuel <= 0 && fuelStack.isIn(ItemTags.BREWING_FUEL)) {
			blockEntity.fuel = MAX_FUEL_USES;
			fuelStack.decrement(1);
			markDirty(world, pos, state);
		}

		boolean canCraft = canCraft(world.getBrewingRecipeRegistry(), blockEntity.inventory);
		boolean isBrewing = blockEntity.brewTime > 0;
		ItemStack ingredientStack = blockEntity.inventory.get(INPUT_SLOT_INDEX);

		if (isBrewing) {
			blockEntity.brewTime--;
			boolean brewFinished = blockEntity.brewTime == 0;

			if (brewFinished && canCraft) {
				craft(world, pos, blockEntity.inventory);
			} else if (!canCraft || !ingredientStack.isOf(blockEntity.itemBrewing)) {
				blockEntity.brewTime = 0;
			}

			markDirty(world, pos, state);
		} else if (canCraft && blockEntity.fuel > 0) {
			blockEntity.fuel--;
			blockEntity.brewTime = 400;
			blockEntity.itemBrewing = ingredientStack.getItem();
			markDirty(world, pos, state);
		}

		boolean[] slotsEmpty = blockEntity.getSlotsEmpty();

		if (!Arrays.equals(slotsEmpty, blockEntity.slotsEmptyLastTick)) {
			blockEntity.slotsEmptyLastTick = slotsEmpty;

			if (!(state.getBlock() instanceof BrewingStandBlock)) {
				return;
			}

			BlockState updatedState = state;

			for (int slot = 0; slot < BrewingStandBlock.BOTTLE_PROPERTIES.length; slot++) {
				updatedState = updatedState.with(BrewingStandBlock.BOTTLE_PROPERTIES[slot], slotsEmpty[slot]);
			}

			world.setBlockState(pos, updatedState, 2);
		}
	}

	private static final int BOTTLE_SLOT_COUNT = 3;
	private static final int BREW_TOTAL_TICKS = 400;

	private boolean[] getSlotsEmpty() {
		boolean[] result = new boolean[BOTTLE_SLOT_COUNT];

		for (int slot = 0; slot < BOTTLE_SLOT_COUNT; slot++) {
			if (!inventory.get(slot).isEmpty()) {
				result[slot] = true;
			}
		}

		return result;
	}

	private static boolean canCraft(BrewingRecipeRegistry registry, DefaultedList<ItemStack> slots) {
		ItemStack ingredient = slots.get(INPUT_SLOT_INDEX);

		if (ingredient.isEmpty()) {
			return false;
		}

		if (!registry.isValidIngredient(ingredient)) {
			return false;
		}

		for (int slot = 0; slot < BOTTLE_SLOT_COUNT; slot++) {
			ItemStack bottle = slots.get(slot);
			if (!bottle.isEmpty() && registry.hasRecipe(bottle, ingredient)) {
				return true;
			}
		}

		return false;
	}

	private static void craft(World world, BlockPos pos, DefaultedList<ItemStack> slots) {
		ItemStack ingredient = slots.get(INPUT_SLOT_INDEX);
		BrewingRecipeRegistry registry = world.getBrewingRecipeRegistry();

		for (int slot = 0; slot < BOTTLE_SLOT_COUNT; slot++) {
			slots.set(slot, registry.craft(ingredient, slots.get(slot)));
		}

		ingredient.decrement(1);
		ItemStack remainder = ingredient.getItem().getRecipeRemainder();

		if (!remainder.isEmpty()) {
			if (ingredient.isEmpty()) {
				ingredient = remainder;
			} else {
				ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), remainder);
			}
		}

		slots.set(INPUT_SLOT_INDEX, ingredient);
		world.syncWorldEvent(1035, pos, 0);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		inventory = DefaultedList.ofSize(size(), ItemStack.EMPTY);
		Inventories.readData(view, inventory);
		brewTime = view.getShort("BrewTime", DEFAULT_BREW_TIME);

		if (brewTime > 0) {
			itemBrewing = inventory.get(INPUT_SLOT_INDEX).getItem();
		}

		fuel = view.getByte("Fuel", DEFAULT_FUEL);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putShort("BrewTime", (short) brewTime);
		Inventories.writeData(view, inventory);
		view.putByte("Fuel", (byte) fuel);
	}

	@Override
	public boolean isValid(int slot, ItemStack stack) {
		if (slot == INPUT_SLOT_INDEX) {
			BrewingRecipeRegistry registry = world != null ? world.getBrewingRecipeRegistry() : BrewingRecipeRegistry.EMPTY;
			return registry.isValidIngredient(stack);
		}

		if (slot == FUEL_SLOT_INDEX) {
			return stack.isIn(ItemTags.BREWING_FUEL);
		}

		return (stack.isOf(Items.POTION)
				|| stack.isOf(Items.SPLASH_POTION)
				|| stack.isOf(Items.LINGERING_POTION)
				|| stack.isOf(Items.GLASS_BOTTLE))
				&& getStack(slot).isEmpty();
	}

	@Override
	public int[] getAvailableSlots(Direction side) {
		if (side == Direction.UP) {
			return TOP_SLOTS;
		}

		return side == Direction.DOWN ? BOTTOM_SLOTS : SIDE_SLOTS;
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
		return isValid(slot, stack);
	}

	@Override
	public boolean canExtract(int slot, ItemStack stack, Direction dir) {
		return slot == INPUT_SLOT_INDEX ? stack.isOf(Items.GLASS_BOTTLE) : true;
	}

	@Override
	protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return new BrewingStandScreenHandler(syncId, playerInventory, this, propertyDelegate);
	}
}
