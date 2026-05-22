package net.minecraft.util.logging;

import org.slf4j.Logger;

/**
 * Упрощённый обработчик необработанных исключений потока,
 * логирующий только само исключение без имени потока.
 */
public class UncaughtExceptionLogger implements Thread.UncaughtExceptionHandler {

	private final Logger logger;

	public UncaughtExceptionLogger(Logger logger) {
		this.logger = logger;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable throwable) {
		logger.error("Caught previously unhandled exception :", throwable);
	}
}
