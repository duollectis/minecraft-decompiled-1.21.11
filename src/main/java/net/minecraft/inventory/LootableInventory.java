package net.minecraft.inventory;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Инвентарь, способный генерировать содержимое из лут-таблицы при первом открытии.
 * Реализующие классы должны хранить ключ таблицы и сид до момента генерации.
 */
public interface LootableInventory extends Inventory {

	String LOOT_TABLE_KEY = "LootTable";
	String LOOT_TABLE_SEED_KEY = "LootTableSeed";

	@Nullable RegistryKey<LootTable> getLootTable();

	void setLootTable(@Nullable RegistryKey<LootTable> lootTable);

	default void setLootTable(RegistryKey<LootTable> lootTableId, long lootTableSeed) {
		setLootTable(lootTableId);
		setLootTableSeed(lootTableSeed);
	}

	long getLootTableSeed();

	void setLootTableSeed(long lootTableSeed);

	BlockPos getPos();

	@Nullable World getWorld();

	/**
	 * Назначает лут-таблицу блок-сущности по позиции, если она реализует {@link LootableInventory}.
	 *
	 * @param world       мир, в котором находится блок-сущность
	 * @param random      генератор случайных чисел для создания сида
	 * @param pos         позиция блок-сущности
	 * @param lootTableId ключ лут-таблицы для назначения
	 */
	static void setLootTable(BlockView world, Random random, BlockPos pos, RegistryKey<LootTable> lootTableId) {
		if (world.getBlockEntity(pos) instanceof LootableInventory lootableInventory) {
			lootableInventory.setLootTable(lootTableId, random.nextLong());
		}
	}

	/**
	 * Читает ключ лут-таблицы и сид из NBT-представления.
	 *
	 * @param view источник данных
	 * @return {@code true}, если лут-таблица была прочитана (ключ присутствовал в данных)
	 */
	default boolean readLootTable(ReadView view) {
		RegistryKey<LootTable> lootTableKey = view.<RegistryKey<LootTable>>read(LOOT_TABLE_KEY, LootTable.TABLE_KEY)
			.orElse(null);

		setLootTable(lootTableKey);
		setLootTableSeed(view.getLong(LOOT_TABLE_SEED_KEY, 0L));

		return lootTableKey != null;
	}

	/**
	 * Записывает ключ лут-таблицы и сид в NBT-представление.
	 * Сид записывается только если он ненулевой.
	 *
	 * @param view целевое представление для записи
	 * @return {@code true}, если лут-таблица была записана (ключ не {@code null})
	 */
	default boolean writeLootTable(WriteView view) {
		RegistryKey<LootTable> lootTableKey = getLootTable();

		if (lootTableKey == null) {
			return false;
		}

		view.put(LOOT_TABLE_KEY, LootTable.TABLE_KEY, lootTableKey);

		long seed = getLootTableSeed();

		if (seed != 0L) {
			view.putLong(LOOT_TABLE_SEED_KEY, seed);
		}

		return true;
	}

	/**
	 * Генерирует содержимое инвентаря из привязанной лут-таблицы.
	 * Вызывается при первом открытии контейнера игроком.
	 * После генерации лут-таблица сбрасывается в {@code null}.
	 *
	 * @param player игрок, открывший контейнер; может быть {@code null} для безликой генерации
	 */
	default void generateLoot(@Nullable PlayerEntity player) {
		World world = getWorld();
		BlockPos blockPos = getPos();
		RegistryKey<LootTable> lootTableKey = getLootTable();

		if (lootTableKey == null || world == null || world.getServer() == null) {
			return;
		}

		LootTable lootTable = world.getServer().getReloadableRegistries().getLootTable(lootTableKey);

		if (player instanceof ServerPlayerEntity serverPlayer) {
			Criteria.PLAYER_GENERATES_CONTAINER_LOOT.trigger(serverPlayer, lootTableKey);
		}

		setLootTable(null);

		LootWorldContext.Builder builder = new LootWorldContext.Builder((ServerWorld) world)
			.add(LootContextParameters.ORIGIN, Vec3d.ofCenter(blockPos));

		if (player != null) {
			builder.luck(player.getLuck()).add(LootContextParameters.THIS_ENTITY, player);
		}

		lootTable.supplyInventory(this, builder.build(LootContextTypes.CHEST), getLootTableSeed());
	}
}
