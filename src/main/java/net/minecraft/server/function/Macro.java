package net.minecraft.server.function;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.command.MacroInvocation;
import net.minecraft.command.SourcedCommandAction;
import net.minecraft.nbt.*;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Функция с макро-переменными: при вызове подставляет значения из {@link NbtCompound}
 * и кэширует результирующие {@link ExpandedMacro} для повторного использования.
 */
public class Macro<T extends AbstractServerCommandSource<T>> implements CommandFunction<T> {

	private static final DecimalFormat DECIMAL_FORMAT = Util.make(
			new DecimalFormat("#", DecimalFormatSymbols.getInstance(Locale.ROOT)),
			decimalFormat -> decimalFormat.setMaximumFractionDigits(15)
	);
	private static final int CACHE_SIZE = 8;

	private final List<String> varNames;
	private final Object2ObjectLinkedOpenHashMap<List<String>, Procedure<T>> cache =
			new Object2ObjectLinkedOpenHashMap<>(CACHE_SIZE, 0.25F);
	private final Identifier id;
	private final List<Macro.Line<T>> lines;

	public Macro(Identifier id, List<Macro.Line<T>> lines, List<String> varNames) {
		this.id = id;
		this.lines = lines;
		this.varNames = varNames;
	}

	@Override
	public Identifier id() {
		return id;
	}

	@Override
	public Procedure<T> withMacroReplaced(@Nullable NbtCompound arguments, CommandDispatcher<T> dispatcher)
	throws MacroException {
		if (arguments == null) {
			throw new MacroException(Text.translatable(
					"commands.function.error.missing_arguments",
					Text.of(id())
			));
		}

		List<String> varValues = new ArrayList<>(varNames.size());

		for (String varName : varNames) {
			NbtElement nbtValue = arguments.get(varName);

			if (nbtValue == null) {
				throw new MacroException(Text.translatable(
						"commands.function.error.missing_argument",
						Text.of(id()),
						varName
				));
			}

			varValues.add(toString(nbtValue));
		}

		Procedure<T> cached = (Procedure<T>) cache.getAndMoveToLast(varValues);

		if (cached != null) {
			return cached;
		}

		if (cache.size() >= CACHE_SIZE) {
			cache.removeFirst();
		}

		Procedure<T> expanded = expandWithValues(varNames, varValues, dispatcher);
		cache.put(varValues, expanded);
		return expanded;
	}

	private static String toString(NbtElement nbt) {
		return switch (nbt) {
			case NbtFloat(float value) -> DECIMAL_FORMAT.format(value);
			case NbtDouble(double value) -> DECIMAL_FORMAT.format(value);
			case NbtByte(byte value) -> String.valueOf((int) value);
			case NbtShort(short value) -> String.valueOf((int) value);
			case NbtLong(long value) -> String.valueOf(value);
			case NbtString(String value) -> value;
			default -> nbt.toString();
		};
	}

	private static void addArgumentsByIndices(List<String> arguments, IntList indices, List<String> out) {
		out.clear();
		indices.forEach(index -> out.add(arguments.get(index)));
	}

	private Procedure<T> expandWithValues(
			List<String> varNames,
			List<String> arguments,
			CommandDispatcher<T> dispatcher
	) throws MacroException {
		List<SourcedCommandAction<T>> actions = new ArrayList<>(lines.size());
		List<String> argBuffer = new ArrayList<>(arguments.size());

		for (Macro.Line<T> line : lines) {
			addArgumentsByIndices(arguments, line.getDependentVariables(), argBuffer);
			actions.add(line.instantiate(argBuffer, dispatcher, id));
		}

		return new ExpandedMacro<>(id().withPath(path -> path + "/" + varNames.hashCode()), actions);
	}

	interface Line<T> {

		IntList getDependentVariables();

		SourcedCommandAction<T> instantiate(List<String> args, CommandDispatcher<T> dispatcher, Identifier id)
		throws MacroException;
	}

	static class FixedLine<T> implements Macro.Line<T> {

		private final SourcedCommandAction<T> action;

		public FixedLine(SourcedCommandAction<T> action) {
			this.action = action;
		}

		@Override
		public IntList getDependentVariables() {
			return IntLists.emptyList();
		}

		@Override
		public SourcedCommandAction<T> instantiate(List<String> args, CommandDispatcher<T> dispatcher, Identifier id) {
			return action;
		}
	}

	static class VariableLine<T extends AbstractServerCommandSource<T>> implements Macro.Line<T> {

		private final MacroInvocation invocation;
		private final IntList variableIndices;
		private final T source;

		public VariableLine(MacroInvocation invocation, IntList variableIndices, T source) {
			this.invocation = invocation;
			this.variableIndices = variableIndices;
			this.source = source;
		}

		@Override
		public IntList getDependentVariables() {
			return variableIndices;
		}

		@Override
		public SourcedCommandAction<T> instantiate(List<String> args, CommandDispatcher<T> dispatcher, Identifier id)
		throws MacroException {
			String command = invocation.apply(args);

			try {
				return CommandFunction.parse(dispatcher, source, new StringReader(command));
			}
			catch (CommandSyntaxException exception) {
				throw new MacroException(Text.translatable(
						"commands.function.error.parse",
						Text.of(id),
						command,
						exception.getMessage()
				));
			}
		}
	}
}
