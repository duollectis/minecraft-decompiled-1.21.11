package net.minecraft.loot.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.util.context.ContextParameter;

import java.util.Optional;
import java.util.Set;

/**
 * Условие, проверяющее инструмент в контексте лута через {@link ItemPredicate}.
 */
public record MatchToolLootCondition(Optional<ItemPredicate> predicate) implements LootCondition {

	public static final MapCodec<MatchToolLootCondition> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(
				ItemPredicate.CODEC.optionalFieldOf("predicate").forGetter(MatchToolLootCondition::predicate)
			)
			.apply(instance, MatchToolLootCondition::new)
	);

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.MATCH_TOOL;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of(LootContextParameters.TOOL);
	}

	public boolean test(LootContext lootContext) {
		ItemStack tool = lootContext.get(LootContextParameters.TOOL);
		return tool != null && (predicate.isEmpty() || predicate.get().test(tool));
	}

	public static LootCondition.Builder builder(ItemPredicate.Builder predicate) {
		return () -> new MatchToolLootCondition(Optional.of(predicate.build()));
	}
}
