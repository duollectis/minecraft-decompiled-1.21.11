package net.minecraft.command;

import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.Procedure;
import net.minecraft.server.function.Tracer;

import java.util.List;

/**
 * Действие, выполняющее функцию (процедуру) команды — разворачивает её записи
 * в очередь выполнения с новым дочерним фреймом.
 *
 * @param <T> тип источника команды
 */
public class CommandFunctionAction<T extends AbstractServerCommandSource<T>> implements SourcedCommandAction<T> {

	private final Procedure<T> function;
	private final ReturnValueConsumer returnValueConsumer;
	private final boolean propagateReturn;

	public CommandFunctionAction(
			Procedure<T> function,
			ReturnValueConsumer returnValueConsumer,
			boolean propagateReturn
	) {
		this.function = function;
		this.returnValueConsumer = returnValueConsumer;
		this.propagateReturn = propagateReturn;
	}

	public void execute(T source, CommandExecutionContext<T> context, Frame frame) {
		context.decrementCommandQuota();
		List<SourcedCommandAction<T>> entries = function.entries();
		Tracer tracer = context.getTracer();
		if (tracer != null) {
			tracer.traceFunctionCall(frame.depth(), function.id(), entries.size());
		}

		int childDepth = frame.depth() + 1;
		Frame.Control control = propagateReturn
				? frame.frameControl()
				: context.getEscapeControl(childDepth);
		Frame childFrame = new Frame(childDepth, returnValueConsumer, control);
		SteppedCommandAction.enqueueCommands(
				context,
				childFrame,
				entries,
				(framex, action) -> new CommandQueueEntry<>(framex, action.bind(source))
		);
	}
}
