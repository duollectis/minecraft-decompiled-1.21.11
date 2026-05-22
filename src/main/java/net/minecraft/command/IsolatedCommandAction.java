package net.minecraft.command;

import net.minecraft.server.command.AbstractServerCommandSource;

import java.util.function.Consumer;

/**
 * Действие команды, выполняемое в изолированном фрейме с собственным
 * потребителем возвращаемого значения.
 */
public class IsolatedCommandAction<T extends AbstractServerCommandSource<T>> implements CommandAction<T> {

	private final Consumer<ExecutionControl<T>> controlConsumer;
	private final ReturnValueConsumer returnValueConsumer;

	public IsolatedCommandAction(
			Consumer<ExecutionControl<T>> controlConsumer,
			ReturnValueConsumer returnValueConsumer
	) {
		this.controlConsumer = controlConsumer;
		this.returnValueConsumer = returnValueConsumer;
	}

	@Override
	public void execute(CommandExecutionContext<T> context, Frame frame) {
		int childDepth = frame.depth() + 1;
		Frame childFrame = new Frame(childDepth, returnValueConsumer, context.getEscapeControl(childDepth));
		controlConsumer.accept(ExecutionControl.of(context, childFrame));
	}
}
