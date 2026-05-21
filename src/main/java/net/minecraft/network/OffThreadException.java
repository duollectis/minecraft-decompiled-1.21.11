package net.minecraft.network;

/**
 * Исключение-сигнал для прерывания обработки пакета вне сетевого потока.
 * <p>Используется как лёгкий сигнальный механизм: стек вызовов намеренно очищается,
 * чтобы не тратить ресурсы на его заполнение. Всегда используй единственный
 * экземпляр {@link #INSTANCE} вместо создания новых объектов.
 */
public final class OffThreadException extends RuntimeException {

	/**
	 * Единственный экземпляр — создание новых объектов запрещено.
	 */
	public static final OffThreadException INSTANCE = new OffThreadException();

	private OffThreadException() {
		setStackTrace(new StackTraceElement[0]);
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		setStackTrace(new StackTraceElement[0]);
		return this;
	}
}
