package net.minecraft.server.dedicated;

import net.minecraft.server.command.ServerCommandSource;

/**
 * Команда, ожидающая выполнения в очереди сервера: хранит текст команды и источник.
 */
public class PendingServerCommand {

	public final String command;
	public final ServerCommandSource source;

	public PendingServerCommand(String command, ServerCommandSource commandSource) {
		this.command = command;
		this.source = commandSource;
	}
}
