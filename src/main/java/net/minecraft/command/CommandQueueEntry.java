package net.minecraft.command;

/**
 * {@code CommandQueueEntry}.
 */
public record CommandQueueEntry<T>(Frame frame, CommandAction<T> action) {

	public void execute(CommandExecutionContext<T> context) {
		this.action.execute(context, this.frame);
	}
}
