package net.minecraft.loot.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProviderTypes;

/** Условие лута: срабатывает с заданной случайной вероятностью. */
public record RandomChanceLootCondition(LootNumberProvider chance) implements LootCondition {

	public static final MapCodec<RandomChanceLootCondition> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(LootNumberProviderTypes.CODEC.fieldOf("chance").forGetter(RandomChanceLootCondition::chance))
			.apply(instance, RandomChanceLootCondition::new)
	);

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.RANDOM_CHANCE;
	}

	@Override
	public boolean test(LootContext lootContext) {
		float chance = this.chance.nextFloat(lootContext);
		return lootContext.getRandom().nextFloat() < chance;
	}

	public static LootCondition.Builder builder(float chance) {
		return () -> new RandomChanceLootCondition(ConstantLootNumberProvider.create(chance));
	}

	public static LootCondition.Builder builder(LootNumberProvider chance) {
		return () -> new RandomChanceLootCondition(chance);
	}
}
