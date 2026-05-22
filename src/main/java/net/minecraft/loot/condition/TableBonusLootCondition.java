package net.minecraft.loot.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.dynamic.Codecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Условие лута: срабатывает с вероятностью из таблицы шансов, индексированной уровнем зачарования инструмента.
 * Если уровень превышает размер таблицы — используется последний элемент.
 */
public record TableBonusLootCondition(
	RegistryEntry<Enchantment> enchantment,
	List<Float> chances
) implements LootCondition {

	public static final MapCodec<TableBonusLootCondition> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Enchantment.ENTRY_CODEC.fieldOf("enchantment").forGetter(TableBonusLootCondition::enchantment),
			Codecs.nonEmptyList(Codec.FLOAT.listOf()).fieldOf("chances").forGetter(TableBonusLootCondition::chances)
		).apply(instance, TableBonusLootCondition::new)
	);

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.TABLE_BONUS;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of(LootContextParameters.TOOL);
	}

	@Override
	public boolean test(LootContext lootContext) {
		ItemStack tool = lootContext.get(LootContextParameters.TOOL);
		int enchantLevel = tool != null ? EnchantmentHelper.getLevel(enchantment, tool) : 0;
		float chance = chances.get(Math.min(enchantLevel, chances.size() - 1));
		return lootContext.getRandom().nextFloat() < chance;
	}

	public static LootCondition.Builder builder(RegistryEntry<Enchantment> enchantment, float... chances) {
		List<Float> chanceList = new ArrayList<>(chances.length);

		for (float chance : chances) {
			chanceList.add(chance);
		}

		return () -> new TableBonusLootCondition(enchantment, chanceList);
	}
}
