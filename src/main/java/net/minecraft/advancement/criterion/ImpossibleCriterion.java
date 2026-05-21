package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.predicate.entity.LootContextPredicateValidator;

/**
 * {@code ImpossibleCriterion}.
 */
public class ImpossibleCriterion implements Criterion<ImpossibleCriterion.Conditions> {

	@Override
	public void beginTrackingCondition(
			PlayerAdvancementTracker manager,
			Criterion.ConditionsContainer<ImpossibleCriterion.Conditions> conditions
	) {
	}

	@Override
	public void endTrackingCondition(
			PlayerAdvancementTracker manager,
			Criterion.ConditionsContainer<ImpossibleCriterion.Conditions> conditions
	) {
	}

	@Override
	public void endTracking(PlayerAdvancementTracker tracker) {
	}

	@Override
	public Codec<ImpossibleCriterion.Conditions> getConditionsCodec() {
		return ImpossibleCriterion.Conditions.CODEC;
	}

	/**
	 * {@code Conditions}.
	 */
	public record Conditions() implements CriterionConditions {

		public static final Codec<ImpossibleCriterion.Conditions>
				CODEC =
				MapCodec.unitCodec(new ImpossibleCriterion.Conditions());

		@Override
		public void validate(LootContextPredicateValidator validator) {
		}
	}
}
