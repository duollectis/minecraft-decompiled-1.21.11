package net.minecraft.loot;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.minecraft.loot.context.LootContextAware;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.context.ContextType;

import java.util.Optional;
import java.util.Set;

/**
 * Репортер ошибок валидации лут-таблиц с поддержкой контекста и стека ссылок.
 *
 * <p>Отслеживает стек посещённых ключей реестра для обнаружения рекурсивных ссылок
 * и проверяет соответствие параметров контекста допустимым для данного типа таблицы.</p>
 */
public class LootTableReporter {

	private final ErrorReporter errorReporter;
	private final ContextType contextType;
	private final Optional<RegistryEntryLookup.RegistryLookup> dataLookup;
	private final Set<RegistryKey<?>> referenceStack;

	public LootTableReporter(
		ErrorReporter errorReporter,
		ContextType contextType,
		RegistryEntryLookup.RegistryLookup dataLookup
	) {
		this(errorReporter, contextType, Optional.of(dataLookup), Set.of());
	}

	public LootTableReporter(ErrorReporter errorReporter, ContextType contextType) {
		this(errorReporter, contextType, Optional.empty(), Set.of());
	}

	private LootTableReporter(
		ErrorReporter errorReporter,
		ContextType contextType,
		Optional<RegistryEntryLookup.RegistryLookup> dataLookup,
		Set<RegistryKey<?>> referenceStack
	) {
		this.errorReporter = errorReporter;
		this.contextType = contextType;
		this.dataLookup = dataLookup;
		this.referenceStack = referenceStack;
	}

	public LootTableReporter makeChild(ErrorReporter.Context context) {
		return new LootTableReporter(
			errorReporter.makeChild(context),
			contextType,
			dataLookup,
			referenceStack
		);
	}

	/**
	 * Создаёт дочерний репортер, добавляя ключ в стек посещённых ссылок
	 * для последующего обнаружения рекурсии.
	 */
	public LootTableReporter makeChild(ErrorReporter.Context context, RegistryKey<?> key) {
		Set<RegistryKey<?>> updatedStack = ImmutableSet.<RegistryKey<?>>builder()
			.addAll(referenceStack)
			.add(key)
			.build();

		return new LootTableReporter(errorReporter.makeChild(context), contextType, dataLookup, updatedStack);
	}

	public boolean isInStack(RegistryKey<?> key) {
		return referenceStack.contains(key);
	}

	public void report(ErrorReporter.Error error) {
		errorReporter.report(error);
	}

	/**
	 * Проверяет, что все параметры контекста, требуемые {@code contextAware},
	 * присутствуют в текущем типе контекста. Сообщает об ошибке при несоответствии.
	 */
	public void validateContext(LootContextAware contextAware) {
		Set<ContextParameter<?>> required = contextAware.getAllowedParameters();
		Set<ContextParameter<?>> missing = Sets.difference(required, contextType.getAllowed());

		if (!missing.isEmpty()) {
			errorReporter.report(new LootTableReporter.ParametersNotProvidedError(missing));
		}
	}

	public RegistryEntryLookup.RegistryLookup getDataLookup() {
		return dataLookup.orElseThrow(() -> new UnsupportedOperationException("References not allowed"));
	}

	public boolean canUseReferences() {
		return dataLookup.isPresent();
	}

	public LootTableReporter withContextType(ContextType contextType) {
		return new LootTableReporter(errorReporter, contextType, dataLookup, referenceStack);
	}

	public ErrorReporter getErrorReporter() {
		return errorReporter;
	}

	/** Ошибка: ссылка на несуществующий элемент реестра. */
	public record MissingElementError(RegistryKey<?> referenced) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Missing element " + referenced.getValue() + " of type " + referenced.getRegistry();
		}
	}

	/** Ошибка: требуемые параметры контекста не предоставлены в текущем типе таблицы. */
	public record ParametersNotProvidedError(Set<ContextParameter<?>> notProvided) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Parameters " + notProvided + " are not provided in this context";
		}
	}

	/** Ошибка: обнаружена рекурсивная ссылка на элемент реестра. */
	public record RecursionError(RegistryKey<?> referenced) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return referenced.getValue() + " of type " + referenced.getRegistry() + " is recursively called";
		}
	}

	/** Ошибка: ссылки запрещены в текущем контексте валидации. */
	public record ReferenceNotAllowedError(RegistryKey<?> referenced) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Reference to " + referenced.getValue() + " of type " + referenced.getRegistry()
				+ " was used, but references are not allowed";
		}
	}
}
