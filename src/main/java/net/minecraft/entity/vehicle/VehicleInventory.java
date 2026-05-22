package net.minecraft.entity.vehicle;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

/**
 * Интерфейс для транспортных средств с инвентарём (сундуки-лодки, вагонетки-сундуки и т.д.).
 * Предоставляет стандартную реализацию работы с лут-таблицами, сериализацией инвентаря
 * и проверкой доступа игрока.
 */
public interface VehicleInventory extends Inventory, NamedScreenHandlerFactory {

	Vec3d getEntityPos();

	Box getBoundingBox();

	@Nullable RegistryKey<LootTable> getLootTable();

	void setLootTable(@Nullable RegistryKey<LootTable> lootTable);

	long getLootTableSeed();

	void setLootTableSeed(long seed);

	DefaultedList<ItemStack> getInventory();

	void resetInventory();

	World getEntityWorld();

	boolean isRemoved();

	@Override
	default boolean isEmpty() {
		return isInventoryEmpty();
	}

	default void writeInventoryToData(WriteView view) {
		RegistryKey<LootTable> table = getLootTable();

		if (table != null) {
			view.putString("LootTable", table.getValue().toString());

			if (getLootTableSeed() != 0L) {
				view.putLong("LootTableSeed", getLootTableSeed());
			}
		} else {
			Inventories.writeData(view, getInventory());
		}
	}

	default void readInventoryFromData(ReadView view) {
		resetInventory();
		RegistryKey<LootTable> table = view.<RegistryKey<LootTable>>read("LootTable", LootTable.TABLE_KEY).orElse(null);
		setLootTable(table);
		setLootTableSeed(view.getLong("LootTableSeed", 0L));

		if (table == null) {
			Inventories.readData(view, getInventory());
		}
	}

	/**
	 * Вызывается при уничтожении транспортного средства: рассыпает содержимое инвентаря
	 * и уведомляет мозг пиглина об взаимодействии с охраняемым блоком.
	 */
	default void onBroken(DamageSource source, ServerWorld world, Entity vehicle) {
		if (world.getGameRules().getValue(GameRules.ENTITY_DROPS)) {
			ItemScatterer.spawn(world, vehicle, this);
			Entity sourceEntity = source.getSource();

			if (sourceEntity != null && sourceEntity.getType() == EntityType.PLAYER) {
				PiglinBrain.onGuardedBlockInteracted(world, (PlayerEntity) sourceEntity, true);
			}
		}
	}

	default ActionResult open(PlayerEntity player) {
		player.openHandledScreen(this);
		return ActionResult.SUCCESS;
	}

	/**
	 * Генерирует лут из лут-таблицы и заполняет инвентарь.
	 * После генерации лут-таблица сбрасывается, чтобы не генерировать повторно.
	 */
	default void generateInventoryLoot(@Nullable PlayerEntity player) {
		MinecraftServer server = getEntityWorld().getServer();

		if (getLootTable() == null || server == null) {
			return;
		}

		LootTable lootTable = server.getReloadableRegistries().getLootTable(getLootTable());

		if (player != null) {
			Criteria.PLAYER_GENERATES_CONTAINER_LOOT.trigger((ServerPlayerEntity) player, getLootTable());
		}

		setLootTable(null);

		LootWorldContext.Builder builder = new LootWorldContext.Builder((ServerWorld) getEntityWorld())
				.add(LootContextParameters.ORIGIN, getEntityPos());

		if (player != null) {
			builder.luck(player.getLuck()).add(LootContextParameters.THIS_ENTITY, player);
		}

		lootTable.supplyInventory(this, builder.build(LootContextTypes.CHEST), getLootTableSeed());
	}

	default void clearInventory() {
		generateInventoryLoot(null);
		getInventory().clear();
	}

	default boolean isInventoryEmpty() {
		for (ItemStack stack : getInventory()) {
			if (!stack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	default ItemStack removeInventoryStack(int slot) {
		generateInventoryLoot(null);
		ItemStack stack = getInventory().get(slot);

		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		getInventory().set(slot, ItemStack.EMPTY);
		return stack;
	}

	default ItemStack getInventoryStack(int slot) {
		generateInventoryLoot(null);
		return getInventory().get(slot);
	}

	default ItemStack removeInventoryStack(int slot, int amount) {
		generateInventoryLoot(null);
		return Inventories.splitStack(getInventory(), slot, amount);
	}

	default void setInventoryStack(int slot, ItemStack stack) {
		generateInventoryLoot(null);
		getInventory().set(slot, stack);
		stack.capCount(getMaxCount(stack));
	}

	default @Nullable StackReference getInventoryStackReference(int slot) {
		return slot >= 0 && slot < size()
				? new StackReference() {
					@Override
					public ItemStack get() {
						return VehicleInventory.this.getInventoryStack(slot);
					}

					@Override
					public boolean set(ItemStack stack) {
						VehicleInventory.this.setInventoryStack(slot, stack);
						return true;
					}
				}
				: null;
	}

	default boolean canPlayerAccess(PlayerEntity player) {
		return !isRemoved() && player.canInteractWithEntityIn(getBoundingBox(), 4.0);
	}
}
