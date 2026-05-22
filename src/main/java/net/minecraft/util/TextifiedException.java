package net.minecraft.util;

import net.minecraft.text.Text;

/**
 * Исключение, несущее форматированное сообщение об ошибке в виде {@link Text}.
 * Используется там, где сообщение об ошибке должно быть отображено игроку
 * с поддержкой локализации и форматирования.
 */
public class TextifiedException extends Exception {

	private final Text messageText;

	public TextifiedException(Text messageText) {
		super(messageText.getString());
		this.messageText = messageText;
	}

	public TextifiedException(Text messageText, Throwable cause) {
		super(messageText.getString(), cause);
		this.messageText = messageText;
	}

	public Text getMessageText() {
		return messageText;
	}
}
