package net.minecraft.screen;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BannerItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BannerPatternTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.DyeColor;

import java.util.List;

/**
 * Обработчик экрана ткацкого станка.
 * <p>
 * Принимает знамя, краситель и (опционально) предмет с узором.
 * Вычисляет список доступных узоров и применяет выбранный при взятии результата.
 * Максимальное количество слоёв узора на знамени — 6.
 */
public class LoomScreenHandler extends ScreenHandler {

	private static final int NO_PATTERN = -1;
	private static final int MAX_BANNER_LAYERS = 6;
	private static final int INVENTORY_START = 4;
	private static final int INVENTORY_END = 31;
	private static final int HOTBAR_START = 31;
	private static final int HOTBAR_END = 40;

	private final ScreenHandlerContext context;
	public final Property selectedPattern = Property.create();
	private List<RegistryEntry<BannerPattern>> bannerPatterns = List.of();
	Runnable inventoryChangeListener = () -> {};
	private final RegistryEntryLookup<BannerPattern> bannerPatternLookup;
	final Slot bannerSlot;
	final Slot dyeSlot;
	private final Slot patternSlot;
	private final Slot outputSlot;
	long lastTakeResultTime;

	private final Inventory input = new SimpleInventory(3) {
		@Override
		public void markDirty() {
			super.markDirty();
			LoomScreenHandler.this.onContentChanged(this);
			LoomScreenHandler.this.inventoryChangeListener.run();
		}
	};
	private final Inventory output = new SimpleInventory(1) {
		@Override
		public void markDirty() {
			super.markDirty();
			LoomScreenHandler.this.inventoryChangeListener.run();
		}
	};

	public LoomScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	public LoomScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(ScreenHandlerType.LOOM, syncId);
		this.context = context;
		bannerSlot = addSlot(new Slot(input, 0, 13, 26) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.getItem() instanceof BannerItem;
			}
		});
		dyeSlot = addSlot(new Slot(input, 1, 33, 26) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.getItem() instanceof DyeItem;
			}
		});
		patternSlot = addSlot(new Slot(input, 2, 23, 45) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.contains(DataComponentTypes.PROVIDES_BANNER_PATTERNS);
			}
		});
		outputSlot = addSlot(new Slot(output, 0, 143, 57) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return false;
			}

			@Override
			public void onTakeItem(PlayerEntity player, ItemStack stack) {
				LoomScreenHandler.this.bannerSlot.takeStack(1);
				LoomScreenHandler.this.dyeSlot.takeStack(1);

				if (!LoomScreenHandler.this.bannerSlot.hasStack()
						|| !LoomScreenHandler.this.dyeSlot.hasStack()) {
					LoomScreenHandler.this.selectedPattern.set(NO_PATTERN);
				}

				context.run((world, pos) -> {
					long currentTime = world.getTime();

					if (LoomScreenHandler.this.lastTakeResultTime != currentTime) {
						world.playSound(null, pos, SoundEvents.UI_LOOM_TAKE_RESULT, SoundCategory.BLOCKS, 1.0F, 1.0F);
						LoomScreenHandler.this.lastTakeResultTime = currentTime;
					}
				});
				super.onTakeItem(player, stack);
			}
		});
		addPlayerSlots(playerInventory, 8, 84);
		addProperty(selectedPattern);
		bannerPatternLookup = playerInventory.player.getRegistryManager().getOrThrow(RegistryKeys.BANNER_PATTERN);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(context, player, Blocks.LOOM);
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		if (id < 0 || id >= bannerPatterns.size()) {
			return false;
		}

		selectedPattern.set(id);
		updateOutputSlot(bannerPatterns.get(id));
		return true;
	}

	private List<RegistryEntry<BannerPattern>> getPatternsFor(ItemStack stack) {
		if (stack.isEmpty()) {
			return bannerPatternLookup
					.getOptional(BannerPatternTags.NO_ITEM_REQUIRED)
					.<List<RegistryEntry<BannerPattern>>>map(ImmutableList::copyOf)
					.orElse(ImmutableList.of());
		}

		TagKey<BannerPattern> tagKey = stack.get(DataComponentTypes.PROVIDES_BANNER_PATTERNS);

		return tagKey != null
				? bannerPatternLookup
				.getOptional(tagKey)
				.<List<RegistryEntry<BannerPattern>>>map(ImmutableList::copyOf)
				.orElse(ImmutableList.of())
				: List.of();
	}

	private boolean isPatternIndexValid(int index) {
		return index >= 0 && index < bannerPatterns.size();
	}

	/**
	 * Пересчитывает список доступных узоров и обновляет слот результата
	 * при изменении содержимого входных слотов.
	 * <p>
	 * Если знамя или краситель отсутствуют — очищает результат.
	 * Если узоров ровно один — выбирает его автоматически.
	 * Если текущий выбранный узор больше не доступен — сбрасывает выбор.
	 */
	@Override
	public void onContentChanged(Inventory inventory) {
		ItemStack banner = bannerSlot.getStack();
		ItemStack dye = dyeSlot.getStack();
		ItemStack patternItem = patternSlot.getStack();

		if (banner.isEmpty() || dye.isEmpty()) {
			outputSlot.setStackNoCallbacks(ItemStack.EMPTY);
			bannerPatterns = List.of();
			selectedPattern.set(NO_PATTERN);
			return;
		}

		int currentIndex = selectedPattern.get();
		boolean wasValid = isPatternIndexValid(currentIndex);
		List<RegistryEntry<BannerPattern>> previousPatterns = bannerPatterns;
		bannerPatterns = getPatternsFor(patternItem);

		RegistryEntry<BannerPattern> chosenPattern;

		if (bannerPatterns.size() == 1) {
			selectedPattern.set(0);
			chosenPattern = bannerPatterns.get(0);
		}
		else if (!wasValid) {
			selectedPattern.set(NO_PATTERN);
			chosenPattern = null;
		}
		else {
			RegistryEntry<BannerPattern> previouslySelected = previousPatterns.get(currentIndex);
			int newIndex = bannerPatterns.indexOf(previouslySelected);

			if (newIndex != -1) {
				chosenPattern = previouslySelected;
				selectedPattern.set(newIndex);
			}
			else {
				chosenPattern = null;
				selectedPattern.set(NO_PATTERN);
			}
		}

		if (chosenPattern == null) {
			outputSlot.setStackNoCallbacks(ItemStack.EMPTY);
			sendContentUpdates();
			return;
		}

		BannerPatternsComponent existingLayers = banner.getOrDefault(
				DataComponentTypes.BANNER_PATTERNS,
				BannerPatternsComponent.DEFAULT
		);

		if (existingLayers.layers().size() >= MAX_BANNER_LAYERS) {
			selectedPattern.set(NO_PATTERN);
			outputSlot.setStackNoCallbacks(ItemStack.EMPTY);
		}
		else {
			updateOutputSlot(chosenPattern);
		}

		sendContentUpdates();
	}

	public List<RegistryEntry<BannerPattern>> getBannerPatterns() {
		return bannerPatterns;
	}

	public int getSelectedPattern() {
		return selectedPattern.get();
	}

	public void setInventoryChangeListener(Runnable inventoryChangeListener) {
		this.inventoryChangeListener = inventoryChangeListener;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		Slot sourceSlot = slots.get(slot);

		if (sourceSlot == null || !sourceSlot.hasStack()) {
			return ItemStack.EMPTY;
		}

		ItemStack slotStack = sourceSlot.getStack();
		ItemStack original = slotStack.copy();

		if (slot == outputSlot.id) {
			if (!insertItem(slotStack, INVENTORY_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}

			sourceSlot.onQuickTransfer(slotStack, original);
		}
		else if (slot == dyeSlot.id || slot == bannerSlot.id || slot == patternSlot.id) {
			if (!insertItem(slotStack, INVENTORY_START, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		}
		else if (slotStack.getItem() instanceof BannerItem) {
			if (!insertItem(slotStack, bannerSlot.id, bannerSlot.id + 1, false)) {
				return ItemStack.EMPTY;
			}
		}
		else if (slotStack.getItem() instanceof DyeItem) {
			if (!insertItem(slotStack, dyeSlot.id, dyeSlot.id + 1, false)) {
				return ItemStack.EMPTY;
			}
		}
		else if (slotStack.contains(DataComponentTypes.PROVIDES_BANNER_PATTERNS)) {
			if (!insertItem(slotStack, patternSlot.id, patternSlot.id + 1, false)) {
				return ItemStack.EMPTY;
			}
		}
		else if (slot >= INVENTORY_START && slot < INVENTORY_END) {
			if (!insertItem(slotStack, HOTBAR_START, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		}
		else if (slot >= HOTBAR_START && slot < HOTBAR_END) {
			if (!insertItem(slotStack, INVENTORY_START, INVENTORY_END, false)) {
				return ItemStack.EMPTY;
			}
		}
		else if (!insertItem(slotStack, INVENTORY_START, HOTBAR_END, false)) {
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

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		context.run((world, pos) -> dropInventory(player, input));
	}

	private void updateOutputSlot(RegistryEntry<BannerPattern> pattern) {
		ItemStack banner = bannerSlot.getStack();
		ItemStack dye = dyeSlot.getStack();

		if (banner.isEmpty() || dye.isEmpty()) {
			outputSlot.setStackNoCallbacks(ItemStack.EMPTY);
			return;
		}

		ItemStack result = banner.copyWithCount(1);
		DyeColor dyeColor = ((DyeItem) dye.getItem()).getColor();
		result.apply(
				DataComponentTypes.BANNER_PATTERNS,
				BannerPatternsComponent.DEFAULT,
				component -> new BannerPatternsComponent.Builder().addAll(component).add(pattern, dyeColor).build()
		);

		if (!ItemStack.areEqual(result, outputSlot.getStack())) {
			outputSlot.setStackNoCallbacks(result);
		}
	}

	public Slot getBannerSlot() {
		return bannerSlot;
	}

	public Slot getDyeSlot() {
		return dyeSlot;
	}

	public Slot getPatternSlot() {
		return patternSlot;
	}

	public Slot getOutputSlot() {
		return outputSlot;
	}

	public boolean canApplyDyePattern() {
		return !bannerSlot.getStack().isEmpty() && !dyeSlot.getStack().isEmpty();
	}

	public boolean hasTooManyPatterns() {
		ItemStack banner = bannerSlot.getStack();
		if (banner.isEmpty()) {
			return false;
		}
		net.minecraft.component.type.BannerPatternsComponent layers = banner.getOrDefault(
				net.minecraft.component.DataComponentTypes.BANNER_PATTERNS,
				net.minecraft.component.type.BannerPatternsComponent.DEFAULT
		);
		return layers.layers().size() >= MAX_BANNER_LAYERS;
	}

	public java.util.List<net.minecraft.registry.entry.RegistryEntry<net.minecraft.block.entity.BannerPattern>> getOutputBannerPatterns() {
		return bannerPatterns;
	}

}
