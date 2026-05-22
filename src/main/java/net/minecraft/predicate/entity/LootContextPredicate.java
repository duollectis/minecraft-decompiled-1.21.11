package net.minecraft.predicate.entity;

import com.mojang.serialization.Codec;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Util;

import java.util.List;
import java.util.function.Predicate;

/**
 * Предикат, объединяющий список {@link LootCondition} через логическое «И».
 * Используется в системе достижений и наград для проверки контекста лута.
 */
public class LootContextPredicate {

	public static final Codec<LootContextPredicate> CODEC = LootCondition.CODEC
			.listOf()
			.xmap(LootContextPredicate::new, predicate -> predicate.conditions);

	private final List<LootCondition> conditions;
	private final Predicate<LootContext> combinedCondition;

	LootContextPredicate(List<LootCondition> conditions) {
		this.conditions = conditions;
		combinedCondition = Util.allOf(conditions);
	}

	public static LootContextPredicate create(LootCondition... conditions) {
		return new LootContextPredicate(List.of(conditions));
	}

	public boolean test(LootContext context) {
		return combinedCondition.test(context);
	}

	public void validateConditions(LootTableReporter reporter) {
		for (int index = 0; index < conditions.size(); index++) {
			conditions.get(index).validate(reporter.makeChild(new ErrorReporter.ListElementContext(index)));
		}
	}
}
