package net.minecraft.server;

/**
 * Задача, поставленная в очередь выполнения сервера.
 * Хранит тик создания для отладки и диагностики зависших задач.
 */
public class ServerTask implements Runnable {

	private final int creationTicks;
	private final Runnable runnable;

	public ServerTask(int creationTicks, Runnable runnable) {
		this.creationTicks = creationTicks;
		this.runnable = runnable;
	}

	public int getCreationTicks() {
		return creationTicks;
	}

	@Override
	public void run() {
		runnable.run();
	}
}
