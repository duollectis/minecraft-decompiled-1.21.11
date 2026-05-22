package net.minecraft.command;

import com.google.common.collect.ImmutableList;
import net.minecraft.server.function.CommandFunction;

import java.util.List;

/**
 * Представляет вызов макроса команды — строку с переменными вида {@code $(name)}.
 * Хранит сегменты текста между переменными и имена самих переменных.
 */
public record MacroInvocation(List<String> segments, List<String> variables) {

	private static final char MACRO_SIGIL = '$';
	private static final char MACRO_OPEN = '(';
	private static final char MACRO_CLOSE = ')';

	/**
	 * Разбирает строку команды на сегменты и переменные макроса.
	 * Переменные имеют формат {@code $(имя)}.
	 *
	 * @param command строка команды с макро-переменными
	 * @return разобранный объект {@code MacroInvocation}
	 * @throws IllegalArgumentException если переменных нет, имя некорректно или скобка не закрыта
	 */
	public static MacroInvocation parse(String command) {
		ImmutableList.Builder<String> segmentBuilder = ImmutableList.builder();
		ImmutableList.Builder<String> variableBuilder = ImmutableList.builder();
		int length = command.length();
		int segmentStart = 0;
		int sigilPos = command.indexOf(MACRO_SIGIL);

		while (sigilPos != -1) {
			if (sigilPos != length - 1 && command.charAt(sigilPos + 1) == MACRO_OPEN) {
				segmentBuilder.add(command.substring(segmentStart, sigilPos));
				int closePos = command.indexOf(MACRO_CLOSE, sigilPos + 1);
				if (closePos == -1) {
					throw new IllegalArgumentException("Unterminated macro variable");
				}

				String variableName = command.substring(sigilPos + 2, closePos);
				if (!isValidMacroName(variableName)) {
					throw new IllegalArgumentException("Invalid macro variable name '" + variableName + "'");
				}

				variableBuilder.add(variableName);
				segmentStart = closePos + 1;
				sigilPos = command.indexOf(MACRO_SIGIL, segmentStart);
			} else {
				sigilPos = command.indexOf(MACRO_SIGIL, sigilPos + 1);
			}
		}

		if (segmentStart == 0) {
			throw new IllegalArgumentException("No variables in macro");
		}

		if (segmentStart != length) {
			segmentBuilder.add(command.substring(segmentStart));
		}

		return new MacroInvocation(segmentBuilder.build(), variableBuilder.build());
	}

	public static boolean isValidMacroName(String name) {
		for (int index = 0; index < name.length(); index++) {
			char ch = name.charAt(index);
			if (!Character.isLetterOrDigit(ch) && ch != '_') {
				return false;
			}
		}

		return true;
	}

	/**
	 * Подставляет аргументы в шаблон макроса и возвращает итоговую строку команды.
	 *
	 * @param arguments список значений, по одному на каждую переменную
	 * @return строка команды с подставленными значениями
	 */
	public String apply(List<String> arguments) {
		StringBuilder result = new StringBuilder();

		for (int index = 0; index < variables.size(); index++) {
			result.append(segments.get(index)).append(arguments.get(index));
			CommandFunction.validateCommandLength(result);
		}

		if (segments.size() > variables.size()) {
			result.append(segments.getLast());
		}

		CommandFunction.validateCommandLength(result);
		return result.toString();
	}
}
