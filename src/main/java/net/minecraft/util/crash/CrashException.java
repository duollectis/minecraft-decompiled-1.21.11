package net.minecraft.util.crash;

/**
 * Непроверяемое исключение, оборачивающее {@link CrashReport}.
 * Используется для прерывания выполнения при критических ошибках с сохранением полного контекста.
 */
public class CrashException extends RuntimeException {

	private final CrashReport report;

	public CrashException(CrashReport report) {
		this.report = report;
	}

	public CrashReport getReport() {
		return report;
	}

	@Override
	public Throwable getCause() {
		return report.getCause();
	}

	@Override
	public String getMessage() {
		return report.getMessage();
	}
}
