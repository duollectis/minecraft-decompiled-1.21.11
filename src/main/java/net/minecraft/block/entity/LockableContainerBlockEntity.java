package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для блок-сущностей с инвентарём, поддерживающих замок и пользовательское имя.
 * <p>
 * Реализует полный цикл работы с {@link ContainerLock}: проверку при открытии экрана,
 * воспроизведение звука заблокированного сундука и сериализацию замка в NBT/компоненты.
 */
public abstract class LockableContainerBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory, Nameable {

	private ContainerLock lock = ContainerLock.EMPTY;
	private @Nullable Text customName;

	protected LockableContainerBlockEntity(
			BlockEntityType<?> blockEntityType,
			BlockPos blockPos,
			BlockState blockState
	) {
		super(blockEntityType, blockPos, blockState);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		lock = ContainerLock.read(view);
		customName = tryParseCustomName(view, "CustomName");
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		lock.write(view);
		view.putNullable("CustomName", TextCodecs.CODEC, customName);
	}

	@Override
	public Text getName() {
		return customName != null ? customName : getContainerName();
	}

	@Override
	public Text getDisplayName() {
		return getName();
	}

	@Override
	public @Nullable Text getCustomName() {
		return customName;
	}

	protected abstract Text getContainerName();

	public boolean checkUnlocked(PlayerEntity player) {
		return lock.checkUnlocked(player);
	}

	/**
	 * Уведомляет игрока о заблокированном контейнере и воспроизводит звук замка на сервере.
	 */
	public static void handleLocked(Vec3d containerPos, PlayerEntity player, Text name) {
		World world = player.getEntityWorld();
		player.sendMessage(Text.translatable("container.isLocked", name), true);

		if (!world.isClient()) {
			world.playSound(
					null,
					containerPos.getX(),
					containerPos.getY(),
					containerPos.getZ(),
					SoundEvents.BLOCK_CHEST_LOCKED,
					SoundCategory.BLOCKS,
					1.0F,
					1.0F
			);
		}
	}

	public boolean isLocked() {
		return !lock.equals(ContainerLock.EMPTY);
	}

	protected abstract DefaultedList<ItemStack> getHeldStacks();

	protected abstract void setHeldStacks(DefaultedList<ItemStack> inventory);

	@Override
	public boolean isEmpty() {
		for (ItemStack itemStack : getHeldStacks()) {
			if (!itemStack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		return getHeldStacks().get(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack removed = Inventories.splitStack(getHeldStacks(), slot, amount);

		if (!removed.isEmpty()) {
			markDirty();
		}

		return removed;
	}

	@Override
	public ItemStack removeStack(int slot) {
		return Inventories.removeStack(getHeldStacks(), slot);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		getHeldStacks().set(slot, stack);
		stack.capCount(getMaxCount(stack));
		markDirty();
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return Inventory.canPlayerUse(this, player);
	}

	@Override
	public void clear() {
		getHeldStacks().clear();
	}

	@Override
	public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
		if (checkUnlocked(player)) {
			return createScreenHandler(syncId, playerInventory);
		}

		handleLocked(getPos().toCenterPos(), player, getDisplayName());
		return null;
	}

	protected abstract ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory);

	@Override
	protected void readComponents(ComponentsAccess components) {
		super.readComponents(components);
		customName = components.get(DataComponentTypes.CUSTOM_NAME);
		lock = components.getOrDefault(DataComponentTypes.LOCK, ContainerLock.EMPTY);
		components.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyTo(getHeldStacks());
	}

	@Override
	protected void addComponents(ComponentMap.Builder builder) {
		super.addComponents(builder);
		builder.add(DataComponentTypes.CUSTOM_NAME, customName);

		if (isLocked()) {
			builder.add(DataComponentTypes.LOCK, lock);
		}

		builder.add(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(getHeldStacks()));
	}

	@Override
	public void removeFromCopiedStackData(WriteView view) {
		view.remove("CustomName");
		view.remove("lock");
		view.remove("Items");
	}
}
