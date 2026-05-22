package net.minecraft.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Тип аргумента команды для разбора арифметической операции над очками скорборда.
 *
 * <p>Поддерживаемые операторы: {@code =}, {@code +=}, {@code -=}, {@code *=},
 * {@code /=}, {@code %=}, {@code <} (min), {@code >} (max), {@code ><} (swap).
 * Деление и взятие остатка используют математическое (floor) деление, а не Java-деление.
 */
public class OperationArgumentType implements ArgumentType<OperationArgumentType.Operation> {

	private static final Collection<String> EXAMPLES = Arrays.asList("=", ">", "<");

	private static final SimpleCommandExceptionType INVALID_OPERATION =
			new SimpleCommandExceptionType(Text.translatable("arguments.operation.invalid"));

	private static final SimpleCommandExceptionType DIVISION_ZERO_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("arguments.operation.div0"));

	public static OperationArgumentType operation() {
		return new OperationArgumentType();
	}

	public static OperationArgumentType.Operation getOperation(
			CommandContext<ServerCommandSource> context,
			String name
	) {
		return (OperationArgumentType.Operation) context.getArgument(name, OperationArgumentType.Operation.class);
	}

	@Override
	public OperationArgumentType.Operation parse(StringReader reader) throws CommandSyntaxException {
		if (!reader.canRead()) {
			throw INVALID_OPERATION.createWithContext(reader);
		}

		int start = reader.getCursor();

		while (reader.canRead() && reader.peek() != ' ') {
			reader.skip();
		}

		return getOperator(reader.getString().substring(start, reader.getCursor()));
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(
				new String[]{"=", "+=", "-=", "*=", "/=", "%=", "<", ">", "><"},
				builder
		);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	private static OperationArgumentType.Operation getOperator(String operator) throws CommandSyntaxException {
		if (operator.equals("><")) {
			return (a, b) -> {
				int temp = a.getScore();
				a.setScore(b.getScore());
				b.setScore(temp);
			};
		}

		return getIntOperator(operator);
	}

	private static OperationArgumentType.IntOperator getIntOperator(String operator) throws CommandSyntaxException {
		return switch (operator) {
			case "=" -> (a, b) -> b;
			case "+=" -> Integer::sum;
			case "-=" -> (a, b) -> a - b;
			case "*=" -> (a, b) -> a * b;
			case "/=" -> (a, b) -> {
				if (b == 0) {
					throw DIVISION_ZERO_EXCEPTION.create();
				}

				return MathHelper.floorDiv(a, b);
			};
			case "%=" -> (a, b) -> {
				if (b == 0) {
					throw DIVISION_ZERO_EXCEPTION.create();
				}

				return MathHelper.floorMod(a, b);
			};
			case "<" -> Math::min;
			case ">" -> Math::max;
			default -> throw INVALID_OPERATION.create();
		};
	}

	/**
	 * Операция над двумя целочисленными значениями очков скорборда.
	 * Реализует {@link Operation} через делегирование к {@link ScoreAccess#setScore}.
	 */
	@FunctionalInterface
	interface IntOperator extends OperationArgumentType.Operation {

		int apply(int a, int b) throws CommandSyntaxException;

		@Override
		default void apply(ScoreAccess a, ScoreAccess b) throws CommandSyntaxException {
			a.setScore(apply(a.getScore(), b.getScore()));
		}

	}

	/**
	 * Функциональный интерфейс операции над двумя {@link ScoreAccess}.
	 */
	@FunctionalInterface
	public interface Operation {

		void apply(ScoreAccess a, ScoreAccess b) throws CommandSyntaxException;

	}

}
