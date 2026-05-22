package net.minecraft.entity.vehicle;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для вагонеток с инвентарём (сундук, воронка).
 * Управляет сериализацией инвентаря, лут-таблицами и замедлением в зависимости от заполненности.
 */
public abstract class StorageMinecartEntity extends AbstractMinecartEntity implements VehicleInventory {

	private static final int INVENTORY_SIZE = 36;
	private static final float BASE_SLOWDOWN = 0.98F;
	private static final float SLOWDOWN_PER_EMPTY_SLOT = 0.001F;
	private static final float WATER_SLOWDOWN_FACTOR = 0.95F;
	private static final int MAX_COMPARATOR_OUTPUT = 15;

	private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
	private @Nullable RegistryKey<LootTable> lootTable;
	private long lootTableSeed;

	protected StorageMinecartEntity(EntityType<?> entityType, World world) {
		super(entityType, world);
	}

	@Override
	public void killAndDropSelf(ServerWorld world, DamageSource damageSource) {
		super.killAndDropSelf(world, damageSource);
		onBroken(damageSource, world, this);
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
	public void remove(Entity.RemovalReason reason) {
		if (!getEntityWorld().isClient() && reason.shouldDestroy()) {
			ItemScatterer.spawn(getEntityWorld(), this, this);
		}

		super.remove(reason);
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
	public ActionResult interact(PlayerEntity player, Hand hand) {
		return open(player);
	}

	/**
	 * Замедляет вагонетку в зависимости от количества пустых слотов инвентаря.
	 * Чем больше предметов — тем медленнее вагонетка (имитация веса груза).
	 */
	@Override
	protected Vec3d applySlowdown(Vec3d velocity) {
		float slowdown = BASE_SLOWDOWN;

		if (lootTable == null) {
			int emptySlots = MAX_COMPARATOR_OUTPUT - ScreenHandler.calculateComparatorOutput(this);
			slowdown += emptySlots * SLOWDOWN_PER_EMPTY_SLOT;
		}

		if (isTouchingWater()) {
			slowdown *= WATER_SLOWDOWN_FACTOR;
		}

		return velocity.multiply(slowdown, 0.0, slowdown);
	}

	@Override
	public void clear() {
		clearInventory();
	}

	public void setLootTable(RegistryKey<LootTable> lootTable, long lootSeed) {
		this.lootTable = lootTable;
		lootTableSeed = lootSeed;
	}

	@Override
	public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
		if (lootTable != null && player.isSpectator()) {
			return null;
		}

		generateInventoryLoot(playerInventory.player);
		return getScreenHandler(syncId, playerInventory);
	}

	protected abstract ScreenHandler getScreenHandler(int syncId, PlayerInventory playerInventory);

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
}
