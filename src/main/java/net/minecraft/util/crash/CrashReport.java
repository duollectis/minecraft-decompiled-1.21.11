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
 * Полный отчёт о сбое: содержит сообщение, причину, секции с деталями и системную информацию.
 * Может быть сохранён в файл или преобразован в строку для вывода.
 */
public class CrashReport {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final DateTimeFormatter DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
	private static final int SEPARATOR_LENGTH = 87;

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
		return message;
	}

	public Throwable getCause() {
		return cause;
	}

	public String getStackTrace() {
		StringBuilder builder = new StringBuilder();
		addDetails(builder);
		return builder.toString();
	}

	/**
	 * Добавляет в {@code builder} заголовок стека, все секции и системные детали.
	 *
	 * @param builder целевой строковый буфер
	 */
	public void addDetails(StringBuilder builder) {
		if ((stackTrace == null || stackTrace.length <= 0) && !otherSections.isEmpty()) {
			stackTrace = (StackTraceElement[]) ArrayUtils.subarray(otherSections.get(0).getStackTrace(), 0, 1);
		}

		if (stackTrace != null && stackTrace.length > 0) {
			builder.append("-- Head --\n");
			builder.append("Thread: ").append(Thread.currentThread().getName()).append("\n");
			builder.append("Stacktrace:\n");

			for (StackTraceElement element : stackTrace) {
				builder.append("\t").append("at ").append(element).append("\n");
			}

			builder.append("\n");
		}

		for (CrashReportSection section : otherSections) {
			section.addStackTrace(builder);
			builder.append("\n\n");
		}

		systemDetailsSection.writeTo(builder);
	}

	public String getCauseAsString() {
		Throwable throwable = cause;

		if (throwable.getMessage() == null) {
			if (throwable instanceof NullPointerException) {
				throwable = new NullPointerException(message);
			}
			else if (throwable instanceof StackOverflowError) {
				throwable = new StackOverflowError(message);
			}
			else if (throwable instanceof OutOfMemoryError) {
				throwable = new OutOfMemoryError(message);
			}

			throwable.setStackTrace(cause.getStackTrace());
		}

		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);

		try {
			throwable.printStackTrace(printWriter);
			return stringWriter.toString();
		}
		finally {
			IOUtils.closeQuietly(stringWriter);
			IOUtils.closeQuietly(printWriter);
		}
	}

	/**
	 * Формирует полный текст отчёта о сбое с заголовком, временем, описанием и деталями.
	 *
	 * @param type      тип отчёта (определяет заголовок и случайную фразу)
	 * @param extraInfo дополнительные строки после заголовка
	 * @return полный текст отчёта
	 */
	public String asString(ReportType type, List<String> extraInfo) {
		StringBuilder builder = new StringBuilder();
		type.addHeaderAndNugget(builder, extraInfo);
		builder.append("Time: ").append(DATE_TIME_FORMATTER.format(ZonedDateTime.now())).append("\n");
		builder.append("Description: ").append(message).append("\n\n");
		builder.append(getCauseAsString());
		builder.append("\n\nA detailed walkthrough of the error, its code path and all known details is as follows:\n");
		builder.append("-".repeat(SEPARATOR_LENGTH)).append("\n\n");
		addDetails(builder);
		return builder.toString();
	}

	public String asString(ReportType type) {
		return asString(type, List.of());
	}

	public @Nullable Path getFile() {
		return file;
	}

	/**
	 * Записывает отчёт в файл по указанному пути.
	 * Если файл уже был записан ранее, возвращает {@code false} без повторной записи.
	 *
	 * @param path      путь для сохранения
	 * @param type      тип отчёта
	 * @param extraInfo дополнительные строки
	 * @return {@code true} при успешной записи
	 */
	public boolean writeToFile(Path path, ReportType type, List<String> extraInfo) {
		if (file != null) {
			return false;
		}

		try {
			if (path.getParent() != null) {
				PathUtil.createDirectories(path.getParent());
			}

			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				writer.write(asString(type, extraInfo));
			}

			file = path;
			return true;
		}
		catch (Throwable exception) {
			LOGGER.error("Could not save crash report to {}", path, exception);
			return false;
		}
	}

	public boolean writeToFile(Path path, ReportType type) {
		return writeToFile(path, type, List.of());
	}

	public SystemDetails getSystemDetailsSection() {
		return systemDetailsSection;
	}

	public CrashReportSection addElement(String name) {
		return addElement(name, 1);
	}

	/**
	 * Добавляет новую секцию в отчёт и пытается связать её со стек-трейсом причины.
	 *
	 * @param name                      название секции
	 * @param ignoredStackTraceCallCount количество фреймов стека для пропуска
	 * @return созданная секция
	 */
	public CrashReportSection addElement(String name, int ignoredStackTraceCallCount) {
		CrashReportSection section = new CrashReportSection(name);

		if (hasStackTrace) {
			int sectionDepth = section.initStackTrace(ignoredStackTraceCallCount);
			StackTraceElement[] causeTrace = cause.getStackTrace();
			StackTraceElement prev = null;
			StackTraceElement next = null;
			int index = causeTrace.length - sectionDepth;

			if (index < 0) {
				LOGGER.error("Negative index in crash report handler ({}/{})", causeTrace.length, sectionDepth);
			}

			if (causeTrace != null && 0 <= index && index < causeTrace.length) {
				prev = causeTrace[index];

				if (causeTrace.length + 1 - sectionDepth < causeTrace.length) {
					next = causeTrace[causeTrace.length + 1 - sectionDepth];
				}
			}

			hasStackTrace = section.shouldGenerateStackTrace(prev, next);

			if (causeTrace != null && causeTrace.length >= sectionDepth && 0 <= index && index < causeTrace.length) {
				stackTrace = new StackTraceElement[index];
				System.arraycopy(causeTrace, 0, stackTrace, 0, stackTrace.length);
			}
			else {
				hasStackTrace = false;
			}
		}

		otherSections.add(section);
		return section;
	}

	/**
	 * Создаёт отчёт о сбое из исключения, разворачивая {@link CompletionException} и
	 * переиспользуя существующий отчёт из {@link CrashException}.
	 *
	 * @param cause причина сбоя
	 * @param title заголовок отчёта
	 * @return отчёт о сбое
	 */
	public static CrashReport create(Throwable cause, String title) {
		while (cause instanceof CompletionException && cause.getCause() != null) {
			cause = cause.getCause();
		}

		return cause instanceof CrashException crashException
				? crashException.getReport()
				: new CrashReport(title, cause);
	}

	/**
	 * Прогревает систему отчётов о сбоях: резервирует память и инициализирует шаблон отчёта.
	 * Вызывается при старте игры для снижения задержки при первом реальном сбое.
	 */
	public static void initCrashReport() {
		CrashMemoryReserve.reserveMemory();
		new CrashReport("Don't panic!", new Throwable()).asString(ReportType.MINECRAFT_CRASH_REPORT);
	}
}
