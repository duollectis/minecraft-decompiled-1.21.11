package net.minecraft.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShelfBlock;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.ListInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Блок-сущность полки (Shelf). Хранит до {@link #SLOT_COUNT} предметов,
 * отображаемых на поверхности блока. Поддерживает выравнивание предметов
 * по нижнему краю полки через флаг {@code align_items_to_bottom}.
 */
public class ShelfBlockEntity extends BlockEntity implements HeldItemContext, ListInventory {

	public static final int SLOT_COUNT = 3;
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String ALIGN_ITEMS_TO_BOTTOM_KEY = "align_items_to_bottom";
	private final DefaultedList<ItemStack> heldStacks = DefaultedList.ofSize(3, ItemStack.EMPTY);
	private boolean alignItemsToBottom;

	public ShelfBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.SHELF, pos, state);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		heldStacks.clear();
		Inventories.readData(view, heldStacks);
		alignItemsToBottom = view.getBoolean(ALIGN_ITEMS_TO_BOTTOM_KEY, false);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		Inventories.writeData(view, heldStacks, true);
		view.putBoolean(ALIGN_ITEMS_TO_BOTTOM_KEY, alignItemsToBottom);
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	/**
	 * Формирует NBT-данные для первоначальной синхронизации с клиентом при загрузке чанка.
	 * Включает только содержимое инвентаря и флаг выравнивания — без полного состояния сущности.
	 */
	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(getReporterContext(), LOGGER)) {
			NbtWriteView nbtWriteView = NbtWriteView.create(logging, registries);
			Inventories.writeData(nbtWriteView, heldStacks, true);
			nbtWriteView.putBoolean(ALIGN_ITEMS_TO_BOTTOM_KEY, alignItemsToBottom);
			return nbtWriteView.getNbt();
		}
	}

	@Override
	public DefaultedList<ItemStack> getHeldStacks() {
		return heldStacks;
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return Inventory.canPlayerUse(this, player);
	}

	public ItemStack swapStackNoMarkDirty(int slot, ItemStack stack) {
		ItemStack removed = removeStack(slot);
		setStackNoMarkDirty(slot, stack);
		return removed;
	}

	/**
	 * Помечает сущность как изменённую и генерирует игровое событие, если оно задано.
	 * Также уведомляет соседние блоки об изменении состояния.
	 *
	 * @param gameEvent игровое событие для генерации, или {@code null} если событие не нужно
	 */
	public void markDirty(RegistryEntry.@Nullable Reference<GameEvent> gameEvent) {
		super.markDirty();
		if (world == null) {
			return;
		}

		if (gameEvent != null) {
			world.emitGameEvent(gameEvent, pos, GameEvent.Emitter.of(getCachedState()));
		}

		getWorld().updateListeners(getPos(), getCachedState(), getCachedState(), 3);
	}

	@Override
	public void markDirty() {
		markDirty(GameEvent.BLOCK_ACTIVATE);
	}

	@Override
	protected void readComponents(ComponentsAccess components) {
		super.readComponents(components);
		components.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyTo(heldStacks);
	}

	@Override
	protected void addComponents(ComponentMap.Builder builder) {
		super.addComponents(builder);
		builder.add(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(heldStacks));
	}

	@Override
	public void removeFromCopiedStackData(WriteView view) {
		view.remove("Items");
	}

	@Override
	public World getEntityWorld() {
		return world;
	}

	@Override
	public Vec3d getEntityPos() {
		return getPos().toCenterPos();
	}

	@Override
	public float getBodyYaw() {
		return getCachedState().get(ShelfBlock.FACING).getOpposite().getPositiveHorizontalDegrees();
	}

	public boolean shouldAlignItemsToBottom() {
		return alignItemsToBottom;
	}
}
