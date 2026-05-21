package net.minecraft.test;

import net.minecraft.text.Text;

/**
 * {@code TestException}.
 */
public abstract class TestException extends RuntimeException {

	public TestException(String message) {
		super(message);
	}

	public abstract Text getText();
}
