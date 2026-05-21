package net.minecraft.util.crash;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.minecraft.util.SystemDetails;
import net.minecraft.util.path.PathUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;

/**
 * {@code CrashReport}.
 */
public class CrashReport {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final DateTimeFormatter
			DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
	private final String message;
	private final Throwable cause;
	private final List<CrashReportSection> otherSections = Lists.newArrayList();
	private @Nullable Path file;
	private boolean hasStackTrace = true;
	private StackTraceElement[] stackTrace = new StackTraceElement[0];
	private final SystemDetails systemDetailsSection = new SystemDetails();

	public CrashReport(String message, Throwable cause) {
		this.message = message;
		this.cause = cause;
	}

	public String getMessage() {
		return this.message;
	}

	public Throwable getCause() {
		return this.cause;
	}

	public String getStackTrace() {
		StringBuilder stringBuilder = new StringBuilder();
		this.addDetails(stringBuilder);
		return stringBuilder.toString();
	}

	/**
	 * Добавляет details.
	 *
	 * @param crashReportBuilder crash report builder
	 */
	public void addDetails(StringBuilder crashReportBuilder) {
		if ((this.stackTrace == null || this.stackTrace.length <= 0) && !this.otherSections.isEmpty()) {
			this.stackTrace =
					(StackTraceElement[]) ArrayUtils.subarray(this.otherSections.get(0).getStackTrace(), 0, 1);
		}

		if (this.stackTrace != null && this.stackTrace.length > 0) {
			crashReportBuilder.append("-- Head --\n");
			crashReportBuilder.append("Thread: ").append(Thread.currentThread().getName()).append("\n");
			crashReportBuilder.append("Stacktrace:\n");

			for (StackTraceElement stackTraceElement : this.stackTrace) {
				crashReportBuilder.append("\t").append("at ").append(stackTraceElement);
				crashReportBuilder.append("\n");
			}

			crashReportBuilder.append("\n");
		}

		for (CrashReportSection crashReportSection : this.otherSections) {
			crashReportSection.addStackTrace(crashReportBuilder);
			crashReportBuilder.append("\n\n");
		}

		this.systemDetailsSection.writeTo(crashReportBuilder);
	}

	public String getCauseAsString() {
		StringWriter stringWriter = null;
		PrintWriter printWriter = null;
		Throwable throwable = this.cause;
		if (throwable.getMessage() == null) {
			if (throwable instanceof NullPointerException) {
				throwable = new NullPointerException(this.message);
			}
			else if (throwable instanceof StackOverflowError) {
				throwable = new StackOverflowError(this.message);
			}
			else if (throwable instanceof OutOfMemoryError) {
				throwable = new OutOfMemoryError(this.message);
			}

			throwable.setStackTrace(this.cause.getStackTrace());
		}

		String var4;
		try {
			stringWriter = new StringWriter();
			printWriter = new PrintWriter(stringWriter);
			throwable.printStackTrace(printWriter);
			var4 = stringWriter.toString();
		}
		finally {
			IOUtils.closeQuietly(stringWriter);
			IOUtils.closeQuietly(printWriter);
		}

		return var4;
	}

	/**
	 * As string.
	 *
	 * @param type type
	 * @param extraInfo extra info
	 *
	 * @return String — результат операции
	 */
	public String asString(ReportType type, List<String> extraInfo) {
		StringBuilder stringBuilder = new StringBuilder();
		type.addHeaderAndNugget(stringBuilder, extraInfo);
		stringBuilder.append("Time: ");
		stringBuilder.append(DATE_TIME_FORMATTER.format(ZonedDateTime.now()));
		stringBuilder.append("\n");
		stringBuilder.append("Description: ");
		stringBuilder.append(this.message);
		stringBuilder.append("\n\n");
		stringBuilder.append(this.getCauseAsString());
		stringBuilder.append(
				"\n\nA detailed walkthrough of the error, its code path and all known details is as follows:\n");

		for (int i = 0; i < 87; i++) {
			stringBuilder.append("-");
		}

		stringBuilder.append("\n\n");
		this.addDetails(stringBuilder);
		return stringBuilder.toString();
	}

	/**
	 * As string.
	 *
	 * @param type type
	 *
	 * @return String — результат операции
	 */
	public String asString(ReportType type) {
		return this.asString(type, List.of());
	}

	public @Nullable Path getFile() {
		return this.file;
	}

	/**
	 * Записывает to file.
	 *
	 * @param path path
	 * @param type type
	 * @param extraInfo extra info
	 *
	 * @return boolean — результат операции
	 */
	public boolean writeToFile(Path path, ReportType type, List<String> extraInfo) {
		if (this.file != null) {
			return false;
		}
		else {
			try {
				if (path.getParent() != null) {
					PathUtil.createDirectories(path.getParent());
				}

				try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
					writer.write(this.asString(type, extraInfo));
				}

				this.file = path;
				return true;
			}
			catch (Throwable var9) {
				LOGGER.error("Could not save crash report to {}", path, var9);
				return false;
			}
		}
	}

	/**
	 * Записывает to file.
	 *
	 * @param path path
	 * @param type type
	 *
	 * @return boolean — результат операции
	 */
	public boolean writeToFile(Path path, ReportType type) {
		return this.writeToFile(path, type, List.of());
	}

	public SystemDetails getSystemDetailsSection() {
		return this.systemDetailsSection;
	}

	/**
	 * Добавляет element.
	 *
	 * @param name name
	 *
	 * @return CrashReportSection — результат операции
	 */
	public CrashReportSection addElement(String name) {
		return this.addElement(name, 1);
	}

	/**
	 * Добавляет element.
	 *
	 * @param name name
	 * @param ignoredStackTraceCallCount ignored stack trace call count
	 *
	 * @return CrashReportSection — результат операции
	 */
	public CrashReportSection addElement(String name, int ignoredStackTraceCallCount) {
		CrashReportSection crashReportSection = new CrashReportSection(name);
		if (this.hasStackTrace) {
			int i = crashReportSection.initStackTrace(ignoredStackTraceCallCount);
			StackTraceElement[] stackTraceElements = this.cause.getStackTrace();
			StackTraceElement stackTraceElement = null;
			StackTraceElement stackTraceElement2 = null;
			int j = stackTraceElements.length - i;
			if (j < 0) {
				LOGGER.error("Negative index in crash report handler ({}/{})", stackTraceElements.length, i);
			}

			if (stackTraceElements != null && 0 <= j && j < stackTraceElements.length) {
				stackTraceElement = stackTraceElements[j];
				if (stackTraceElements.length + 1 - i < stackTraceElements.length) {
					stackTraceElement2 = stackTraceElements[stackTraceElements.length + 1 - i];
				}
			}

			this.hasStackTrace = crashReportSection.shouldGenerateStackTrace(stackTraceElement, stackTraceElement2);
			if (stackTraceElements != null && stackTraceElements.length >= i && 0 <= j
					&& j < stackTraceElements.length) {
				this.stackTrace = new StackTraceElement[j];
				System.arraycopy(stackTraceElements, 0, this.stackTrace, 0, this.stackTrace.length);
			}
			else {
				this.hasStackTrace = false;
			}
		}

		this.otherSections.add(crashReportSection);
		return crashReportSection;
	}

	/**
	 * Create.
	 *
	 * @param cause cause
	 * @param title title
	 *
	 * @return CrashReport — результат операции
	 */
	public static CrashReport create(Throwable cause, String title) {
		while (cause instanceof CompletionException && cause.getCause() != null) {
			cause = cause.getCause();
		}

		CrashReport crashReport;
		if (cause instanceof CrashException crashException) {
			crashReport = crashException.getReport();
		}
		else {
			crashReport = new CrashReport(title, cause);
		}

		return crashReport;
	}

	/**
	 * Инициализирует crash report.
	 */
	public static void initCrashReport() {
		CrashMemoryReserve.reserveMemory();
		new CrashReport("Don't panic!", new Throwable()).asString(ReportType.MINECRAFT_CRASH_REPORT);
	}
}
