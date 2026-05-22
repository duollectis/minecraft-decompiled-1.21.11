package net.minecraft.structure.rule.blockentity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

/**
 * Реализация {@link RuleBlockEntityModifier}, назначающая таблицу лута блок-сущности.
 * Записывает ключ таблицы лута и случайный seed в NBT, что заставляет блок-сущность
 * (например, сундук) генерировать содержимое из указанной таблицы при первом открытии.
 */
public class AppendLootRuleBlockEntityModifier implements RuleBlockEntityModifier {

	public static final MapCodec<AppendLootRuleBlockEntityModifier> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(LootTable.TABLE_KEY.fieldOf("loot_table").forGetter(modifier -> modifier.lootTable))
			.apply(instance, AppendLootRuleBlockEntityModifier::new)
	);

	private final RegistryKey<LootTable> lootTable;

	public AppendLootRuleBlockEntityModifier(RegistryKey<LootTable> lootTable) {
		this.lootTable = lootTable;
	}

	@Override
	public NbtCompound modifyBlockEntityNbt(Random random, @Nullable NbtCompound nbt) {
		NbtCompound result = nbt == null ? new NbtCompound() : nbt.copy();
		result.put("LootTable", LootTable.TABLE_KEY, lootTable);
		result.putLong("LootTableSeed", random.nextLong());

		return result;
	}

	@Override
	public RuleBlockEntityModifierType<?> getType() {
		return RuleBlockEntityModifierType.APPEND_LOOT;
	}
}
