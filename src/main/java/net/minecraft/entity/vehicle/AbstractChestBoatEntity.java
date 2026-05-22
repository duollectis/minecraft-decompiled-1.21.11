package net.minecraft.entity.vehicle;

import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.RideableInventory;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Базовый класс для лодок и плотов со встроенным сундуком.
 * Вмещает одного пассажира; при уничтожении рассыпает содержимое инвентаря.
 */
public abstract class AbstractChestBoatEntity extends AbstractBoatEntity implements RideableInventory, VehicleInventory {

	private static final int INVENTORY_SIZE = 27;

	private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
	private @Nullable RegistryKey<LootTable> lootTable;
	private long lootTableSeed;

	public AbstractChestBoatEntity(
			EntityType<? extends AbstractChestBoatEntity> entityType,
			World world,
			Supplier<Item> supplier
	) {
		super(entityType, world, supplier);
	}

	@Override
	protected float getPassengerHorizontalOffset() {
		return 0.15F;
	}

	@Override
	protected int getMaxPassengers() {
		return 1;
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		writeInventoryToData(view);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		readInventoryFromData(view);
	}

	@Override
	public void killAndDropSelf(ServerWorld world, DamageSource damageSource) {
		killAndDropItem(world, asItem());
		onBroken(damageSource, world, this);
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		if (!getEntityWorld().isClient() && reason.shouldDestroy()) {
			ItemScatterer.spawn(getEntityWorld(), this, this);
		}

		super.remove(reason);
	}

	/**
	 * Обрабатывает взаимодействие игрока: если пассажир может сесть и не отменяет взаимодействие —
	 * пропускает открытие инвентаря; иначе открывает сундук.
	 */
	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		ActionResult result = super.interact(player, hand);

		if (result != ActionResult.PASS) {
			return result;
		}

		if (canAddPassenger(player) && !player.shouldCancelInteraction()) {
			return ActionResult.PASS;
		}

		ActionResult openResult = open(player);

		if (openResult.isAccepted() && player.getEntityWorld() instanceof ServerWorld serverWorld) {
			emitGameEvent(GameEvent.CONTAINER_OPEN, player);
			PiglinBrain.onGuardedBlockInteracted(serverWorld, player, true);
		}

		return openResult;
	}

	@Override
	public void openInventory(PlayerEntity player) {
		player.openHandledScreen(this);

		if (player.getEntityWorld() instanceof ServerWorld serverWorld) {
			emitGameEvent(GameEvent.CONTAINER_OPEN, player);
			PiglinBrain.onGuardedBlockInteracted(serverWorld, player, true);
		}
	}

	@Override
	public void clear() {
		clearInventory();
	}

	@Override
	public int size() {
		return INVENTORY_SIZE;
	}

	@Override
	public ItemStack getStack(int slot) {
		return getInventoryStack(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		return removeInventoryStack(slot, amount);
	}

	@Override
	public ItemStack removeStack(int slot) {
		return removeInventoryStack(slot);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		setInventoryStack(slot, stack);
	}

	@Override
	public StackReference getStackReference(int slot) {
		return getInventoryStackReference(slot);
	}

	@Override
	public void markDirty() {
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return canPlayerAccess(player);
	}

	@Override
	public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
		if (lootTable != null && player.isSpectator()) {
			return null;
		}

		generateLoot(playerInventory.player);
		return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this);
	}

	public void generateLoot(@Nullable PlayerEntity player) {
		generateInventoryLoot(player);
	}

	@Override
	public @Nullable RegistryKey<LootTable> getLootTable() {
		return lootTable;
	}

	@Override
	public void setLootTable(@Nullable RegistryKey<LootTable> lootTable) {
		this.lootTable = lootTable;
	}

	@Override
	public long getLootTableSeed() {
		return lootTableSeed;
	}

	@Override
	public void setLootTableSeed(long seed) {
		lootTableSeed = seed;
	}

	@Override
	public DefaultedList<ItemStack> getInventory() {
		return inventory;
	}

	@Override
	public void resetInventory() {
		inventory = DefaultedList.ofSize(size(), ItemStack.EMPTY);
	}

	@Override
	public void onClose(ContainerUser user) {
		getEntityWorld().emitGameEvent(
				GameEvent.CONTAINER_CLOSE,
				getEntityPos(),
				GameEvent.Emitter.of(user.asLivingEntity())
		);
	}
}
