package net.minecraft.inventory;

import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.jspecify.annotations.Nullable;

/**
 * Персональный инвентарь эндер-сундука игрока.
 * Хранит 27 слотов и опционально привязывается к физической блок-сущности
 * {@link EnderChestBlockEntity} для делегирования событий открытия/закрытия
 * (анимация крышки, звуки). При закрытии привязка к блок-сущности сбрасывается.
 */
public class EnderChestInventory extends SimpleInventory {

	private static final int ENDER_CHEST_SIZE = 27;

	private @Nullable EnderChestBlockEntity activeBlockEntity;

	public EnderChestInventory() {
		super(ENDER_CHEST_SIZE);
	}

	public void setActiveBlockEntity(EnderChestBlockEntity blockEntity) {
		activeBlockEntity = blockEntity;
	}

	public boolean isActiveBlockEntity(EnderChestBlockEntity blockEntity) {
		return activeBlockEntity == blockEntity;
	}

	/**
	 * Загружает содержимое инвентаря из NBT-списка {@link StackWithSlot}.
	 * Перед загрузкой все слоты очищаются. Слоты с невалидными индексами пропускаются.
	 *
	 * @param list список пар «слот + стак» из NBT
	 */
	public void readData(ReadView.TypedListReadView<StackWithSlot> list) {
		for (int slot = 0; slot < size(); slot++) {
			setStack(slot, ItemStack.EMPTY);
		}

		for (StackWithSlot stackWithSlot : list) {
			if (stackWithSlot.isValidSlot(size())) {
				setStack(stackWithSlot.slot(), stackWithSlot.stack());
			}
		}
	}

	/**
	 * Сохраняет непустые слоты инвентаря в NBT-список {@link StackWithSlot}.
	 *
	 * @param list аппендер для записи пар «слот + стак»
	 */
	public void writeData(WriteView.ListAppender<StackWithSlot> list) {
		for (int slot = 0; slot < size(); slot++) {
			ItemStack stack = getStack(slot);

			if (!stack.isEmpty()) {
				list.add(new StackWithSlot(slot, stack));
			}
		}
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		if (activeBlockEntity != null && !activeBlockEntity.canPlayerUse(player)) {
			return false;
		}

		return super.canPlayerUse(player);
	}

	@Override
	public void onOpen(ContainerUser user) {
		if (activeBlockEntity != null) {
			activeBlockEntity.onOpen(user);
		}

		super.onOpen(user);
	}

	@Override
	public void onClose(ContainerUser user) {
		if (activeBlockEntity != null) {
			activeBlockEntity.onClose(user);
		}

		super.onClose(user);
		activeBlockEntity = null;
	}
}
