package net.minecraft.command;

@FunctionalInterface
/**
 * {@code SourcedCommandAction}.
 */
public interface SourcedCommandAction<T> {

	void execute(T source, CommandExecutionContext<T> context, Frame frame);

	default CommandAction<T> bind(T source) {
		return (context, frame) -> this.execute(source, context, frame);
	}
}
