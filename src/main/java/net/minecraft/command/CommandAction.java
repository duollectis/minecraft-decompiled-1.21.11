package net.minecraft.command;

/**
 * Действие, выполняемое в рамках очереди команд.
 *
 * @param <T> тип источника команды
 */
@FunctionalInterface
public interface CommandAction<T> {

	void execute(CommandExecutionContext<T> context, Frame frame);
}
