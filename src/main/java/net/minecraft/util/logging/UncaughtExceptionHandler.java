package net.minecraft.util.logging;

import org.slf4j.Logger;

/**
 * Обработчик необработанных исключений потока, логирующий и имя потока, и само исключение.
 */
public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

	private final Logger logger;

	public UncaughtExceptionHandler(Logger logger) {
		this.logger = logger;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable throwable) {
		logger.error("Caught previously unhandled exception :");
		logger.error(thread.getName(), throwable);
	}
}
