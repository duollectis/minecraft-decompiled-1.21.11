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
 * {@code VehicleInventory}.
 */
public interface VehicleInventory extends Inventory, NamedScreenHandlerFactory {

	Vec3d getEntityPos();

	Box getBoundingBox();

	@Nullable RegistryKey<LootTable> getLootTable();

	void setLootTable(@Nullable RegistryKey<LootTable> lootTable);

	long getLootTableSeed();

	void setLootTableSeed(long lootTableSeed);

	DefaultedList<ItemStack> getInventory();

	void resetInventory();

	World getEntityWorld();

	boolean isRemoved();

	@Override
	default boolean isEmpty() {
		return this.isInventoryEmpty();
	}

	default void writeInventoryToData(WriteView view) {
		if (this.getLootTable() != null) {
			view.putString("LootTable", this.getLootTable().getValue().toString());
			if (this.getLootTableSeed() != 0L) {
				view.putLong("LootTableSeed", this.getLootTableSeed());
			}
		}
		else {
			Inventories.writeData(view, this.getInventory());
		}
	}

	default void readInventoryFromData(ReadView view) {
		this.resetInventory();
		RegistryKey<LootTable>
				registryKey =
				view.<RegistryKey<LootTable>>read("LootTable", LootTable.TABLE_KEY).orElse(null);
		this.setLootTable(registryKey);
		this.setLootTableSeed(view.getLong("LootTableSeed", 0L));
		if (registryKey == null) {
			Inventories.readData(view, this.getInventory());
		}
	}

	default void onBroken(DamageSource source, ServerWorld world, Entity vehicle) {
		if (world.getGameRules().getValue(GameRules.ENTITY_DROPS)) {
			ItemScatterer.spawn(world, vehicle, this);
			Entity entity = source.getSource();
			if (entity != null && entity.getType() == EntityType.PLAYER) {
				PiglinBrain.onGuardedBlockInteracted(world, (PlayerEntity) entity, true);
			}
		}
	}

	default ActionResult open(PlayerEntity player) {
		player.openHandledScreen(this);
		return ActionResult.SUCCESS;
	}

	default void generateInventoryLoot(@Nullable PlayerEntity player) {
		MinecraftServer minecraftServer = this.getEntityWorld().getServer();
		if (this.getLootTable() != null && minecraftServer != null) {
			LootTable lootTable = minecraftServer.getReloadableRegistries().getLootTable(this.getLootTable());
			if (player != null) {
				Criteria.PLAYER_GENERATES_CONTAINER_LOOT.trigger((ServerPlayerEntity) player, this.getLootTable());
			}

			this.setLootTable(null);
			LootWorldContext.Builder builder = new LootWorldContext.Builder((ServerWorld) this.getEntityWorld())
					.add(LootContextParameters.ORIGIN, this.getEntityPos());
			if (player != null) {
				builder.luck(player.getLuck()).add(LootContextParameters.THIS_ENTITY, player);
			}

			lootTable.supplyInventory(this, builder.build(LootContextTypes.CHEST), this.getLootTableSeed());
		}
	}

	default void clearInventory() {
		this.generateInventoryLoot(null);
		this.getInventory().clear();
	}

	default boolean isInventoryEmpty() {
		for (ItemStack itemStack : this.getInventory()) {
			if (!itemStack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	default ItemStack removeInventoryStack(int slot) {
		this.generateInventoryLoot(null);
		ItemStack itemStack = this.getInventory().get(slot);
		if (itemStack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		else {
			this.getInventory().set(slot, ItemStack.EMPTY);
			return itemStack;
		}
	}

	default ItemStack getInventoryStack(int slot) {
		this.generateInventoryLoot(null);
		return this.getInventory().get(slot);
	}

	default ItemStack removeInventoryStack(int slot, int amount) {
		this.generateInventoryLoot(null);
		return Inventories.splitStack(this.getInventory(), slot, amount);
	}

	default void setInventoryStack(int slot, ItemStack stack) {
		this.generateInventoryLoot(null);
		this.getInventory().set(slot, stack);
		stack.capCount(this.getMaxCount(stack));
	}

	default @Nullable StackReference getInventoryStackReference(int slot) {
		return slot >= 0 && slot < this.size() ? new StackReference() {
			@Override
			public ItemStack get() {
				return VehicleInventory.this.getInventoryStack(slot);
			}

			@Override
			public boolean set(ItemStack stack) {
				VehicleInventory.this.setInventoryStack(slot, stack);
				return true;
			}
		} : null;
	}

	default boolean canPlayerAccess(PlayerEntity player) {
		return !this.isRemoved() && player.canInteractWithEntityIn(this.getBoundingBox(), 4.0);
	}
}
