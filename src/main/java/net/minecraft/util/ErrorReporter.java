package net.minecraft.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.registry.RegistryKey;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Иерархический коллектор ошибок сериализации/десериализации.
 * <p>
 * Позволяет накапливать ошибки с контекстом (путём в структуре данных)
 * и выводить их в виде читаемого дерева. Поддерживает создание дочерних
 * репортеров для вложенных контекстов через {@link #makeChild(Context)}.
 */
public interface ErrorReporter {

	ErrorReporter EMPTY = new ErrorReporter() {
		@Override
		public ErrorReporter makeChild(Context context) {
			return this;
		}

		@Override
		public void report(Error error) {
		}
	};

	/**
	 * Создаёт дочерний репортер с указанным контекстом.
	 * Ошибки дочернего репортера будут включать путь к данному контексту.
	 *
	 * @param context контекст вложенного уровня (имя поля, индекс элемента и т.д.)
	 * @return дочерний репортер
	 */
	ErrorReporter makeChild(Context context);

	void report(Error error);

	/**
	 * Контекст одного уровня иерархии ошибок.
	 * Предоставляет строковое имя для построения пути к ошибке.
	 */
	@FunctionalInterface
	interface Context {

		String getName();
	}

	/**
	 * Контекст критерия достижения.
	 *
	 * @param name имя критерия
	 */
	record CriterionContext(String name) implements Context {

		@Override
		public String getName() {
			return name;
		}
	}

	/** Описание одной ошибки с текстовым сообщением. */
	interface Error {

		String getMessage();
	}

	/**
	 * Базовая реализация {@link ErrorReporter} с накоплением ошибок в общем хранилище.
	 * Все дочерние репортеры разделяют одно и то же множество ошибок с корневым репортером.
	 */
	class Impl implements ErrorReporter {

		public static final Context CONTEXT = () -> "";

		private final @Nullable Impl parent;
		private final Context context;
		private final Set<ErrorEntry> errors;

		public Impl() {
			this(CONTEXT);
		}

		public Impl(Context context) {
			this.parent = null;
			this.errors = new LinkedHashSet<>();
			this.context = context;
		}

		private Impl(Impl parent, Context context) {
			this.errors = parent.errors;
			this.parent = parent;
			this.context = context;
		}

		@Override
		public ErrorReporter makeChild(Context context) {
			return new Impl(this, context);
		}

		@Override
		public void report(Error error) {
			errors.add(new ErrorEntry(this, error));
		}

		public boolean isEmpty() {
			return errors.isEmpty();
		}

		/**
		 * Передаёт все накопленные ошибки в указанный потребитель.
		 * Каждая ошибка передаётся вместе с полным путём контекста в виде строки.
		 *
		 * @param consumer потребитель пар (путь, ошибка)
		 */
		public void apply(BiConsumer<String, Error> consumer) {
			List<Context> contextPath = new ArrayList<>();
			StringBuilder pathBuilder = new StringBuilder();

			for (ErrorEntry entry : errors) {
				for (Impl current = entry.source(); current != null; current = current.parent) {
					contextPath.add(current.context);
				}

				for (int i = contextPath.size() - 1; i >= 0; i--) {
					pathBuilder.append(contextPath.get(i).getName());
				}

				consumer.accept(pathBuilder.toString(), entry.error());
				pathBuilder.setLength(0);
				contextPath.clear();
			}
		}

		/**
		 * Возвращает все ошибки в виде компактной строки, сгруппированной по пути.
		 *
		 * @return многострочная строка с ошибками
		 */
		public String getErrorsAsString() {
			Multimap<String, Error> grouped = HashMultimap.create();
			apply(grouped::put);

			return grouped.asMap()
				.entrySet()
				.stream()
				.map(entry -> " at " + entry.getKey() + ": "
					+ ((Collection<Error>) entry.getValue()).stream()
					.map(Error::getMessage)
					.collect(Collectors.joining("; "))
				)
				.collect(Collectors.joining("\n"));
		}

		/**
		 * Возвращает все ошибки в виде подробного дерева с отступами.
		 *
		 * @return многострочная строка с иерархическим деревом ошибок
		 */
		public String getErrorsAsLongString() {
			List<Context> contextPath = new ArrayList<>();
			ErrorList rootList = new ErrorList(context);

			for (ErrorEntry entry : errors) {
				for (Impl current = entry.source(); current != this; current = current.parent) {
					contextPath.add(current.context);
				}

				ErrorList currentList = rootList;

				for (int i = contextPath.size() - 1; i >= 0; i--) {
					currentList = currentList.get(contextPath.get(i));
				}

				contextPath.clear();
				currentList.errors.add(entry.error());
			}

			return String.join("\n", rootList.getMessages());
		}

		record ErrorEntry(Impl source, Error error) {
		}

		record ErrorList(
			Context element,
			List<Error> errors,
			Map<Context, ErrorList> children
		) {

			public ErrorList(Context context) {
				this(context, new ArrayList<>(), new LinkedHashMap<>());
			}

			public ErrorList get(Context context) {
				return children.computeIfAbsent(context, ErrorList::new);
			}

			/**
			 * Рекурсивно строит список строк для отображения дерева ошибок.
			 * Применяет компактный вывод для одиночных ошибок и развёрнутый — для множественных.
			 *
			 * @return список строк для данного узла дерева
			 */
			public List<String> getMessages() {
				int errorCount = errors.size();
				int childCount = children.size();

				if (errorCount == 0 && childCount == 0) {
					return List.of();
				}

				if (errorCount == 0 && childCount == 1) {
					List<String> messages = new ArrayList<>();
					children.forEach((ctx, child) -> messages.addAll(child.getMessages()));
					messages.set(0, element.getName() + messages.get(0));
					return messages;
				}

				if (errorCount == 1 && childCount == 0) {
					return List.of(element.getName() + ": " + errors.getFirst().getMessage());
				}

				List<String> messages = new ArrayList<>();
				children.forEach((ctx, child) -> messages.addAll(child.getMessages()));
				messages.replaceAll(message -> "  " + message);

				for (Error error : errors) {
					messages.add("  " + error.getMessage());
				}

				messages.addFirst(element.getName() + ":");
				return messages;
			}
		}
	}

	/**
	 * Контекст элемента списка по индексу.
	 *
	 * @param index индекс элемента в списке
	 */
	record ListElementContext(int index) implements Context {

		@Override
		public String getName() {
			return "[" + index + "]";
		}
	}

	/**
	 * Реализация {@link Impl} с автоматическим логированием накопленных ошибок при закрытии.
	 */
	class Logging extends Impl implements AutoCloseable {

		private final Logger logger;

		public Logging(Logger logger) {
			this.logger = logger;
		}

		public Logging(Context context, Logger logger) {
			super(context);
			this.logger = logger;
		}

		@Override
		public void close() {
			if (!isEmpty()) {
				logger.warn("[{}] Serialization errors:\n{}", logger.getName(), getErrorsAsLongString());
			}
		}
	}

	/**
	 * Контекст таблицы лута по ключу реестра.
	 *
	 * @param id ключ реестра таблицы лута
	 */
	record LootTableContext(RegistryKey<?> id) implements Context {

		@Override
		public String getName() {
			return "{" + id.getValue() + "@" + id.getRegistry() + "}";
		}
	}

	/**
	 * Контекст поля карты по строковому ключу.
	 *
	 * @param key ключ поля
	 */
	record MapElementContext(String key) implements Context {

		@Override
		public String getName() {
			return "." + key;
		}
	}

	/**
	 * Контекст именованного элемента списка (поле + индекс).
	 *
	 * @param key   имя поля
	 * @param index индекс элемента
	 */
	record NamedListElementContext(String key, int index) implements Context {

		@Override
		public String getName() {
			return "." + key + "[" + index + "]";
		}
	}

	/**
	 * Контекст ссылки на таблицу лута.
	 *
	 * @param id ключ реестра таблицы лута
	 */
	record ReferenceLootTableContext(RegistryKey<?> id) implements Context {

		@Override
		public String getName() {
			return "->{" + id.getValue() + "@" + id.getRegistry() + "}";
		}
	}
}
