package net.minecraft.util.path;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реализация {@link PathMatcher}, проверяющая, разрешён ли символьный путь
 * согласно списку правил из конфигурационного файла. Поддерживает три типа правил:
 * {@code prefix} (строковый префикс), {@code glob} и {@code regex}.
 * Кэширует скомпилированные матчеры по схеме файловой системы.
 */
public class AllowedSymlinkPathMatcher implements PathMatcher {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String COMMENT_LINE_PREFIX = "#";
	private final List<AllowedSymlinkPathMatcher.Entry> allowedEntries;
	private final Map<String, PathMatcher> matcherCache = new ConcurrentHashMap<>();

	public AllowedSymlinkPathMatcher(List<AllowedSymlinkPathMatcher.Entry> allowedEntries) {
		this.allowedEntries = allowedEntries;
	}

	/**
	 * Возвращает скомпилированный {@link PathMatcher} для заданной файловой системы.
	 * Результат кэшируется по схеме провайдера ФС, чтобы не перекомпилировать паттерны
	 * при каждом вызове.
	 */
	public PathMatcher get(FileSystem fileSystem) {
		return matcherCache.computeIfAbsent(
				fileSystem.provider().getScheme(),
				scheme -> {
					List<PathMatcher> matchers;

					try {
						matchers = allowedEntries.stream()
								.map(entry -> entry.compile(fileSystem))
								.toList();
					} catch (Exception ex) {
						LOGGER.error("Failed to compile file pattern list", ex);
						return path -> false;
					}

					return switch (matchers.size()) {
						case 0 -> path -> false;
						case 1 -> matchers.get(0);
						default -> path -> {
							for (PathMatcher matcher : matchers) {
								if (matcher.matches(path)) {
									return true;
								}
							}

							return false;
						};
					};
				}
		);
	}

	@Override
	public boolean matches(Path path) {
		return get(path.getFileSystem()).matches(path);
	}

	/**
	 * Создаёт экземпляр из текстового файла конфигурации. Строки, начинающиеся с {@code #}
	 * или пустые, игнорируются. Остальные строки разбираются как записи разрешённых путей.
	 */
	public static AllowedSymlinkPathMatcher fromReader(BufferedReader reader) {
		return new AllowedSymlinkPathMatcher(
				reader.lines()
						.flatMap(line -> AllowedSymlinkPathMatcher.Entry.readLine(line).stream())
						.toList()
		);
	}

	/**
	 * Одна запись в списке разрешённых симлинков: тип правила и сам паттерн.
	 */
	public record Entry(AllowedSymlinkPathMatcher.EntryType type, String pattern) {

		public PathMatcher compile(FileSystem fileSystem) {
			return type().compile(fileSystem, pattern);
		}

		static Optional<AllowedSymlinkPathMatcher.Entry> readLine(String line) {
			if (line.isBlank() || line.startsWith(COMMENT_LINE_PREFIX)) {
				return Optional.empty();
			}

			if (!line.startsWith("[")) {
				return Optional.of(new AllowedSymlinkPathMatcher.Entry(
						AllowedSymlinkPathMatcher.EntryType.PREFIX,
						line
				));
			}

			// ASCII 93 = ']'
			int closingBracket = line.indexOf(93, 1);

			if (closingBracket == -1) {
				throw new IllegalArgumentException("Unterminated type in line '" + line + "'");
			}

			String typeName = line.substring(1, closingBracket);
			String patternValue = line.substring(closingBracket + 1);

			return switch (typeName) {
				case "glob", "regex" -> Optional.of(new AllowedSymlinkPathMatcher.Entry(
						AllowedSymlinkPathMatcher.EntryType.DEFAULT,
						typeName + ":" + patternValue
				));
				case "prefix" -> Optional.of(new AllowedSymlinkPathMatcher.Entry(
						AllowedSymlinkPathMatcher.EntryType.PREFIX,
						patternValue
				));
				default -> throw new IllegalArgumentException(
						"Unsupported definition type in line '" + line + "'");
			};
		}

		static AllowedSymlinkPathMatcher.Entry glob(String pattern) {
			return new AllowedSymlinkPathMatcher.Entry(AllowedSymlinkPathMatcher.EntryType.DEFAULT, "glob:" + pattern);
		}

		static AllowedSymlinkPathMatcher.Entry regex(String pattern) {
			return new AllowedSymlinkPathMatcher.Entry(
					AllowedSymlinkPathMatcher.EntryType.DEFAULT,
					"regex:" + pattern
			);
		}

		static AllowedSymlinkPathMatcher.Entry prefix(String prefix) {
			return new AllowedSymlinkPathMatcher.Entry(AllowedSymlinkPathMatcher.EntryType.PREFIX, prefix);
		}
	}

	/**
	 * Стратегия компиляции паттерна в {@link PathMatcher}.
	 * {@link #DEFAULT} делегирует в {@link FileSystem#getPathMatcher(String)},
	 * {@link #PREFIX} проверяет строковый префикс пути.
	 */
	@FunctionalInterface
	public interface EntryType {

		AllowedSymlinkPathMatcher.EntryType DEFAULT = FileSystem::getPathMatcher;

		AllowedSymlinkPathMatcher.EntryType PREFIX = (fileSystem, prefix) -> path -> path.toString().startsWith(prefix);

		PathMatcher compile(FileSystem fileSystem, String pattern);
	}
}
