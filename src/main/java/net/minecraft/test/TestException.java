package net.minecraft.test;

import net.minecraft.text.Text;

/**
 * Базовый класс для всех исключений, возникающих в процессе выполнения игровых тестов.
 * Наследники обязаны предоставить форматированное текстовое описание ошибки.
 */
public abstract class TestException extends RuntimeException {

	public TestException(String message) {
		super(message);
	}

	public abstract Text getText();
}
