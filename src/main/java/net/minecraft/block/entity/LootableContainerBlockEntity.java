package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerLootComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * Расширение {@link LockableContainerBlockEntity} с поддержкой лут-таблиц.
 * <p>
 * Лут генерируется лениво при первом обращении к инвентарю. Спектаторы не могут
 * открыть контейнер с нераскрытой лут-таблицей, чтобы не раскрывать содержимое.
 */
public abstract class LootableContainerBlockEntity extends LockableContainerBlockEntity implements LootableInventory {

	protected @Nullable RegistryKey<LootTable> lootTable;
	protected long lootTableSeed = 0L;

	protected LootableContainerBlockEntity(
			BlockEntityType<?> blockEntityType,
			BlockPos blockPos,
			BlockState blockState
	) {
		super(blockEntityType, blockPos, blockState);
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
	public void setLootTableSeed(long lootTableSeed) {
		this.lootTableSeed = lootTableSeed;
	}

	@Override
	public boolean isEmpty() {
		generateLoot(null);
		return super.isEmpty();
	}

	@Override
	public ItemStack getStack(int slot) {
		generateLoot(null);
		return super.getStack(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		generateLoot(null);
		return super.removeStack(slot, amount);
	}

	@Override
	public ItemStack removeStack(int slot) {
		generateLoot(null);
		return super.removeStack(slot);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		generateLoot(null);
		super.setStack(slot, stack);
	}

	@Override
	public boolean checkUnlocked(PlayerEntity player) {
		return super.checkUnlocked(player) && (lootTable == null || !player.isSpectator());
	}

	@Override
	public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
		if (checkUnlocked(player)) {
			generateLoot(playerInventory.player);
			return createScreenHandler(syncId, playerInventory);
		}

		LockableContainerBlockEntity.handleLocked(getPos().toCenterPos(), player, getDisplayName());
		return null;
	}

	@Override
	protected void readComponents(ComponentsAccess components) {
		super.readComponents(components);
		ContainerLootComponent lootComponent = components.get(DataComponentTypes.CONTAINER_LOOT);

		if (lootComponent != null) {
			lootTable = lootComponent.lootTable();
			lootTableSeed = lootComponent.seed();
		}
	}

	@Override
	protected void addComponents(ComponentMap.Builder builder) {
		super.addComponents(builder);

		if (lootTable != null) {
			builder.add(
					DataComponentTypes.CONTAINER_LOOT,
					new ContainerLootComponent(lootTable, lootTableSeed)
			);
		}
	}

	@Override
	public void removeFromCopiedStackData(WriteView view) {
		super.removeFromCopiedStackData(view);
		view.remove("LootTable");
		view.remove("LootTableSeed");
	}
}
