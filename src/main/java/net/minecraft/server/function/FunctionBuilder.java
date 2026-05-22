package net.minecraft.server.function;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.command.MacroInvocation;
import net.minecraft.command.SourcedCommandAction;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Построитель {@link CommandFunction}: накапливает команды и макро-строки,
 * затем создаёт либо {@link ExpandedMacro} (без макросов), либо {@link Macro} (с макросами).
 */
class FunctionBuilder<T extends AbstractServerCommandSource<T>> {

	private @Nullable List<SourcedCommandAction<T>> actions = new ArrayList<>();
	private @Nullable List<Macro.Line<T>> macroLines;
	private final List<String> usedVariables = new ArrayList<>();

	public void addAction(SourcedCommandAction<T> action) {
		if (macroLines != null) {
			macroLines.add(new Macro.FixedLine<>(action));
		}
		else {
			actions.add(action);
		}
	}

	private int indexOfVariable(String variable) {
		int index = usedVariables.indexOf(variable);

		if (index == -1) {
			index = usedVariables.size();
			usedVariables.add(variable);
		}

		return index;
	}

	private IntList indicesOfVariables(List<String> variables) {
		IntArrayList indices = new IntArrayList(variables.size());

		for (String variable : variables) {
			indices.add(indexOfVariable(variable));
		}

		return indices;
	}

	public void addMacroCommand(String command, int lineNum, T source) {
		MacroInvocation invocation;
		try {
			invocation = MacroInvocation.parse(command);
		}
		catch (Exception exception) {
			throw new IllegalArgumentException(
					"Can't parse function line " + lineNum + ": '" + command + "'", exception
			);
		}

		if (actions != null) {
			macroLines = new ArrayList<>(actions.size() + 1);

			for (SourcedCommandAction<T> action : actions) {
				macroLines.add(new Macro.FixedLine<>(action));
			}

			actions = null;
		}

		macroLines.add(new Macro.VariableLine<>(
				invocation,
				indicesOfVariables(invocation.variables()),
				source
		));
	}

	public CommandFunction<T> toCommandFunction(Identifier id) {
		return (CommandFunction<T>) (macroLines != null
				? new Macro<>(id, macroLines, usedVariables)
				: new ExpandedMacro<>(id, actions));
	}
}
