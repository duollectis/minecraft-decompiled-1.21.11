package net.minecraft.server.function;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.SingleCommandAction;
import net.minecraft.command.SourcedCommandAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Представляет функцию (mcfunction-файл) — именованную последовательность команд,
 * которая может содержать макро-переменные (строки, начинающиеся с {@code $}).
 */
public interface CommandFunction<T> {

	int MAX_COMMAND_LENGTH = 2000000;
	int MAX_COMMAND_PREVIEW_LENGTH = 512;

	Identifier id();

	Procedure<T> withMacroReplaced(@Nullable NbtCompound arguments, CommandDispatcher<T> dispatcher)
	throws MacroException;

	private static boolean continuesToNextLine(CharSequence line) {
		int length = line.length();
		return length > 0 && line.charAt(length - 1) == '\\';
	}

	/**
	 * Парсит список строк mcfunction-файла и создаёт {@link CommandFunction}.
	 * Поддерживает перенос строк через {@code \}, комментарии {@code #} и макро-команды {@code $}.
	 */
	static <T extends AbstractServerCommandSource<T>> CommandFunction<T> create(
			Identifier id,
			CommandDispatcher<T> dispatcher,
			T source,
			List<String> lines
	) {
		FunctionBuilder<T> builder = new FunctionBuilder<>();

		for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
			int lineNumber = lineIndex + 1;
			String trimmedLine = lines.get(lineIndex).trim();
			String fullLine;

			if (continuesToNextLine(trimmedLine)) {
				StringBuilder lineBuilder = new StringBuilder(trimmedLine);

				do {
					if (++lineIndex == lines.size()) {
						throw new IllegalArgumentException("Line continuation at end of file");
					}

					lineBuilder.deleteCharAt(lineBuilder.length() - 1);
					String continuedLine = lines.get(lineIndex).trim();
					lineBuilder.append(continuedLine);
					validateCommandLength(lineBuilder);
				}
				while (continuesToNextLine(lineBuilder));

				fullLine = lineBuilder.toString();
			}
			else {
				fullLine = trimmedLine;
			}

			validateCommandLength(fullLine);
			StringReader reader = new StringReader(fullLine);

			if (reader.canRead() && reader.peek() != '#') {
				if (reader.peek() == '/') {
					reader.skip();

					if (reader.peek() == '/') {
						throw new IllegalArgumentException(
								"Unknown or invalid command '" + fullLine + "' on line " + lineNumber
										+ " (if you intended to make a comment, use '#' not '//')"
						);
					}

					String commandName = reader.readUnquotedString();
					throw new IllegalArgumentException(
							"Unknown or invalid command '" + fullLine + "' on line " + lineNumber
									+ " (did you mean '" + commandName + "'? Do not use a preceding forwards slash.)"
					);
				}

				if (reader.peek() == '$') {
					builder.addMacroCommand(fullLine.substring(1), lineNumber, source);
				}
				else {
					try {
						builder.addAction(parse(dispatcher, source, reader));
					}
					catch (CommandSyntaxException exception) {
						throw new IllegalArgumentException(
								"Whilst parsing command on line " + lineNumber + ": " + exception.getMessage()
						);
					}
				}
			}
		}

		return builder.toCommandFunction(id);
	}

	static void validateCommandLength(CharSequence command) {
		if (command.length() <= MAX_COMMAND_LENGTH) {
			return;
		}

		CharSequence preview = command.subSequence(0, Math.min(MAX_COMMAND_PREVIEW_LENGTH, MAX_COMMAND_LENGTH));
		throw new IllegalStateException(
				"Command too long: " + command.length() + " characters, contents: " + preview + "..."
		);
	}

	static <T extends AbstractServerCommandSource<T>> SourcedCommandAction<T> parse(
			CommandDispatcher<T> dispatcher,
			T source,
			StringReader reader
	) throws CommandSyntaxException {
		ParseResults<T> parseResults = dispatcher.parse(reader, source);
		CommandManager.throwException(parseResults);
		Optional<ContextChain<T>> contextChain = ContextChain.tryFlatten(
				parseResults.getContext().build(reader.getString())
		);

		if (contextChain.isEmpty()) {
			throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
					.dispatcherUnknownCommand()
					.createWithContext(parseResults.getReader());
		}

		return new SingleCommandAction.Sourced<>(reader.getString(), contextChain.get());
	}
}
