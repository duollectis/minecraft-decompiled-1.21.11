package net.minecraft.client.gl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringHelper;
import net.minecraft.util.path.PathUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Препроцессор GLSL-импортов: разворачивает директивы {@code #moj_import} в исходный код шейдера.
 * Поддерживает относительные ({@code "file.glsl"}) и абсолютные ({@code <file.glsl>}) пути.
 * Рекурсивно обрабатывает вложенные импорты и вставляет директивы {@code #line} для корректной
 * диагностики ошибок компилятора.
 */
@Environment(EnvType.CLIENT)
public abstract class GlImportProcessor {

	private static final String MULTI_LINE_COMMENT_PATTERN = "/\\*(?:[^*]|\\*+[^*/])*\\*+/";
	private static final String SINGLE_LINE_COMMENT_PATTERN = "//[^\\v]*";
	private static final Pattern MOJ_IMPORT_PATTERN = Pattern.compile(
		"(#(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*moj_import(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*(?:\"(.*)\"|<(.*)>))"
	);
	private static final Pattern IMPORT_VERSION_PATTERN = Pattern.compile(
		"(#(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*version(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*(\\d+))\\b"
	);
	private static final Pattern TRAILING_WHITESPACE_PATTERN = Pattern.compile(
		"(?:^|\\v)(?:\\s|/\\*(?:[^*]|\\*+[^*/])*\\*+/|(//[^\\v]*))*\\z"
	);

	public List<String> readSource(String source) {
		Context context = new Context();
		List<String> lines = parseImports(source, context, "");
		lines.set(0, readImport(lines.get(0), context.column));
		return lines;
	}

	/**
	 * Загружает содержимое импортируемого файла по имени.
	 *
	 * @param inline {@code true} — относительный путь (кавычки), {@code false} — абсолютный (угловые скобки)
	 * @param name путь к импортируемому файлу
	 * @return содержимое файла или {@code null} если файл не найден
	 */
	public abstract @Nullable String loadImport(boolean inline, String name);

	/**
	 * Вставляет директивы {@code #define} из {@code defines} сразу после первой строки (обычно {@code #version}).
	 * Добавляет {@code #line 1 0} для восстановления нумерации строк после блока define.
	 */
	public static String addDefines(String source, Defines defines) {
		if (defines.isEmpty()) {
			return source;
		}

		int newlinePos = source.indexOf(10);
		int afterNewline = newlinePos + 1;
		return source.substring(0, afterNewline) + defines.toSource() + "#line 1 0\n" + source.substring(afterNewline);
	}

	private List<String> parseImports(String source, Context context, String basePath) {
		int fileIndex = context.line;
		int searchFrom = 0;
		String lineDirective = "";
		List<String> result = Lists.newArrayList();
		Matcher matcher = MOJ_IMPORT_PATTERN.matcher(source);

		while (matcher.find()) {
			if (hasBogusString(source, matcher, searchFrom)) {
				continue;
			}

			String relativePath = matcher.group(2);
			boolean isInline = relativePath != null;

			if (!isInline) {
				relativePath = matcher.group(3);
			}

			if (relativePath == null) {
				continue;
			}

			String prefix = source.substring(searchFrom, matcher.start(1));
			String fullPath = basePath + relativePath;
			String importedSource = loadImport(isInline, fullPath);

			if (!Strings.isNullOrEmpty(importedSource)) {
				if (!StringHelper.endsWithLineBreak(importedSource)) {
					importedSource = importedSource + System.lineSeparator();
				}

				context.line++;
				int importFileIndex = context.line;
				List<String> importedLines = parseImports(
					importedSource,
					context,
					isInline ? PathUtil.getPosixFullPath(fullPath) : ""
				);
				importedLines.set(
					0,
					String.format(
						Locale.ROOT,
						"#line %d %d\n%s",
						0,
						importFileIndex,
						extractVersion(importedLines.get(0), context)
					)
				);

				if (!StringHelper.isBlank(prefix)) {
					result.add(prefix);
				}

				result.addAll(importedLines);
			}
			else {
				String commentedImport = isInline
					? String.format(Locale.ROOT, "/*#moj_import \"%s\"*/", relativePath)
					: String.format(Locale.ROOT, "/*#moj_import <%s>*/", relativePath);
				result.add(lineDirective + prefix + commentedImport);
			}

			int linesConsumed = StringHelper.countLines(source.substring(0, matcher.end(1)));
			lineDirective = String.format(Locale.ROOT, "#line %d %d", linesConsumed, fileIndex);
			searchFrom = matcher.end(1);
		}

		String remainder = source.substring(searchFrom);

		if (!StringHelper.isBlank(remainder)) {
			result.add(lineDirective + remainder);
		}

		return result;
	}

	private String extractVersion(String line, Context context) {
		Matcher matcher = IMPORT_VERSION_PATTERN.matcher(line);

		if (matcher.find() && isLineValid(line, matcher)) {
			context.column = Math.max(context.column, Integer.parseInt(matcher.group(2)));
			return line.substring(0, matcher.start(1))
				+ "/*" + line.substring(matcher.start(1), matcher.end(1)) + "*/"
				+ line.substring(matcher.end(1));
		}

		return line;
	}

	private String readImport(String line, int maxVersion) {
		Matcher matcher = IMPORT_VERSION_PATTERN.matcher(line);

		return matcher.find() && isLineValid(line, matcher)
			? line.substring(0, matcher.start(2))
				+ Math.max(maxVersion, Integer.parseInt(matcher.group(2)))
				+ line.substring(matcher.end(2))
			: line;
	}

	private static boolean isLineValid(String line, Matcher matcher) {
		return !hasBogusString(line, matcher, 0);
	}

	private static boolean hasBogusString(String source, Matcher matcher, int searchFrom) {
		int gap = matcher.start() - searchFrom;

		if (gap == 0) {
			return false;
		}

		Matcher trailingMatcher = TRAILING_WHITESPACE_PATTERN.matcher(source.substring(searchFrom, matcher.start()));

		if (!trailingMatcher.find()) {
			return true;
		}

		return trailingMatcher.end(1) == matcher.start();
	}

	@Environment(EnvType.CLIENT)
	static final class Context {

		int column;
		int line;
	}
}
