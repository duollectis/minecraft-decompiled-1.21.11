package net.minecraft.network;

import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.crash.CrashException;
import org.slf4j.Logger;

import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;

/**
 * Потокобезопасная очередь для отложенного применения пакетов в главном потоке сервера.
 * <p>Пакеты, поступающие из сетевых потоков, помещаются в очередь через {@link #add},
 * а затем применяются в главном потоке через {@link #apply}.
 * После вызова {@link #close} добавление новых пакетов запрещено.
 */
public class PacketApplyBatcher implements AutoCloseable {

	static final Logger LOGGER = LogUtils.getLogger();

	private final Queue<Entry<?>> entries = Queues.newConcurrentLinkedQueue();
	private final Thread thread;
	private boolean closed;

	/**
	 * Создаёт батчер, привязанный к указанному потоку.
	 *
	 * @param thread поток, в котором должны применяться пакеты
	 */
	public PacketApplyBatcher(Thread thread) {
		this.thread = thread;
	}

	/**
	 * Проверяет, выполняется ли текущий код в потоке этого батчера.
	 *
	 * @return {@code true}, если текущий поток совпадает с потоком батчера
	 */
	public boolean isOnThread() {
		return Thread.currentThread() == thread;
	}

	/**
	 * Добавляет пакет в очередь для последующего применения.
	 *
	 * @param listener слушатель, которому будет передан пакет
	 * @param packet   пакет для применения
	 * @throws RejectedExecutionException если батчер уже закрыт
	 */
	public <T extends PacketListener> void add(T listener, Packet<T> packet) {
		if (closed) {
			throw new RejectedExecutionException("Server already shutting down");
		}

		entries.add(new Entry<>(listener, packet));
	}

	/**
	 * Применяет все накопленные пакеты из очереди.
	 * Не выполняет ничего, если батчер закрыт.
	 */
	public void apply() {
		if (closed) {
			return;
		}

		while (entries.isEmpty() == false) {
			entries.poll().apply();
		}
	}

	@Override
	public void close() {
		closed = true;
	}

	/**
	 * Запись очереди, связывающая пакет с его слушателем.
	 *
	 * @param listener слушатель пакетов
	 * @param packet   пакет для применения
	 */
	record Entry<T extends PacketListener>(T listener, Packet<T> packet) {

		/**
		 * Применяет пакет к слушателю, если тот его принимает.
		 * При критической ошибке памяти пробрасывает {@link CrashException}.
		 */
		public void apply() {
			if (listener.accepts(packet) == false) {
				LOGGER.debug("Ignoring packet due to disconnection: {}", packet);
				return;
			}

			try {
				packet.apply(listener);
			}
			catch (Exception exception) {
				if (exception instanceof CrashException crash
						&& crash.getCause() instanceof OutOfMemoryError) {
					throw NetworkThreadUtils.createCrashException(exception, packet, listener);
				}

				listener.onPacketException(packet, exception);
			}
		}
	}
}
