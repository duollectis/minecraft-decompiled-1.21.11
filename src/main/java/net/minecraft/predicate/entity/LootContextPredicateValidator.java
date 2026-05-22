package net.minecraft.predicate.entity;

import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.context.ContextType;

import java.util.List;
import java.util.Optional;

/**
 * Утилита для валидации {@link LootContextPredicate} в контексте системы достижений.
 * Оборачивает {@link ErrorReporter} и реестр условий для удобного создания репортеров.
 */
public class LootContextPredicateValidator {

	private final ErrorReporter errorReporter;
	private final RegistryEntryLookup.RegistryLookup conditionsLookup;

	public LootContextPredicateValidator(
			ErrorReporter errorReporter,
			RegistryEntryLookup.RegistryLookup conditionsLookup
	) {
		this.errorReporter = errorReporter;
		this.conditionsLookup = conditionsLookup;
	}

	public void validateEntityPredicate(Optional<LootContextPredicate> predicate, String path) {
		predicate.ifPresent(p -> validateEntityPredicate(p, path));
	}

	public void validateEntityPredicates(List<LootContextPredicate> predicates, String path) {
		validate(predicates, LootContextTypes.ADVANCEMENT_ENTITY, path);
	}

	public void validateEntityPredicate(LootContextPredicate predicate, String path) {
		validate(predicate, LootContextTypes.ADVANCEMENT_ENTITY, path);
	}

	public void validate(LootContextPredicate predicate, ContextType type, String path) {
		predicate.validateConditions(
				new LootTableReporter(errorReporter.makeChild(new ErrorReporter.MapElementContext(path)), type, conditionsLookup)
		);
	}

	public void validate(List<LootContextPredicate> predicates, ContextType type, String path) {
		for (int index = 0; index < predicates.size(); index++) {
			predicates.get(index).validateConditions(
					new LootTableReporter(
							errorReporter.makeChild(new ErrorReporter.NamedListElementContext(path, index)),
							type,
							conditionsLookup
					)
			);
		}
	}
}
