package net.minecraft.command;

@FunctionalInterface
/**
 * {@code CommandAction}.
 */
public interface CommandAction<T> {

	void execute(CommandExecutionContext<T> context, Frame frame);
}
