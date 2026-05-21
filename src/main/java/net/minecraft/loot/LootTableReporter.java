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
 * {@code LootTableReporter}.
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
				this.errorReporter.makeChild(context),
				this.contextType,
				this.dataLookup,
				this.referenceStack
		);
	}

	public LootTableReporter makeChild(ErrorReporter.Context context, RegistryKey<?> key) {
		Set<RegistryKey<?>> set = ImmutableSet.<RegistryKey<?>>builder().addAll(this.referenceStack).add(key).build();
		return new LootTableReporter(this.errorReporter.makeChild(context), this.contextType, this.dataLookup, set);
	}

	public boolean isInStack(RegistryKey<?> key) {
		return this.referenceStack.contains(key);
	}

	public void report(ErrorReporter.Error error) {
		this.errorReporter.report(error);
	}

	public void validateContext(LootContextAware contextAware) {
		Set<ContextParameter<?>> set = contextAware.getAllowedParameters();
		Set<ContextParameter<?>> set2 = Sets.difference(set, this.contextType.getAllowed());
		if (!set2.isEmpty()) {
			this.errorReporter.report(new LootTableReporter.ParametersNotProvidedError(set2));
		}
	}

	public RegistryEntryLookup.RegistryLookup getDataLookup() {
		return this.dataLookup.orElseThrow(() -> new UnsupportedOperationException("References not allowed"));
	}

	public boolean canUseReferences() {
		return this.dataLookup.isPresent();
	}

	public LootTableReporter withContextType(ContextType contextType) {
		return new LootTableReporter(this.errorReporter, contextType, this.dataLookup, this.referenceStack);
	}

	public ErrorReporter getErrorReporter() {
		return this.errorReporter;
	}

	/**
	 * {@code MissingElementError}.
	 */
	public record MissingElementError(RegistryKey<?> referenced) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Missing element " + this.referenced.getValue() + " of type " + this.referenced.getRegistry();
		}
	}

	/**
	 * {@code ParametersNotProvidedError}.
	 */
	public record ParametersNotProvidedError(Set<ContextParameter<?>> notProvided) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Parameters " + this.notProvided + " are not provided in this context";
		}
	}

	/**
	 * {@code RecursionError}.
	 */
	public record RecursionError(RegistryKey<?> referenced) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return this.referenced.getValue() + " of type " + this.referenced.getRegistry() + " is recursively called";
		}
	}

	/**
	 * {@code ReferenceNotAllowedError}.
	 */
	public record ReferenceNotAllowedError(RegistryKey<?> referenced) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Reference to " + this.referenced.getValue() + " of type " + this.referenced.getRegistry()
					+ " was used, but references are not allowed";
		}
	}
}
