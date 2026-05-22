package net.minecraft.command;

/**
 * Действие команды, привязанное к конкретному источнику.
 *
 * @param <T> тип источника команды
 */
@FunctionalInterface
public interface SourcedCommandAction<T> {

	void execute(T source, CommandExecutionContext<T> context, Frame frame);

	default CommandAction<T> bind(T source) {
		return (context, frame) -> execute(source, context, frame);
	}
}
