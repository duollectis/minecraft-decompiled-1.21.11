package net.minecraft.client.realms.exception;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;

import java.lang.Thread.UncaughtExceptionHandler;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsDefaultUncaughtExceptionHandler}.
 */
public class RealmsDefaultUncaughtExceptionHandler implements UncaughtExceptionHandler {

	private final Logger logger;

	public RealmsDefaultUncaughtExceptionHandler(Logger logger) {
		this.logger = logger;
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		this.logger.error("Caught previously unhandled exception", e);
	}
}
