package net.minecraft.command;

/**
 * Запись в очереди команд: связывает фрейм выполнения с конкретным действием.
 *
 * @param <T> тип источника команды
 */
public record CommandQueueEntry<T>(Frame frame, CommandAction<T> action) {

	public void execute(CommandExecutionContext<T> context) {
		action.execute(context, frame);
	}
}
