package net.minecraft.block.entity;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.FuelRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Базовая реализация блок-сущности печи (обычная, доменная, коптильня).
 * <p>
 * Управляет тремя слотами: входным ({@code INPUT_SLOT_INDEX}), топливным ({@code FUEL_SLOT_INDEX})
 * и выходным ({@code OUTPUT_SLOT_INDEX}). Тик-логика вынесена в статический метод {@link #tick}
 * для совместимости с {@link BlockEntityTicker}.
 */
public abstract class AbstractFurnaceBlockEntity extends LockableContainerBlockEntity implements SidedInventory, RecipeUnlocker, RecipeInputProvider {

	protected static final int INPUT_SLOT_INDEX = 0;
	protected static final int FUEL_SLOT_INDEX = 1;
	protected static final int OUTPUT_SLOT_INDEX = 2;
	public static final int BURN_TIME_PROPERTY_INDEX = 0;
	private static final int[] TOP_SLOTS = new int[]{INPUT_SLOT_INDEX};
	private static final int[] BOTTOM_SLOTS = new int[]{OUTPUT_SLOT_INDEX, FUEL_SLOT_INDEX};
	private static final int[] SIDE_SLOTS = new int[]{FUEL_SLOT_INDEX};
	public static final int FUEL_TIME_PROPERTY_INDEX = 1;
	public static final int COOK_TIME_PROPERTY_INDEX = 2;
	public static final int COOK_TIME_TOTAL_PROPERTY_INDEX = 3;
	public static final int PROPERTY_COUNT = 4;
	public static final int DEFAULT_COOK_TIME = 200;
	private static final Codec<Map<RegistryKey<Recipe<?>>, Integer>>
			CODEC =
			Codec.unboundedMap(Recipe.KEY_CODEC, Codec.INT);
	private static final short DEFAULT_LIT_TIME_REMAINING = 0;
	private static final short DEFAULT_LIT_TOTAL_TIME = 0;
	private static final short DEFAULT_COOKING_TIME_SPENT = 0;
	private static final short DEFAULT_COOKING_TOTAL_TIME = 0;
	protected DefaultedList<ItemStack> inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
	int litTimeRemaining;
	int litTotalTime;
	int cookingTimeSpent;
	int cookingTotalTime;
	protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
		@Override
		public int get(int index) {
			switch (index) {
				case BURN_TIME_PROPERTY_INDEX:
					return AbstractFurnaceBlockEntity.this.litTimeRemaining;
				case FUEL_TIME_PROPERTY_INDEX:
					return AbstractFurnaceBlockEntity.this.litTotalTime;
				case COOK_TIME_PROPERTY_INDEX:
					return AbstractFurnaceBlockEntity.this.cookingTimeSpent;
				case COOK_TIME_TOTAL_PROPERTY_INDEX:
					return AbstractFurnaceBlockEntity.this.cookingTotalTime;
				default:
					return 0;
			}
		}

		@Override
		public void set(int index, int value) {
			switch (index) {
				case BURN_TIME_PROPERTY_INDEX:
					AbstractFurnaceBlockEntity.this.litTimeRemaining = value;
					break;
				case FUEL_TIME_PROPERTY_INDEX:
					AbstractFurnaceBlockEntity.this.litTotalTime = value;
					break;
				case COOK_TIME_PROPERTY_INDEX:
					AbstractFurnaceBlockEntity.this.cookingTimeSpent = value;
					break;
				case COOK_TIME_TOTAL_PROPERTY_INDEX:
					AbstractFurnaceBlockEntity.this.cookingTotalTime = value;
			}
		}

		@Override
		public int size() {
			return PROPERTY_COUNT;
		}
	};
	private final Reference2IntOpenHashMap<RegistryKey<Recipe<?>>> recipesUsed = new Reference2IntOpenHashMap();
	private final ServerRecipeManager.MatchGetter<SingleStackRecipeInput, ? extends AbstractCookingRecipe> matchGetter;

	protected AbstractFurnaceBlockEntity(
			BlockEntityType<?> blockEntityType,
			BlockPos pos,
			BlockState state,
			RecipeType<? extends AbstractCookingRecipe> recipeType
	) {
		super(blockEntityType, pos, state);
		this.matchGetter = ServerRecipeManager.createCachedMatchGetter(recipeType);
	}

	private boolean isBurning() {
		return this.litTimeRemaining > 0;
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
		Inventories.readData(view, this.inventory);
		this.cookingTimeSpent = view.getShort("cooking_time_spent", DEFAULT_COOKING_TIME_SPENT);
		this.cookingTotalTime = view.getShort("cooking_total_time", DEFAULT_COOKING_TOTAL_TIME);
		this.litTimeRemaining = view.getShort("lit_time_remaining", DEFAULT_LIT_TIME_REMAINING);
		this.litTotalTime = view.getShort("lit_total_time", DEFAULT_LIT_TOTAL_TIME);
		this.recipesUsed.clear();
		this.recipesUsed.putAll(view.<Map<RegistryKey<Recipe<?>>, Integer>>read("RecipesUsed", CODEC).orElse(Map.of()));
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putShort("cooking_time_spent", (short) this.cookingTimeSpent);
		view.putShort("cooking_total_time", (short) this.cookingTotalTime);
		view.putShort("lit_time_remaining", (short) this.litTimeRemaining);
		view.putShort("lit_total_time", (short) this.litTotalTime);
		Inventories.writeData(view, this.inventory);
		view.put("RecipesUsed", CODEC, this.recipesUsed);
	}

	public static void tick(ServerWorld world, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity) {
		boolean wasBurning = blockEntity.isBurning();
		boolean needsUpdate = false;

		if (blockEntity.isBurning()) {
			blockEntity.litTimeRemaining--;
		}

		ItemStack fuelStack = blockEntity.inventory.get(FUEL_SLOT_INDEX);
		ItemStack inputStack = blockEntity.inventory.get(INPUT_SLOT_INDEX);
		boolean hasInput = !inputStack.isEmpty();
		boolean hasFuel = !fuelStack.isEmpty();

		if (blockEntity.isBurning() || hasFuel && hasInput) {
			SingleStackRecipeInput recipeInput = new SingleStackRecipeInput(inputStack);
			RecipeEntry<? extends AbstractCookingRecipe> recipeEntry = hasInput
					? blockEntity.matchGetter.getFirstMatch(recipeInput, world).orElse(null)
					: null;

			int maxCount = blockEntity.getMaxCountPerStack();

			if (!blockEntity.isBurning()
				&& canAcceptRecipeOutput(world.getRegistryManager(), recipeEntry, recipeInput, blockEntity.inventory, maxCount)
			) {
				blockEntity.litTimeRemaining = blockEntity.getFuelTime(world.getFuelRegistry(), fuelStack);
				blockEntity.litTotalTime = blockEntity.litTimeRemaining;

				if (blockEntity.isBurning()) {
					needsUpdate = true;

					if (hasFuel) {
						Item fuelItem = fuelStack.getItem();
						fuelStack.decrement(1);

						if (fuelStack.isEmpty()) {
							blockEntity.inventory.set(FUEL_SLOT_INDEX, fuelItem.getRecipeRemainder());
						}
					}
				}
			}

			if (blockEntity.isBurning()
				&& canAcceptRecipeOutput(world.getRegistryManager(), recipeEntry, recipeInput, blockEntity.inventory, maxCount)
			) {
				blockEntity.cookingTimeSpent++;

				if (blockEntity.cookingTimeSpent == blockEntity.cookingTotalTime) {
					blockEntity.cookingTimeSpent = 0;
					blockEntity.cookingTotalTime = getCookTime(world, blockEntity);

					if (craftRecipe(world.getRegistryManager(), recipeEntry, recipeInput, blockEntity.inventory, maxCount)) {
						blockEntity.setLastRecipe(recipeEntry);
					}

					needsUpdate = true;
				}
			} else {
				blockEntity.cookingTimeSpent = 0;
			}
		} else if (!blockEntity.isBurning() && blockEntity.cookingTimeSpent > 0) {
			blockEntity.cookingTimeSpent = MathHelper.clamp(blockEntity.cookingTimeSpent - 2, 0, blockEntity.cookingTotalTime);
		}

		if (wasBurning != blockEntity.isBurning()) {
			needsUpdate = true;
			state = state.with(AbstractFurnaceBlock.LIT, blockEntity.isBurning());
			world.setBlockState(pos, state, Block.NOTIFY_ALL);
		}

		if (needsUpdate) {
			markDirty(world, pos, state);
		}
	}

	private static boolean canAcceptRecipeOutput(
			DynamicRegistryManager registryManager,
			@Nullable RecipeEntry<? extends AbstractCookingRecipe> recipe,
			SingleStackRecipeInput input,
			DefaultedList<ItemStack> inventory,
			int maxCount
	) {
		if (inventory.get(INPUT_SLOT_INDEX).isEmpty() || recipe == null) {
			return false;
		}

		ItemStack craftResult = recipe.value().craft(input, registryManager);

		if (craftResult.isEmpty()) {
			return false;
		}

		ItemStack outputSlot = inventory.get(OUTPUT_SLOT_INDEX);

		if (outputSlot.isEmpty()) {
			return true;
		}

		if (!ItemStack.areItemsAndComponentsEqual(outputSlot, craftResult)) {
			return false;
		}

		return outputSlot.getCount() < maxCount && outputSlot.getCount() < outputSlot.getMaxCount()
				|| outputSlot.getCount() < craftResult.getMaxCount();
	}

	private static boolean craftRecipe(
			DynamicRegistryManager registryManager,
			@Nullable RecipeEntry<? extends AbstractCookingRecipe> recipe,
			SingleStackRecipeInput input,
			DefaultedList<ItemStack> inventory,
			int maxCount
	) {
		if (recipe == null || !canAcceptRecipeOutput(registryManager, recipe, input, inventory, maxCount)) {
			return false;
		}

		ItemStack inputStack = inventory.get(INPUT_SLOT_INDEX);
		ItemStack craftResult = recipe.value().craft(input, registryManager);
		ItemStack outputSlot = inventory.get(OUTPUT_SLOT_INDEX);

		if (outputSlot.isEmpty()) {
			inventory.set(OUTPUT_SLOT_INDEX, craftResult.copy());
		} else if (ItemStack.areItemsAndComponentsEqual(outputSlot, craftResult)) {
			outputSlot.increment(1);
		}

		// Мокрая губка в доменной печи с ведром в топливном слоте превращается в ведро с водой
		if (inputStack.isOf(Blocks.WET_SPONGE.asItem())
			&& !inventory.get(FUEL_SLOT_INDEX).isEmpty()
			&& inventory.get(FUEL_SLOT_INDEX).isOf(Items.BUCKET)
		) {
			inventory.set(FUEL_SLOT_INDEX, new ItemStack(Items.WATER_BUCKET));
		}

		inputStack.decrement(1);
		return true;
	}

	protected int getFuelTime(FuelRegistry fuelRegistry, ItemStack stack) {
		return fuelRegistry.getFuelTicks(stack);
	}

	private static int getCookTime(ServerWorld world, AbstractFurnaceBlockEntity furnace) {
		SingleStackRecipeInput singleStackRecipeInput = new SingleStackRecipeInput(furnace.getStack(INPUT_SLOT_INDEX));
		return furnace.matchGetter
				.getFirstMatch(singleStackRecipeInput, world)
				.map(recipe -> recipe.value().getCookingTime())
				.orElse(DEFAULT_COOK_TIME);
	}

	@Override
	public int[] getAvailableSlots(Direction side) {
		if (side == Direction.DOWN) {
			return BOTTOM_SLOTS;
		}

		return side == Direction.UP ? TOP_SLOTS : SIDE_SLOTS;
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
		return isValid(slot, stack);
	}

	@Override
	public boolean canExtract(int slot, ItemStack stack, Direction dir) {
		return dir == Direction.DOWN && slot == FUEL_SLOT_INDEX
				? stack.isOf(Items.WATER_BUCKET) || stack.isOf(Items.BUCKET)
				: true;
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

	@Override
	public void setStack(int slot, ItemStack stack) {
		ItemStack existing = inventory.get(slot);
		boolean sameItem = !stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(existing, stack);
		inventory.set(slot, stack);
		stack.capCount(getMaxCount(stack));

		if (slot == INPUT_SLOT_INDEX && !sameItem && world instanceof ServerWorld serverWorld) {
			cookingTotalTime = getCookTime(serverWorld, this);
			cookingTimeSpent = 0;
			markDirty();
		}
	}

	@Override
	public boolean isValid(int slot, ItemStack stack) {
		if (slot == OUTPUT_SLOT_INDEX) {
			return false;
		}

		if (slot != FUEL_SLOT_INDEX) {
			return true;
		}

		ItemStack currentFuel = inventory.get(FUEL_SLOT_INDEX);
		return world.getFuelRegistry().isFuel(stack)
				|| stack.isOf(Items.BUCKET) && !currentFuel.isOf(Items.BUCKET);
	}

	@Override
	public void setLastRecipe(@Nullable RecipeEntry<?> recipe) {
		if (recipe != null) {
			recipesUsed.addTo(recipe.id(), 1);
		}
	}

	@Override
	public @Nullable RecipeEntry<?> getLastRecipe() {
		return null;
	}

	@Override
	public void unlockLastRecipe(PlayerEntity player, List<ItemStack> ingredients) {
	}

	/**
	 * Начисляет игроку опыт за все рецепты, использованные в этой печи с момента последней очистки.
	 * Вызывается при взаимодействии игрока с печью (открытие экрана, разрушение блока).
	 */
	public void dropExperienceForRecipesUsed(ServerPlayerEntity player) {
		List<RecipeEntry<?>> usedRecipes = getRecipesUsedAndDropExperience(player.getEntityWorld(), player.getEntityPos());
		player.unlockRecipes(usedRecipes);

		for (RecipeEntry<?> recipeEntry : usedRecipes) {
			player.onRecipeCrafted(recipeEntry, inventory);
		}

		recipesUsed.clear();
	}

	/**
	 * Собирает список использованных рецептов и спавнит орбы опыта в указанной позиции.
	 * Итерируется по {@code recipesUsed} через for-each по entrySet для избежания артефактов декомпилятора.
	 */
	public List<RecipeEntry<?>> getRecipesUsedAndDropExperience(ServerWorld world, Vec3d pos) {
		List<RecipeEntry<?>> result = Lists.newArrayList();

		for (Entry<RegistryKey<Recipe<?>>> entry : recipesUsed.reference2IntEntrySet()) {
			world.getRecipeManager().get(entry.getKey()).ifPresent(recipe -> {
				result.add(recipe);
				dropExperience(world, pos, entry.getIntValue(), ((AbstractCookingRecipe) recipe.value()).getExperience());
			});
		}

		return result;
	}

	private static void dropExperience(ServerWorld world, Vec3d pos, int multiplier, float experience) {
		int orbCount = MathHelper.floor(multiplier * experience);
		float remainder = MathHelper.fractionalPart(multiplier * experience);

		if (remainder != 0.0F && world.random.nextFloat() < remainder) {
			orbCount++;
		}

		ExperienceOrbEntity.spawn(world, pos, orbCount);
	}

	@Override
	public void provideRecipeInputs(RecipeFinder finder) {
		for (ItemStack itemStack : inventory) {
			finder.addInput(itemStack);
		}
	}

	@Override
	public void onBlockReplaced(BlockPos pos, BlockState oldState) {
		super.onBlockReplaced(pos, oldState);

		if (world instanceof ServerWorld serverWorld) {
			getRecipesUsedAndDropExperience(serverWorld, Vec3d.ofCenter(pos));
		}
	}
}
