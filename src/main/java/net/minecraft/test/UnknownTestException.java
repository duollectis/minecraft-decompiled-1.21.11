package net.minecraft.test;

import net.minecraft.text.Text;

/**
 * Обёртка для непредвиденных исключений, возникших во время выполнения теста.
 * Используется когда тест упал с исключением, не являющимся {@link TestException}.
 */
public class UnknownTestException extends TestException {

	private final Throwable cause;

	public UnknownTestException(Throwable cause) {
		super(cause.getMessage());
		this.cause = cause;
	}

	@Override
	public Text getText() {
		return Text.translatable("test.error.unknown", cause.getMessage());
	}
}
