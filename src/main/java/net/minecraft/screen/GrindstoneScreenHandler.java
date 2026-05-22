package net.minecraft.screen;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Обработчик экрана точильного камня.
 * <p>
 * Принимает до двух предметов в слоты ввода, объединяет их прочность
 * и снимает незлобные зачарования, возвращая опыт пропорционально
 * суммарной стоимости снятых чар.
 */
public class GrindstoneScreenHandler extends ScreenHandler {

	public static final int MAX_EXPERIENCE_COST = 35;
	public static final int INPUT_1_ID = 0;
	public static final int INPUT_2_ID = 1;
	public static final int OUTPUT_ID = 2;
	private static final int INVENTORY_START = 3;
	private static final int INVENTORY_END = 30;
	private static final int HOTBAR_START = 30;
	private static final int HOTBAR_END = 39;
	private static final int REPAIR_BONUS_PERCENT = 5;

	private final Inventory result = new CraftingResultInventory();
	final Inventory input = new SimpleInventory(2) {
		@Override
		public void markDirty() {
			super.markDirty();
			GrindstoneScreenHandler.this.onContentChanged(this);
		}
	};
	private final ScreenHandlerContext context;

	public GrindstoneScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	public GrindstoneScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(ScreenHandlerType.GRINDSTONE, syncId);
		this.context = context;
		addSlot(new Slot(input, INPUT_1_ID, 49, 19) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.isDamageable() || EnchantmentHelper.hasEnchantments(stack);
			}
		});
		addSlot(new Slot(input, INPUT_2_ID, 49, 40) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.isDamageable() || EnchantmentHelper.hasEnchantments(stack);
			}
		});
		addSlot(new Slot(result, OUTPUT_ID, 129, 34) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return false;
			}

			@Override
			public void onTakeItem(PlayerEntity player, ItemStack stack) {
				context.run((world, pos) -> {
					if (world instanceof ServerWorld serverWorld) {
						ExperienceOrbEntity.spawn(serverWorld, Vec3d.ofCenter(pos), calculateExperience(world));
					}

					world.syncWorldEvent(1042, pos, 0);
				});
				GrindstoneScreenHandler.this.input.setStack(INPUT_1_ID, ItemStack.EMPTY);
				GrindstoneScreenHandler.this.input.setStack(INPUT_2_ID, ItemStack.EMPTY);
			}

			private int calculateExperience(World world) {
				int total = getEnchantmentExperience(GrindstoneScreenHandler.this.input.getStack(INPUT_1_ID));
				total += getEnchantmentExperience(GrindstoneScreenHandler.this.input.getStack(INPUT_2_ID));

				if (total <= 0) {
					return 0;
				}

				int half = (int) Math.ceil(total / 2.0);
				return half + world.random.nextInt(half);
			}

			private int getEnchantmentExperience(ItemStack stack) {
				int experience = 0;
				ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);

				for (Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
					RegistryEntry<Enchantment> enchantment = (RegistryEntry<Enchantment>) entry.getKey();

					if (!enchantment.isIn(EnchantmentTags.CURSE)) {
						experience += enchantment.value().getMinPower(entry.getIntValue());
					}
				}

				return experience;
			}
		});
		addPlayerSlots(playerInventory, 8, 84);
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		super.onContentChanged(inventory);

		if (inventory == input) {
			updateResult();
		}
	}

	private void updateResult() {
		result.setStack(0, getOutputStack(input.getStack(INPUT_1_ID), input.getStack(INPUT_2_ID)));
		sendContentUpdates();
	}

	/**
	 * Вычисляет результирующий стек для двух входных предметов.
	 * <p>
	 * Если оба слота пусты — возвращает пустой стек.
	 * Если заполнен только один — снимает зачарования (grind).
	 * Если оба заполнены — объединяет прочность и зачарования.
	 */
	private ItemStack getOutputStack(ItemStack firstInput, ItemStack secondInput) {
		boolean anyPresent = !firstInput.isEmpty() || !secondInput.isEmpty();

		if (!anyPresent) {
			return ItemStack.EMPTY;
		}

		if (firstInput.getCount() > 1 || secondInput.getCount() > 1) {
			return ItemStack.EMPTY;
		}

		boolean bothPresent = !firstInput.isEmpty() && !secondInput.isEmpty();

		if (bothPresent) {
			return combineItems(firstInput, secondInput);
		}

		ItemStack single = !firstInput.isEmpty() ? firstInput : secondInput;
		return EnchantmentHelper.hasEnchantments(single) ? grind(single.copy()) : ItemStack.EMPTY;
	}

	private ItemStack combineItems(ItemStack firstInput, ItemStack secondInput) {
		if (!firstInput.isOf(secondInput.getItem())) {
			return ItemStack.EMPTY;
		}

		int maxDamage = Math.max(firstInput.getMaxDamage(), secondInput.getMaxDamage());
		int firstDurability = firstInput.getMaxDamage() - firstInput.getDamage();
		int secondDurability = secondInput.getMaxDamage() - secondInput.getDamage();
		int combinedDurability = firstDurability + secondDurability + maxDamage * REPAIR_BONUS_PERCENT / 100;
		int resultCount = 1;

		if (!firstInput.isDamageable()) {
			if (firstInput.getMaxCount() < 2 || !ItemStack.areEqual(firstInput, secondInput)) {
				return ItemStack.EMPTY;
			}

			resultCount = 2;
		}

		ItemStack resultStack = firstInput.copyWithCount(resultCount);

		if (resultStack.isDamageable()) {
			resultStack.set(DataComponentTypes.MAX_DAMAGE, maxDamage);
			resultStack.setDamage(Math.max(maxDamage - combinedDurability, 0));
		}

		transferEnchantments(resultStack, secondInput);
		return grind(resultStack);
	}

	private void transferEnchantments(ItemStack target, ItemStack source) {
		EnchantmentHelper.apply(target, components -> {
			ItemEnchantmentsComponent sourceEnchantments = EnchantmentHelper.getEnchantments(source);

			for (Entry<RegistryEntry<Enchantment>> entry : sourceEnchantments.getEnchantmentEntries()) {
				RegistryEntry<Enchantment> enchantment = (RegistryEntry<Enchantment>) entry.getKey();

				if (!enchantment.isIn(EnchantmentTags.CURSE) || components.getLevel(enchantment) == 0) {
					components.add(enchantment, entry.getIntValue());
				}
			}
		});
	}

	private ItemStack grind(ItemStack item) {
		ItemEnchantmentsComponent remaining = EnchantmentHelper.apply(
				item,
				components -> components.remove(enchantment -> !enchantment.isIn(EnchantmentTags.CURSE))
		);

		if (item.isOf(Items.ENCHANTED_BOOK) && remaining.isEmpty()) {
			item = item.withItem(Items.BOOK);
		}

		int repairCost = 0;

		for (int enchantIndex = 0; enchantIndex < remaining.getSize(); enchantIndex++) {
			repairCost = AnvilScreenHandler.getNextCost(repairCost);
		}

		item.set(DataComponentTypes.REPAIR_COST, repairCost);
		return item;
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		context.run((world, pos) -> dropInventory(player, input));
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(context, player, Blocks.GRINDSTONE);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		Slot sourceSlot = slots.get(slot);

		if (sourceSlot == null || !sourceSlot.hasStack()) {
			return ItemStack.EMPTY;
		}

		ItemStack slotStack = sourceSlot.getStack();
		ItemStack original = slotStack.copy();
		ItemStack firstInput = input.getStack(INPUT_1_ID);
		ItemStack secondInput = input.getStack(INPUT_2_ID);

		if (slot == OUTPUT_ID) {
			if (!insertItem(slotStack, INVENTORY_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}

			sourceSlot.onQuickTransfer(slotStack, original);
		}
		else if (slot == INPUT_1_ID || slot == INPUT_2_ID) {
			if (!insertItem(slotStack, INVENTORY_START, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		}
		else if (!firstInput.isEmpty() && !secondInput.isEmpty()) {
			if (slot >= INVENTORY_START && slot < INVENTORY_END) {
				if (!insertItem(slotStack, HOTBAR_START, HOTBAR_END, false)) {
					return ItemStack.EMPTY;
				}
			}
			else if (slot >= HOTBAR_START && slot < HOTBAR_END) {
				if (!insertItem(slotStack, INVENTORY_START, INVENTORY_END, false)) {
					return ItemStack.EMPTY;
				}
			}
		}
		else if (!insertItem(slotStack, INPUT_1_ID, OUTPUT_ID, false)) {
			return ItemStack.EMPTY;
		}

		if (slotStack.isEmpty()) {
			sourceSlot.setStack(ItemStack.EMPTY);
		}
		else {
			sourceSlot.markDirty();
		}

		if (slotStack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}

		sourceSlot.onTakeItem(player, slotStack);

		return original;
	}
}
