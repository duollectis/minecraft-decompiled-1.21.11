package net.minecraft.advancement.criterion;

import net.minecraft.predicate.entity.LootContextPredicateValidator;

/**
 * Маркерный интерфейс для условий критерия достижения. Реализации содержат предикаты,
 * которые проверяются при каждом срабатывании соответствующего критерия.
 */
public interface CriterionConditions {

	void validate(LootContextPredicateValidator validator);
}
