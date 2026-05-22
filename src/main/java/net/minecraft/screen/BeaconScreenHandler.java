package net.minecraft.screen;

import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Обработчик экрана маяка.
 * <p>
 * Управляет одним слотом оплаты (принимает только предметы из тега {@code beacon_payment_items})
 * и тремя синхронизируемыми свойствами: уровень маяка, первичный и вторичный эффекты.
 */
public class BeaconScreenHandler extends ScreenHandler {

	private static final int PAYMENT_SLOT_ID = 0;
	private static final int PROPERTY_COUNT = 3;
	private static final int INVENTORY_START = 1;
	private static final int INVENTORY_END = 28;
	private static final int HOTBAR_START = 28;
	private static final int HOTBAR_END = 37;
	private static final int PROP_LEVEL = 0;
	private static final int PROP_PRIMARY_EFFECT = 1;
	private static final int PROP_SECONDARY_EFFECT = 2;

	private final Inventory payment = new SimpleInventory(1) {
		@Override
		public boolean isValid(int slot, ItemStack stack) {
			return stack.isIn(ItemTags.BEACON_PAYMENT_ITEMS);
		}

		@Override
		public int getMaxCountPerStack() {
			return 1;
		}
	};
	private final PaymentSlot paymentSlot;
	private final ScreenHandlerContext context;
	private final PropertyDelegate propertyDelegate;

	public BeaconScreenHandler(int syncId, Inventory inventory) {
		this(syncId, inventory, new ArrayPropertyDelegate(PROPERTY_COUNT), ScreenHandlerContext.EMPTY);
	}

	public BeaconScreenHandler(
			int syncId,
			Inventory inventory,
			PropertyDelegate propertyDelegate,
			ScreenHandlerContext context
	) {
		super(ScreenHandlerType.BEACON, syncId);
		checkDataCount(propertyDelegate, PROPERTY_COUNT);
		this.propertyDelegate = propertyDelegate;
		this.context = context;
		paymentSlot = new PaymentSlot(payment, PAYMENT_SLOT_ID, 136, 110);
		addSlot(paymentSlot);
		addProperties(propertyDelegate);
		addPlayerSlots(inventory, 36, 137);
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		if (player.getEntityWorld().isClient()) {
			return;
		}

		ItemStack remaining = paymentSlot.takeStack(paymentSlot.getMaxItemCount());
		if (!remaining.isEmpty()) {
			player.dropItem(remaining, false);
		}
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(context, player, Blocks.BEACON);
	}

	@Override
	public void setProperty(int id, int value) {
		super.setProperty(id, value);
		sendContentUpdates();
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

		if (slot == PAYMENT_SLOT_ID) {
			if (!insertItem(stack, INVENTORY_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}

			sourceSlot.onQuickTransfer(stack, original);
		} else if (!paymentSlot.hasStack() && paymentSlot.canInsert(stack) && stack.getCount() == 1) {
			if (!insertItem(stack, PAYMENT_SLOT_ID, PAYMENT_SLOT_ID + 1, false)) {
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

	public int getProperties() {
		return propertyDelegate.get(PROP_LEVEL);
	}

	public static int getRawIdForStatusEffect(@Nullable RegistryEntry<StatusEffect> effect) {
		return effect == null ? 0 : Registries.STATUS_EFFECT.getIndexedEntries().getRawId(effect) + 1;
	}

	public static @Nullable RegistryEntry<StatusEffect> getStatusEffectForRawId(int id) {
		return id == 0 ? null : Registries.STATUS_EFFECT.getIndexedEntries().get(id - 1);
	}

	public @Nullable RegistryEntry<StatusEffect> getPrimaryEffect() {
		return getStatusEffectForRawId(propertyDelegate.get(PROP_PRIMARY_EFFECT));
	}

	public @Nullable RegistryEntry<StatusEffect> getSecondaryEffect() {
		return getStatusEffectForRawId(propertyDelegate.get(PROP_SECONDARY_EFFECT));
	}

	/**
	 * Применяет выбранные эффекты маяка, расходуя предмет оплаты.
	 * Вызывается при нажатии кнопки подтверждения в интерфейсе маяка.
	 */
	public void setEffects(
			Optional<RegistryEntry<StatusEffect>> primary,
			Optional<RegistryEntry<StatusEffect>> secondary
	) {
		if (!paymentSlot.hasStack()) {
			return;
		}

		propertyDelegate.set(PROP_PRIMARY_EFFECT, getRawIdForStatusEffect(primary.orElse(null)));
		propertyDelegate.set(PROP_SECONDARY_EFFECT, getRawIdForStatusEffect(secondary.orElse(null)));
		paymentSlot.takeStack(1);
		context.run(World::markDirty);
	}

	public boolean hasPayment() {
		return !payment.getStack(PAYMENT_SLOT_ID).isEmpty();
	}

	static class PaymentSlot extends Slot {

		public PaymentSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return stack.isIn(ItemTags.BEACON_PAYMENT_ITEMS);
		}

		@Override
		public int getMaxItemCount() {
			return 1;
		}
	}
}
