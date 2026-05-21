package net.minecraft.util.logging;

import com.mojang.logging.LogUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * {@code LoggerPrintStream}.
 */
public class LoggerPrintStream extends PrintStream {

	private static final Logger LOGGER = LogUtils.getLogger();
	protected final String name;

	public LoggerPrintStream(String name, OutputStream out) {
		super(out, false, StandardCharsets.UTF_8);
		this.name = name;
	}

	@Override
	public void println(@Nullable String message) {
		this.log(message);
	}

	@Override
	public void println(@Nullable Object object) {
		this.log(String.valueOf(object));
	}

	/**
	 * Log.
	 *
	 * @param message message
	 */
	protected void log(@Nullable String message) {
		LOGGER.info("[{}]: {}", this.name, message);
	}
}
