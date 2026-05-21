package net.minecraft.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.registry.RegistryKey;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * {@code ErrorReporter}.
 */
public interface ErrorReporter {

	ErrorReporter EMPTY = new ErrorReporter() {
		@Override
		public ErrorReporter makeChild(ErrorReporter.Context context) {
			return this;
		}

		@Override
		public void report(ErrorReporter.Error error) {
		}
	};

	ErrorReporter makeChild(ErrorReporter.Context context);

	void report(ErrorReporter.Error error);

	@FunctionalInterface
	/**
	 * {@code Context}.
	 */
	public interface Context {

		String getName();
	}

	/**
	 * {@code CriterionContext}.
	 */
	public record CriterionContext(String name) implements ErrorReporter.Context {

		@Override
		public String getName() {
			return this.name;
		}
	}

	/**
	 * {@code Error}.
	 */
	public interface Error {

		String getMessage();
	}

	/**
	 * {@code Impl}.
	 */
	public static class Impl implements ErrorReporter {

		public static final ErrorReporter.Context CONTEXT = () -> "";
		private final ErrorReporter.@Nullable Impl parent;
		private final ErrorReporter.Context context;
		private final Set<ErrorReporter.Impl.ErrorEntry> errors;

		public Impl() {
			this(CONTEXT);
		}

		public Impl(ErrorReporter.Context context) {
			this.parent = null;
			this.errors = new LinkedHashSet<>();
			this.context = context;
		}

		private Impl(ErrorReporter.Impl parent, ErrorReporter.Context context) {
			this.errors = parent.errors;
			this.parent = parent;
			this.context = context;
		}

		@Override
		public ErrorReporter makeChild(ErrorReporter.Context context) {
			return new ErrorReporter.Impl(this, context);
		}

		@Override
		public void report(ErrorReporter.Error error) {
			this.errors.add(new ErrorReporter.Impl.ErrorEntry(this, error));
		}

		public boolean isEmpty() {
			return this.errors.isEmpty();
		}

		public void apply(BiConsumer<String, ErrorReporter.Error> consumer) {
			List<ErrorReporter.Context> list = new ArrayList<>();
			StringBuilder stringBuilder = new StringBuilder();

			for (ErrorReporter.Impl.ErrorEntry errorEntry : this.errors) {
				for (ErrorReporter.Impl impl = errorEntry.source; impl != null; impl = impl.parent) {
					list.add(impl.context);
				}

				for (int i = list.size() - 1; i >= 0; i--) {
					stringBuilder.append(list.get(i).getName());
				}

				consumer.accept(stringBuilder.toString(), errorEntry.error());
				stringBuilder.setLength(0);
				list.clear();
			}
		}

		public String getErrorsAsString() {
			Multimap<String, ErrorReporter.Error> multimap = HashMultimap.create();
			this.apply(multimap::put);
			return multimap.asMap()
			               .entrySet()
			               .stream()
			               .map(
					               entry -> " at "
							               + (String) entry.getKey()
							               + ": "
							               + ((Collection<ErrorReporter.Error>) entry.getValue())
							               .stream()
							               .map(e -> e.getMessage())
							               .collect(Collectors.joining("; "))
			               )
			               .collect(Collectors.joining("\n"));
		}

		public String getErrorsAsLongString() {
			List<ErrorReporter.Context> list = new ArrayList<>();
			ErrorReporter.Impl.ErrorList errorList = new ErrorReporter.Impl.ErrorList(this.context);

			for (ErrorReporter.Impl.ErrorEntry errorEntry : this.errors) {
				for (ErrorReporter.Impl impl = errorEntry.source; impl != this; impl = impl.parent) {
					list.add(impl.context);
				}

				ErrorReporter.Impl.ErrorList errorList2 = errorList;

				for (int i = list.size() - 1; i >= 0; i--) {
					errorList2 = errorList2.get(list.get(i));
				}

				list.clear();
				errorList2.errors.add(errorEntry.error);
			}

			return String.join("\n", errorList.getMessages());
		}

		/**
		 * {@code ErrorEntry}.
		 */
		record ErrorEntry(ErrorReporter.Impl source, ErrorReporter.Error error) {
		}

		/**
		 * {@code ErrorList}.
		 */
		record ErrorList(
				ErrorReporter.Context element,
				List<ErrorReporter.Error> errors,
				Map<ErrorReporter.Context, ErrorReporter.Impl.ErrorList> children
		) {

			public ErrorList(ErrorReporter.Context context) {
				this(context, new ArrayList<>(), new LinkedHashMap<>());
			}

			public ErrorReporter.Impl.ErrorList get(ErrorReporter.Context context) {
				return this.children.computeIfAbsent(context, ErrorReporter.Impl.ErrorList::new);
			}

			public List<String> getMessages() {
				int i = this.errors.size();
				int j = this.children.size();
				if (i == 0 && j == 0) {
					return List.of();
				}
				else if (i == 0 && j == 1) {
					List<String> list = new ArrayList<>();
					this.children.forEach((context, errors) -> list.addAll(errors.getMessages()));
					list.set(0, this.element.getName() + list.get(0));
					return list;
				}
				else if (i == 1 && j == 0) {
					return List.of(this.element.getName() + ": " + this.errors.getFirst().getMessage());
				}
				else {
					List<String> list = new ArrayList<>();
					this.children.forEach((context, errors) -> list.addAll(errors.getMessages()));
					list.replaceAll(message -> "  " + message);

					for (ErrorReporter.Error error : this.errors) {
						list.add("  " + error.getMessage());
					}

					list.addFirst(this.element.getName() + ":");
					return list;
				}
			}
		}
	}

	/**
	 * {@code ListElementContext}.
	 */
	public record ListElementContext(int index) implements ErrorReporter.Context {

		@Override
		public String getName() {
			return "[" + this.index + "]";
		}
	}

	/**
	 * {@code Logging}.
	 */
	public static class Logging extends ErrorReporter.Impl implements AutoCloseable {

		private final Logger logger;

		public Logging(Logger logger) {
			this.logger = logger;
		}

		public Logging(ErrorReporter.Context context, Logger logger) {
			super(context);
			this.logger = logger;
		}

		@Override
		public void close() {
			if (!this.isEmpty()) {
				this.logger.warn("[{}] Serialization errors:\n{}", this.logger.getName(), this.getErrorsAsLongString());
			}
		}
	}

	/**
	 * {@code LootTableContext}.
	 */
	public record LootTableContext(RegistryKey<?> id) implements ErrorReporter.Context {

		@Override
		public String getName() {
			return "{" + this.id.getValue() + "@" + this.id.getRegistry() + "}";
		}
	}

	/**
	 * {@code MapElementContext}.
	 */
	public record MapElementContext(String key) implements ErrorReporter.Context {

		@Override
		public String getName() {
			return "." + this.key;
		}
	}

	/**
	 * {@code NamedListElementContext}.
	 */
	public record NamedListElementContext(String key, int index) implements ErrorReporter.Context {

		@Override
		public String getName() {
			return "." + this.key + "[" + this.index + "]";
		}
	}

	/**
	 * {@code ReferenceLootTableContext}.
	 */
	public record ReferenceLootTableContext(RegistryKey<?> id) implements ErrorReporter.Context {

		@Override
		public String getName() {
			return "->{" + this.id.getValue() + "@" + this.id.getRegistry() + "}";
		}
	}
}
