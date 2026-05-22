package net.minecraft.util.logging;

import com.mojang.logging.LogUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.OutputStream;

/**
 * Расширение {@link LoggerPrintStream}, которое дополнительно логирует
 * имя файла и номер строки вызывающего кода для отладки.
 */
public class DebugLoggerPrintStream extends LoggerPrintStream {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int CALLER_STACK_DEPTH = 3;

	public DebugLoggerPrintStream(String name, OutputStream outputStream) {
		super(name, outputStream);
	}

	@Override
	protected void log(@Nullable String message) {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StackTraceElement caller = stack[Math.min(CALLER_STACK_DEPTH, stack.length)];
		LOGGER.info(
				"[{}]@.({}:{}): {}",
				name, caller.getFileName(), caller.getLineNumber(), message
		);
	}
}
