package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.predicate.entity.LootContextPredicateValidator;

/**
 * Критерий, который никогда не может быть выполнен.
 * Используется для достижений, которые выдаются только вручную через команды.
 */
public class ImpossibleCriterion implements Criterion<ImpossibleCriterion.Conditions> {

	@Override
	public void beginTrackingCondition(
			PlayerAdvancementTracker manager,
			Criterion.ConditionsContainer<Conditions> conditions
	) {
	}

	@Override
	public void endTrackingCondition(
			PlayerAdvancementTracker manager,
			Criterion.ConditionsContainer<Conditions> conditions
	) {
	}

	@Override
	public void endTracking(PlayerAdvancementTracker tracker) {
	}

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public record Conditions() implements CriterionConditions {

		public static final Codec<Conditions> CODEC = MapCodec.unitCodec(new Conditions());

		@Override
		public void validate(LootContextPredicateValidator validator) {
		}
	}
}
