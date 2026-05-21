package net.minecraft.advancement.criterion;

import net.minecraft.predicate.entity.LootContextPredicateValidator;

/**
 * {@code CriterionConditions}.
 */
public interface CriterionConditions {

	void validate(LootContextPredicateValidator validator);
}
