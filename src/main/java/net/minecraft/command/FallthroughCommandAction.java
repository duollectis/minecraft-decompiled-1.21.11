package net.minecraft.command;

import net.minecraft.server.command.AbstractServerCommandSource;

/**
 * Действие-заглушка, сигнализирующее о провале выполнения и немедленном
 * возврате из текущего фрейма. Используется как sentinel при пустом списке источников.
 */
public class FallthroughCommandAction<T extends AbstractServerCommandSource<T>> implements CommandAction<T> {

	@SuppressWarnings("rawtypes")
	private static final FallthroughCommandAction INSTANCE = new FallthroughCommandAction<>();

	@SuppressWarnings("unchecked")
	public static <T extends AbstractServerCommandSource<T>> CommandAction<T> getInstance() {
		return INSTANCE;
	}

	@Override
	public void execute(CommandExecutionContext<T> context, Frame frame) {
		frame.fail();
		frame.doReturn();
	}
}
