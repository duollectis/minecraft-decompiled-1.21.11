package net.minecraft.screen;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Blocks;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Optional;

/**
 * Обработчик экрана стола зачарований.
 * <p>
 * Управляет двумя слотами (предмет + лазурит), вычисляет доступные уровни
 * зачарований в зависимости от количества книжных полок вокруг стола
 * и применяет выбранное зачарование при нажатии кнопки.
 */
public class EnchantmentScreenHandler extends ScreenHandler {

	private static final int ITEM_SLOT_INDEX = 0;
	private static final int LAPIS_SLOT_INDEX = 1;
	private static final int PLAYER_SLOTS_START = 2;
	private static final int PLAYER_SLOTS_END = 38;
	private static final int ENCHANTMENT_SLOT_COUNT = 3;
	private static final int PROP_POWER_0 = 0;
	private static final int PROP_POWER_1 = 1;
	private static final int PROP_POWER_2 = 2;
	private static final int PROP_SEED = 3;
	private static final int PROP_ID_0 = 4;
	private static final int PROP_ID_1 = 5;
	private static final int PROP_ID_2 = 6;
	private static final int PROP_LEVEL_0 = 7;
	private static final int PROP_LEVEL_1 = 8;
	private static final int PROP_LEVEL_2 = 9;
	private static final int NO_ENCHANTMENT = -1;

	static final Identifier EMPTY_LAPIS_LAZULI_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/lapis_lazuli");

	private final Inventory inventory = new SimpleInventory(2) {
		@Override
		public void markDirty() {
			super.markDirty();
			EnchantmentScreenHandler.this.onContentChanged(this);
		}
	};
	private final ScreenHandlerContext context;
	private final Random random = Random.create();
	private final Property seed = Property.create();
	public final int[] enchantmentPower = new int[ENCHANTMENT_SLOT_COUNT];
	public final int[] enchantmentId = new int[]{NO_ENCHANTMENT, NO_ENCHANTMENT, NO_ENCHANTMENT};
	public final int[] enchantmentLevel = new int[]{NO_ENCHANTMENT, NO_ENCHANTMENT, NO_ENCHANTMENT};

	public EnchantmentScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	public EnchantmentScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(ScreenHandlerType.ENCHANTMENT, syncId);
		this.context = context;
		addSlot(new Slot(inventory, ITEM_SLOT_INDEX, 15, 47) {
			@Override
			public int getMaxItemCount() {
				return 1;
			}
		});
		addSlot(new Slot(inventory, LAPIS_SLOT_INDEX, 35, 47) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.isOf(Items.LAPIS_LAZULI);
			}

			@Override
			public Identifier getBackgroundSprite() {
				return EnchantmentScreenHandler.EMPTY_LAPIS_LAZULI_SLOT_TEXTURE;
			}
		});
		addPlayerSlots(playerInventory, 8, 84);
		addProperty(Property.create(enchantmentPower, 0));
		addProperty(Property.create(enchantmentPower, 1));
		addProperty(Property.create(enchantmentPower, 2));
		addProperty(seed).set(playerInventory.player.getEnchantingTableSeed());
		addProperty(Property.create(enchantmentId, 0));
		addProperty(Property.create(enchantmentId, 1));
		addProperty(Property.create(enchantmentId, 2));
		addProperty(Property.create(enchantmentLevel, 0));
		addProperty(Property.create(enchantmentLevel, 1));
		addProperty(Property.create(enchantmentLevel, 2));
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		if (inventory != this.inventory) {
			return;
		}

		ItemStack itemStack = inventory.getStack(ITEM_SLOT_INDEX);

		if (itemStack.isEmpty() || !itemStack.isEnchantable()) {
			for (int slot = 0; slot < ENCHANTMENT_SLOT_COUNT; slot++) {
				enchantmentPower[slot] = 0;
				enchantmentId[slot] = NO_ENCHANTMENT;
				enchantmentLevel[slot] = NO_ENCHANTMENT;
			}

			return;
		}

		context.run((world, pos) -> {
			IndexedIterable<RegistryEntry<Enchantment>> indexedEntries = world.getRegistryManager()
					.getOrThrow(RegistryKeys.ENCHANTMENT)
					.getIndexedEntries();

			int bookshelfCount = 0;

			for (BlockPos offset : EnchantingTableBlock.POWER_PROVIDER_OFFSETS) {
				if (EnchantingTableBlock.canAccessPowerProvider(world, pos, offset)) {
					bookshelfCount++;
				}
			}

			random.setSeed(seed.get());

			for (int slot = 0; slot < ENCHANTMENT_SLOT_COUNT; slot++) {
				enchantmentPower[slot] = EnchantmentHelper.calculateRequiredExperienceLevel(
						random,
						slot,
						bookshelfCount,
						itemStack
				);
				enchantmentId[slot] = NO_ENCHANTMENT;
				enchantmentLevel[slot] = NO_ENCHANTMENT;

				if (enchantmentPower[slot] < slot + 1) {
					enchantmentPower[slot] = 0;
				}
			}

			for (int slot = 0; slot < ENCHANTMENT_SLOT_COUNT; slot++) {
				if (enchantmentPower[slot] == 0) {
					continue;
				}

				List<EnchantmentLevelEntry> candidates = generateEnchantments(
						world.getRegistryManager(),
						itemStack,
						slot,
						enchantmentPower[slot]
				);

				if (candidates.isEmpty()) {
					continue;
				}

				EnchantmentLevelEntry chosen = candidates.get(random.nextInt(candidates.size()));
				enchantmentId[slot] = indexedEntries.getRawId(chosen.enchantment());
				enchantmentLevel[slot] = chosen.level();
			}

			sendContentUpdates();
		});
	}

	/**
	 * Применяет выбранное зачарование (кнопка 0–2) к предмету в слоте.
	 * <p>
	 * Проверяет наличие лазурита, достаточный уровень опыта и корректность
	 * индекса кнопки. При успехе списывает лазурит и уровни опыта.
	 */
	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		if (id < 0 || id >= enchantmentPower.length) {
			Util.logErrorOrPause(player.getStringifiedName() + " pressed invalid button id: " + id);
			return false;
		}

		ItemStack itemStack = inventory.getStack(ITEM_SLOT_INDEX);
		ItemStack lapisStack = inventory.getStack(LAPIS_SLOT_INDEX);
		int requiredLapis = id + 1;

		if ((lapisStack.isEmpty() || lapisStack.getCount() < requiredLapis) && !player.isInCreativeMode()) {
			return false;
		}

		if (enchantmentPower[id] <= 0 || itemStack.isEmpty()) {
			return false;
		}

		boolean notEnoughLevels = player.experienceLevel < requiredLapis
				|| player.experienceLevel < enchantmentPower[id];

		if (notEnoughLevels && !player.isInCreativeMode()) {
			return false;
		}

		context.run((world, pos) -> {
			ItemStack enchantTarget = itemStack;
			List<EnchantmentLevelEntry> enchantments = generateEnchantments(
					world.getRegistryManager(),
					itemStack,
					id,
					enchantmentPower[id]
			);

			if (enchantments.isEmpty()) {
				return;
			}

			player.applyEnchantmentCosts(itemStack, requiredLapis);

			if (itemStack.isOf(Items.BOOK)) {
				enchantTarget = itemStack.withItem(Items.ENCHANTED_BOOK);
				inventory.setStack(ITEM_SLOT_INDEX, enchantTarget);
			}

			for (EnchantmentLevelEntry entry : enchantments) {
				enchantTarget.addEnchantment(entry.enchantment(), entry.level());
			}

			lapisStack.decrementUnlessCreative(requiredLapis, player);

			if (lapisStack.isEmpty()) {
				inventory.setStack(LAPIS_SLOT_INDEX, ItemStack.EMPTY);
			}

			player.incrementStat(Stats.ENCHANT_ITEM);

			if (player instanceof ServerPlayerEntity serverPlayer) {
				Criteria.ENCHANTED_ITEM.trigger(serverPlayer, enchantTarget, requiredLapis);
			}

			inventory.markDirty();
			seed.set(player.getEnchantingTableSeed());
			onContentChanged(inventory);
			world.playSound(
					null,
					pos,
					SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
					SoundCategory.BLOCKS,
					1.0F,
					world.random.nextFloat() * 0.1F + 0.9F
			);
		});

		return true;
	}

	/**
	 * Генерирует список возможных зачарований для заданного слота и уровня мощности.
	 * Для книг список обрезается до одного случайного зачарования.
	 */
	private List<EnchantmentLevelEntry> generateEnchantments(
			DynamicRegistryManager registryManager,
			ItemStack stack,
			int slot,
			int level
	) {
		random.setSeed(seed.get() + slot);

		Optional<RegistryEntryList.Named<Enchantment>> enchantingTableTag = registryManager
				.getOrThrow(RegistryKeys.ENCHANTMENT)
				.getOptional(EnchantmentTags.IN_ENCHANTING_TABLE);

		if (enchantingTableTag.isEmpty()) {
			return List.of();
		}

		List<EnchantmentLevelEntry> list = EnchantmentHelper.generateEnchantments(
				random,
				stack,
				level,
				enchantingTableTag.get().stream()
		);

		if (stack.isOf(Items.BOOK) && list.size() > 1) {
			list.remove(random.nextInt(list.size()));
		}

		return list;
	}

	public int getLapisCount() {
		ItemStack lapisStack = inventory.getStack(LAPIS_SLOT_INDEX);
		return lapisStack.isEmpty() ? 0 : lapisStack.getCount();
	}

	public int getSeed() {
		return seed.get();
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		context.run((world, pos) -> dropInventory(player, inventory));
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(context, player, Blocks.ENCHANTING_TABLE);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		Slot sourceSlot = slots.get(slot);

		if (sourceSlot == null || !sourceSlot.hasStack()) {
			return ItemStack.EMPTY;
		}

		ItemStack slotStack = sourceSlot.getStack();
		ItemStack original = slotStack.copy();

		if (slot == ITEM_SLOT_INDEX || slot == LAPIS_SLOT_INDEX) {
			if (!insertItem(slotStack, PLAYER_SLOTS_START, PLAYER_SLOTS_END, true)) {
				return ItemStack.EMPTY;
			}
		}
		else if (slotStack.isOf(Items.LAPIS_LAZULI)) {
			if (!insertItem(slotStack, LAPIS_SLOT_INDEX, LAPIS_SLOT_INDEX + 1, true)) {
				return ItemStack.EMPTY;
			}
		}
		else {
			Slot itemSlot = slots.get(ITEM_SLOT_INDEX);

			if (itemSlot.hasStack() || !itemSlot.canInsert(slotStack)) {
				return ItemStack.EMPTY;
			}

			ItemStack singleItem = slotStack.copyWithCount(1);
			slotStack.decrement(1);
			itemSlot.setStack(singleItem);
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
