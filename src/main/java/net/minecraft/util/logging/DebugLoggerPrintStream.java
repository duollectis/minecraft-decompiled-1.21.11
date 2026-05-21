package net.minecraft.util.logging;

import com.mojang.logging.LogUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.OutputStream;

/**
 * {@code DebugLoggerPrintStream}.
 */
public class DebugLoggerPrintStream extends LoggerPrintStream {

	private static final Logger LOGGER = LogUtils.getLogger();

	public DebugLoggerPrintStream(String string, OutputStream outputStream) {
		super(string, outputStream);
	}

	@Override
	protected void log(@Nullable String message) {
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		StackTraceElement stackTraceElement = stackTraceElements[Math.min(3, stackTraceElements.length)];
		LOGGER.info(
				"[{}]@.({}:{}): {}",
				new Object[]{this.name, stackTraceElement.getFileName(), stackTraceElement.getLineNumber(), message}
		);
	}
}
