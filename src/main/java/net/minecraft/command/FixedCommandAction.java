package net.minecraft.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.Tracer;

/**
 * Действие команды с фиксированным контекстом выполнения.
 * Выполняет конкретный узел дерева команд brigadier для заданного источника.
 */
public class FixedCommandAction<T extends AbstractServerCommandSource<T>> implements SourcedCommandAction<T> {

	private final String command;
	private final ExecutionFlags flags;
	private final CommandContext<T> context;

	public FixedCommandAction(String command, ExecutionFlags flags, CommandContext<T> context) {
		this.command = command;
		this.flags = flags;
		this.context = context;
	}

	public void execute(T source, CommandExecutionContext<T> executionContext, Frame frame) {
		executionContext.getProfiler().push(() -> "execute " + command);

		try {
			executionContext.decrementCommandQuota();
			int returnValue = ContextChain.runExecutable(
					context,
					source,
					AbstractServerCommandSource.asResultConsumer(),
					flags.isSilent()
			);
			Tracer tracer = executionContext.getTracer();
			if (tracer != null) {
				tracer.traceCommandEnd(frame.depth(), command, returnValue);
			}
		} catch (CommandSyntaxException exception) {
			source.handleException(exception, flags.isSilent(), executionContext.getTracer());
		} finally {
			executionContext.getProfiler().pop();
		}
	}
}
